# Integration Testing Guide

## Overview

Distops has a Python-based integration test suite in `scripts/tests/`. It supports two modes:

- **Mode 1 — Direct**: client connects directly to registry ports (5000)
- **Mode 2 — Proxy via nginx**: client connects through nginx (34000) which proxies to registry

## Prerequisites

- Python 3
- Docker
- `regctl` and `skopeo` (for full test coverage)
- Docker insecure registries configured: `localhost:5000`, `localhost:34000`

## Quick Start

```bash
# Mode 1 only (direct to registry)
python3 scripts/tests/run.py

# Mode 2 only (via nginx proxy)
python3 scripts/tests/run.py --nginx

# Both modes
python3 scripts/tests/run.py --all

# Skip containerd/crictl tests (CI runner limitation)
SKIP=test_containerd,test_crictl python3 scripts/tests/run.py

# List available test modules
python3 scripts/tests/run.py --list
```

## Environment Variables

| Variable | Default                | Description                          |
| -------- | ---------------------- | ------------------------------------ |
| `HOST`   | `localhost`            | Registry hostname                    |
| `RP`     | `5000`                 | Registry port (Mode 1)               |
| `PP`     | `34000`                | Proxy port (Mode 2)                  |
| `IMAGE`  | `dyrnq/distops:latest` | Docker image for reset               |
| `SKIP`   | (empty)                | Comma-separated test modules to skip |

## Test Modules

| Module                   | Description                                       |
| ------------------------ | ------------------------------------------------- |
| `test_api`               | Registry /v2/ health and /v2/\_catalog            |
| `test_token`             | Token auth flow                                   |
| `test_docker`            | docker login, push, pull, read-only               |
| `test_regctl`            | regctl manifest head                              |
| `test_skopeo`            | skopeo inspect                                    |
| `test_jwt`               | JWT CLI token generation                          |
| `test_admin`             | Admin API (instances, accounts, repos, artifacts) |
| `test_oauth`             | OAuth2 password/refresh/offline grant             |
| `test_oauth_errors`      | OAuth2 error scenarios (401, 400)                 |
| `test_containerd`        | containerd ctr pull via OAuth2                    |
| `test_crictl`            | crictl pull                                       |
| `test_proxy`             | Proxy registry pull-through                       |
| `test_proxy_push_denied` | Push rejection to proxy registries                |

## Local Testing (Build + Run)

```bash
# 1. Build the project
./build.sh

# 2. Build Docker image
./buildx.sh --local

# 3. Run integration tests (Mode 1 directly)
SKIP=test_containerd,test_crictl python3 scripts/tests/run.py

# 4. Run integration tests (Mode 2 via nginx proxy)
SKIP=test_containerd,test_crictl python3 scripts/tests/run.py --all
```

## VM Testing

Use `docker save` / `docker load` to transfer the image.

```bash
# 1. Build jar locally
cd /data/work/distops
./build.sh

# 2. Sync and build Docker image
cp target/distops-1.0.0.jar docker/rootfs/distops.jar
docker build -t dyrnq/distops:latest docker/

# 3. Save, scp, and load on VM
docker save dyrnq/distops:latest -o /tmp/distops_image.tar
scp /tmp/distops_image.tar vagrant@192.168.66.125:/tmp/
ssh vagrant@192.168.66.125 'docker load -i /tmp/distops_image.tar'

# 4. Start container on VM (host network mode)
ssh vagrant@192.168.66.125 '
  sudo rm -rf /data/distops
  docker rm -f distops-test 2>/dev/null
  docker run -d --name distops-test --restart always \
    --network host \
    -v /data/distops:/data/distops \
    -e TZ=Asia/Shanghai \
    -e JAVA_OPTS="-server -Xms256m -Xmx256m -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
      -Djava.net.preferIPv4Stack=true -Dspring.flyway.enabled=true" \
    -e OTEL_TRACES_EXPORTER=none \
    dyrnq/distops:latest
'

# 5. Fix authRealm (data may have stale realm from previous runs)
ssh vagrant@192.168.66.125 '
  curl -s -X POST -u admin:admin "http://localhost:12680/api/inst/update" \
    -d "id=1&name=default&port=5000&auth=token&authRealm=http://127.0.0.1:12680/auth/default"
  curl -s -X POST -u admin:admin "http://localhost:12680/api/inst/restart" -d "id=1"
  sleep 2
'

# 6. Copy test scripts and run
scp -r scripts/tests vagrant@192.168.66.125:/tmp/
ssh vagrant@192.168.66.125 \
  'cd /tmp/tests && SKIP=test_containerd,test_crictl python3 run.py'
```

## CI (GitHub Actions)

The CI pipeline runs in `.github/workflows/docker.yml`:

1. Maven build (`./mvnw clean package`)
2. Docker image build
3. Install test tools (regctl, skopeo)
4. Configure Docker insecure registries
5. Integration test Mode 1 (direct, skip containerd/crictl)
6. Integration test Mode 2 (via nginx, skip containerd/crictl)
7. Multi-arch image build and push to DockerHub

## Troubleshooting & Known Issues

### 1. authRealm Data Persistence

**Root cause:** The container volume `/data/distops` persists registry config files across restarts. When switching between Mode 1 (direct) and Mode 2 (nginx proxy), the `authRealm` in the generated `config.yml` may still point to the previous mode's port.

- After a Mode 2 run: `authRealm=http://127.0.0.1:34000/auth` (nginx port)
- Switching to Mode 1: the stale realm causes registry to send clients to port 34000, but nginx isn't running — all token/docker operations fail

**Symptoms (all Mode 1 tests fail):**
```
[FAIL] Token obtained
[FAIL] docker login
[FAIL] docker push
[FAIL] docker pull back
[FAIL] read-only login
[FAIL] read-only push denied
[FAIL] read-only pull ok
[FAIL] regctl manifest head
[FAIL] skopeo inspect
```

**Fix option A — API update (recommended):**
```bash
curl -s -X POST -u admin:admin "http://localhost:12680/api/inst/update" \
  -d "id=1&name=default&port=5000&auth=token&authRealm=http://127.0.0.1:12680/auth/default"
curl -s -X POST -u admin:admin "http://localhost:12680/api/inst/restart" -d "id=1"
sleep 2
curl -si http://localhost:5000/v2/_catalog 2>&1 | grep -i realm  # verify: should show 12680
```

**Fix option B — Fresh data:**
```bash
sudo rm -rf /data/distops
docker rm -f distops-test
# Then start container fresh
```

### 2. HTTPS_PROXY Required for Proxy Registries

**Root cause:** Proxy registry instances (docker.io, k8s.gcr.io, gcr.io, ghcr.io, quay.io) need an outbound HTTP proxy to reach their upstream. Without `HTTPS_PROXY`, these registry processes may silently exit, leaving nginx with no backend.

**Without proxy (expected behavior):**
- 3/6 proxy ports respond OK (port 15000 docker.io, 15004 ghcr.io, 15005 quay.io sometimes work via direct access)
- 3/6 proxy ports return 502 (port 35000, 35002, 35003 — upstream unreachable)
- `test_proxy.py` outputs: `[PASS] HTTPS_PROXY not set, skipping pull-through test`

**With proxy (all ports work):**
- All 6 proxy ports respond 200
- Pull-through tests can actually retrieve images from upstream
- `test_proxy.py` runs pull-through verification

```bash
export HTTPS_PROXY=http://proxy_host:proxy_port
export NO_PROXY=localhost,127.0.0.1,192.168.0.0/16
python3 scripts/tests/run.py
```

### 3. Nginx Container Lifecycle

**Root cause:** `setup_nginx.py` (`--nginx` mode) must restart the distops container after updating all instances' `authRealm` to nginx port 34000. The distops container restart kills the nginx container if it was started as a Docker dependency.

**What happens:**
1. First run: distops starts → nginx starts → authRealm updated → distops restarts → nginx container stops → script re-creates nginx → tests proceed
2. The script handles this automatically, but the restart adds ~10s delay
3. On subsequent runs (no docker rm), old authRealm may persist

### 4. Port Differences Between Modes

**Mode 1 (Direct):**
- Registry: `localhost:5000`
- Auth: `localhost:12680/auth/default` (Solon API)
- Proxy ports (direct): 15001-15005
- Nginx: not used

**Mode 2 (Nginx proxy):**
- Registry: `localhost:34000` (via nginx)
- Auth: `localhost:34000/auth/default` (via nginx → Solon API)
- Proxy ports (behind nginx): 35001-35005
- Nginx: required, auto-started by `setup_nginx.py`

When reading test failures, check which mode you're in — the same test from different modes uses different ports and proxy configurations.

### 5. Snapshots / Data Rollover

The test harness (`reset.py`) performs a full reset before each run:
1. Removes nginx and distops containers (`docker rm -f`)
2. Cleans `/data/distops` (needs `sudo`)
3. Starts a fresh distops container
4. Waits up to 60s for health check (`HTTP 200 OK`)

If `sudo` is unavailable, the data clean step fails silently and old config persists — see issue #1 above.

### 6. containerd / crictl Availability

The containerd and crictl tests are skipped by default because:
- Standard GitHub Actions runners do not have containerd installed
- containerd conflicts with Docker on the same host
- CI sets `SKIP=test_containerd,test_crictl` automatically
- Run with `SKIP=` (empty) to test on a host that has containerd