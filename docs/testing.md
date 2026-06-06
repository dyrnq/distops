# Integration Testing Guide

> **⚠️ 必读** — 以下情况会导致测试全部失败或容器无法启动：
> - 未设置 `IMAGE=dyrnq/distops:local` 环境变量 → 默认拉取 `:latest`（可能含旧代码）
> - VM 内存不足或未用 `-Xms1g -Xmx1g` → OOM
> - `sudo rm -rf /data/distops` 失败（无 sudo 权限）→ authRealm 残留
> - Docker ≤ 29.4.1 可用，**29.5.2 的 `docker push` 有 scope challenge 回归**
> - Mode 2 必须设置 `HTTPS_PROXY`，否则 proxy 实例 upstream 不通
> - 启动后等待 **12s** 以确保所有 registry 实例就绪


## Overview

Distops has a Python-based integration test suite in `scripts/tests/`. It supports two modes:

- **Mode 1 — Direct**: client connects directly to registry ports (5000)
- **Mode 2 — Proxy via nginx**: client connects through nginx (34000) which proxies to registry

## Prerequisites

- Python 3
- Docker
- `regctl`, `skopeo` (for full test coverage; `podman` is optional — test skips if unavailable)
- Docker insecure registries configured: `localhost:5000`, `localhost:34000`
- `HTTPS_PROXY` environment variable (required for Mode 2 proxy pull-through tests)

### ⚠️ IMAGE 环境变量

`reset.py` 默认拉取 `dyrnq/distops:latest`。如果 build 时改了 tag（如 `buildx.sh --image-name myimage:dev`），必须：

```bash
IMAGE=myimage:dev python3 scripts/tests/run.py
```

否则 `reset.py` 会卡住 30s 后抛出 `TimeoutExpired`，**整个 run.py 退出**，所有测试不执行。
最常见的本地错误：build 了 `:local` 却没传 `IMAGE=dyrnq/distops:local`。


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
| `test_containerd`        | containerd ctr pull via OAuth2                    |
| `test_crictl`            | crictl pull (CRI-compatible runtime)              |
| `test_podman`            | podman login, push, pull (daemonless container)   |
| `test_skopeo_copy`       | skopeo copy as alternative push method            |
| `test_proxy`             | Proxy registry pull-through                       |
| `test_proxy_push_denied` | Push rejection to proxy registries                |

## Local Testing (Build + Run)

> **Note:** For local development/testing, use `docker/Dockerfile.dev` which builds faster
> (skips downloading s6-overlay/distribution/skopeo binaries, uses apt packages instead).
> This file is **not tracked by git** — only `docker/Dockerfile` is committed.
>
> ```bash
> # Dev build:
> cp target/distops-1.0.0.jar docker/rootfs/distops.jar
> docker build -f docker/Dockerfile.dev -t dyrnq/distops:local docker/
>
> # Production build:
> cp target/distops-1.0.0.jar docker/rootfs/distops.jar
> docker build -t dyrnq/distops:local docker/
> ```

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



## Test Module Dependencies

Test modules are executed in order. Later modules depend on images pushed by earlier ones:

```
test_docker     → pushes docker-test:latest (alpine)
  ├── test_regctl      → reads docker-test:latest
  ├── test_skopeo      → reads docker-test:latest
  └── test_skopeo_copy → pushes skopeo-copy-test:latest

test_containerd → pushes library/ctr-test:latest
test_crictl     → pushes library/crictl-test:latest
test_podman     → pushes podman-test:latest (from docker-daemon)
```

**Image name isolation:** Each module uses a unique image name to avoid cross-contamination. When a module fails, downstream modules that depend on its image will also fail.

## VM Connection

### Avoid Port Confusion (Multiple Vagrant VMs)

**Root cause:** Multiple Vagrant projects on the same host may share the same `insecure_private_key` and use overlapping port forwards or NAT. `ssh -p 2222 vagrant@127.0.0.1` may connect to the wrong VM.

**Checklist:**
1. Always verify the VM hostname first: `ssh ... 'hostname'` — must match the Vagrantfile's `config.vm.hostname`
2. Use `vagrant ssh-config` from the project directory to get the correct port
3. Prefer the static bridge IP (e.g. `192.168.66.125`) for direct SSH
4. The distops Vagrantfile defines:
   - **Hostname:** `server`
   - **IP:** `192.168.66.125`
   - **SSH port:** `2202` (Vagrant-assigned, check with `vagrant ssh-config`)

**Example — correct connection:**
```bash
ssh -i insecure_private_key vagrant@192.168.66.125
# or
ssh -i insecure_private_key -p 2202 vagrant@127.0.0.1
```

**Symptom of wrong VM:** Docker version mismatch (e.g. 29.5.2 vs expected 29.4.1), hostname doesn't match `server`, IP addresses differ.

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
    -e JAVA_OPTS="-server -Xms1g -Xmx1g -Djava.awt.headless=true \
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


### ⚠️ Mode Switching Requires Data Clean

Mode 1 和 Mode 2 **不能连续跑**。Mode 2 的 `setup_nginx.py` 会把所有实例的 `authRealm` 改为 nginx 端口（34000），残留数据会导致 Mode 1 全部失败。

**正确做法：**
```bash
# Mode 1
IMAGE=dyrnq/distops:local python3 scripts/tests/run.py

# Mode 2 — 全新容器
docker rm -f distops-test && sudo rm -rf /data/distops
HTTPS_PROXY=... IMAGE=dyrnq/distops:local python3 scripts/tests/run.py --nginx
```

### ⚠️ authRealm 残留排查

如果 Mode 1 的 `/v2/` 可访问但所有 token/docker 操作都失败，检查 realm：

```bash
curl -s http://localhost:5000/v2/ -D- | grep realm
# 必须是 http://xxx:12680/auth/default
# 如果是 http://xxx:34000/auth/default → Mode 2 残留
```

清除：
```bash
sudo rm -rf /data/distops && docker rm -f distops-test
```
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
   - 冷启动需要 40-90 秒（registry 6 进程 + Flyway 迁移 + JWK 生成）
   - 如果 `reset.py` 报告 `distops startup timeout`：重新执行即可（容器可能正在初始化）

If `sudo` is unavailable, the data clean step fails silently and old config persists — see issue #1 above.

### 6. Docker 29.5.2 Scope Challenge Regression

**Root cause:** Docker 29.5.2 does not properly handle the Bearer scope challenge (401 with `scope=repository:X:pull,push`) when pushing to insecure registries with `--network host`. Earlier versions (28.1.1, 29.4.1) work correctly.

**Symptom:**
```
push access denied, repository does not exist or may require authorization:
server message: insufficient_scope: authorization failed
```

**Mitigation:** `test_docker.py` falls back to `skopeo copy` when `docker push` fails. `test_containerd.py` and `test_crictl.py` also have skopeo fallbacks for the push step.

**Resolution:** Use Docker ≤ 29.4.1 (the distops Vagrant VM ships 29.4.1). Docker 29.5.2 is affected on insecure registries regardless of auth configuration. This is a Docker client regression tracked at moby/moby.

### 7. Memory (OOM) with Heavy Test Suites

**Root cause:** Running containerd `ctr pull` alongside 6 proxy registry instances and auth server in the same JVM consumes significant heap. Default `-Xms256m -Xmx256m` was insufficient for the full test suite.

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
worker thread error
```

**Fix:** `reset.py` now uses `-Xms1g -Xmx1g` for test containers.

### 8. Mode 1 Proxy Instances & HTTPS_PROXY

Even in Mode 1 (no nginx), the distops container starts **6 proxy registry instances** (docker.io, gcr.io, etc.) in supervisord. Without `HTTPS_PROXY`, 3 of these fail to connect upstream and log periodic timeouts. This does not block Mode 1 tests (the test suite skips proxy pull-through when `HTTPS_PROXY` is unset), but the proxy restart noise clutters logs.

**Recommendation:** Always set `HTTPS_PROXY` even for Mode 1:
```bash
HTTPS_PROXY=http://192.168.6.111:7890 python3 scripts/tests/run.py
```
### 9. Event Processing Blocks HTTP Threads

**Root cause:** Distribution registry sends synchronous event notifications (pull/push events) to the auth server. Processing these events synchronously in the controller thread blocks HTTP responses, causing the registry to retry and eventually exhaust the HTTP thread pool.

**Symptom:**
- `curl http://localhost:12680/` hangs (no response)
- Registry logs: `retryingsink: error writing event, retrying`
- `docker login` times out with `Client.Timeout exceeded while awaiting headers`

**Fix:** Event processing is now asynchronous (thread pool, 2 threads). Pull events are skipped entirely (push/mount events only are processed) to reduce load.

### 10. Template Name Collision (FreeMarker)

**Root cause:** `InstService.enable()` creates two FreeMarker `Template` objects both named `"yaml"` — one for the registry config YAML and one for the supervisor INI template. When the second template contains invalid FreeMarker syntax (e.g. standalone `<#elseif>` outside any `<#if>` block), FreeMarker reports the error against the first template's name.

**Symptom:**
```
Syntax error in template "yaml" in line 2, column 76:
Encountered "<#elseif ", but was expecting one of these patterns: <EOF>, <ATTEMPT>, <IF>...
```

**Fix:** The INI template is now named `"ini"` to avoid name collision. **Incorrect syntax (two separate blocks):**
```
environment = OTEL_TRACES_EXPORTER=none<#if env_lines??>,${env_lines}</#if><#elseif inst.env??>,${inst.env}</#if>
```
`<#elseif>` 在第一个 `</#if>` 之外，FreeMarker 无法解析。

**Correct (single block):**
```
environment = OTEL_TRACES_EXPORTER=none<#if env_lines??>,${env_lines}<#elseif inst.env??>,${inst.env}</#if>
```

### 11. authRealm Auto-Detection on Docker Bridge

**Root cause:** When `authRealm` is not explicitly configured, the code auto-detects the first non-loopback IP. In `--network host` mode, this is typically a Docker bridge IP (e.g. `172.18.0.1`) rather than `localhost`.

**Symptom:** Docker clients request tokens from `http://172.18.0.1:12680/auth/default`. This works on the host but may fail in some VM/network configurations.

**Workaround:** Set `authRealm` explicitly via the admin API. The test suite accepts either auto-detected or explicit realms.

### 12. JWT Token Validation — Cookie vs Bearer Header

**Root cause:** `JwtInterceptor` originally only read JWT tokens from cookies (`ctx.cookie(...)`). Admin API test scripts used `Authorization: Bearer` headers which were silently ignored.

**Fix:** `JwtInterceptor` now prefers `Authorization: Bearer` header, falling back to cookie. Both `test_admin.py` and direct API calls work.

> **Note** — `test_admin.py`, `test_proxy.py`, and `setup_nginx.py` all use `Authorization: Bearer` headers.
> These tests depend on this bugfix being deployed. If the JwtInterceptor regression is ever reverted,
> all three modules will fail with HTTP 401.



### 13. podman + HTTPS_PROXY

**Mode 1:** `test_podman.py` imports alpine from `docker-daemon:alpine:3.21` — no network required.

**Mode 2:** podman push/pull goes through nginx to the distops registry (localhost:34000). No HTTPS_PROXY needed for podman itself since all traffic is local.

### 14. podman pull from Docker Hub Timeout

**Root cause:** The test VM may not have direct internet access to Docker Hub. `podman pull alpine:3.21` from registry-1.docker.io times out.

**Fix:** `test_podman.py` imports the alpine image from the local Docker daemon instead:
```python
podman pull docker-daemon:alpine:3.21   # avoids Docker Hub dependency
```

### 15. Mode 2 Proxy Push Denied 502

**Root cause:** Long test runs (Mode 1 + Mode 2 back-to-back) may cause proxy registry instances to restart due to upstream timeouts (gcr.io, k8s.gcr.io require stable outbound connectivity). Nginx returns 502 when backends are unavailable.

**Workaround:** Run Mode 2 on a freshly started container. In CI, Mode 1 and Mode 2 use separate containers.

### 17. containerd / crictl Availability

The containerd and crictl tests are skipped by default because:
- Standard GitHub Actions runners do not have containerd installed
- containerd conflicts with Docker on the same host
- CI sets `SKIP=test_containerd,test_crictl` automatically
- Run with `SKIP=` (empty) to test on a host that has containerd