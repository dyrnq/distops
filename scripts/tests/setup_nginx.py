#!/usr/bin/env python3
"""
Start nginx proxy container that forwards all traffic:
  - /auth/ -> distops API (:12680) /auth/default/
  - /v2/   -> registry (:5000)

Uses --network host to avoid docker bridge routing issues.
Updates all registry instances authRealm to 127.0.0.1:34000,
so all auth traffic goes through nginx.
"""
import os
import subprocess
import time
import shutil
import tempfile

HOST = os.environ.get("HOST", "localhost")
RP = int(os.environ.get("RP", 5000))
PP = int(os.environ.get("PP", 34000))
API_PORT = int(os.environ.get("API_PORT", 12680))
DISTOPS_CONTAINER = os.environ.get("DISTOPS_CONTAINER", "distops-test")


def sh(cmd, timeout=15):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def gen_nginx_conf():
    proxy_pairs = "\n".join(
        "  server {\n"
        f"    listen {np};\n"
        "    client_max_body_size 0;\n"
        "    chunked_transfer_encoding on;\n"
        "    location / {\n"
        f"      proxy_pass http://127.0.0.1:{rp};\n"
        "      proxy_set_header Host $http_host;\n"
        "    }\n"
        "  }\n"
        for rp, np in [(15000,35000),(15001,35001),(15002,35002),(15003,35003),(15004,35004),(15005,35005)]
    )

    return (
        "upstream api { server 127.0.0.1:%d; }\n"
        "\n"
        "map $upstream_http_docker_distribution_api_version $v { 'registry/2.0' ''; default registry/2.0; }\n"
        "\n"
        "server {\n"
        "  listen %d;\n"
        "  client_max_body_size 0;\n"
        "  chunked_transfer_encoding on;\n"
        "\n"
        "  location /auth/default {\n"
        "    proxy_pass http://api;\n"
        "    proxy_set_header Host $http_host;\n"
        "    proxy_set_header X-Real-IP $remote_addr;\n"
        "    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
        "    proxy_set_header X-Forwarded-Proto $scheme;\n"
        "  }\n"
        "\n"
        "  location /v2/ {\n"
        "    add_header 'Docker-Distribution-Api-Version' $v always;\n"
        "    proxy_pass http://127.0.0.1:%d;\n"
        "    proxy_set_header Host $http_host;\n"
        "    proxy_set_header X-Real-IP $remote_addr;\n"
        "    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
        "    proxy_set_header X-Forwarded-Proto $scheme;\n"
        "    proxy_read_timeout 900;\n"
        "  }\n"
        "}\n"
        + proxy_pairs +
        "\n"
    ) % (API_PORT, PP, RP)


def update_inst_via_api(realm_url):
    """Update all registry instances authRealm via admin API (not direct DB)."""
    import base64, json
    api_base = f"http://127.0.0.1:{API_PORT}"

    enc_user = base64.b64encode(base64.b64encode(b"admin")).decode()
    enc_pass = base64.b64encode(base64.b64encode(b"admin")).decode()
    r = sh(f'curl -s "{api_base}/token/getToken?name={enc_user}&pass={enc_pass}"', timeout=15)
    token = json.loads(r.stdout).get("data", "")
    if not token:
        print("[FAIL] Cannot get admin token")
        return False
    auth = f"Authorization: Bearer {token}"

    r = sh(f'curl -s -H "{auth}" "{api_base}/api/inst"', timeout=15)
    data = json.loads(r.stdout)
    if data.get("code") != 200:
        print("[FAIL] Cannot list instances")
        return False

    for inst in data.get("data", []):
        inst_id = inst["id"]
        inst_name = inst["name"]
        payload = {"id": inst_id, "authRealm": realm_url}
        if inst.get("auth"):
            payload["auth"] = inst["auth"]
        payload_json = json.dumps(payload)
        r = sh(
            f'curl -s -H "{auth}" -H "Content-Type: application/json" '
            f'-X POST "{api_base}/api/inst/update" -d \'{payload_json}\'',
            timeout=15,
        )
        print(f"  Updated {inst_name} (port {inst.get('port')}): authRealm={realm_url}")
    return True

def run(update_realm=True):
    rc = sh("docker ps --format '{{.Names}}'", timeout=10)
    if "nginx" in rc.stdout.split():
        print("[INFO] nginx proxy already running")
        return True

    print("[INFO] Starting nginx proxy (--network host)...")

    conf = gen_nginx_conf()

    conf_dir = "/tmp/nginx-distops-conf"
    if os.path.exists(conf_dir):
        shutil.rmtree(conf_dir)
    os.makedirs(conf_dir, exist_ok=True)
    with open(os.path.join(conf_dir, "default.conf"), "w") as f:
        f.write(conf)

    sh("docker rm -f nginx 2>/dev/null || true", timeout=10)
    sh(
        "docker run -d --name nginx --restart always "
        "--network host "
        "-v %s:/etc/nginx/conf.d:ro "
        "nginx:1.27.0-alpine" % conf_dir,
        timeout=30,
    )
    time.sleep(3)

    rc2 = sh("docker ps --format '{{.Names}}'", timeout=10)
    ok = "nginx" in rc2.stdout.split()
    if not ok:
        print("[FAIL] nginx container not running")
        return False
    print("[ OK ] nginx proxy started")

    r = sh("curl -so /dev/null -w '%%{http_code}' http://127.0.0.1:%d/v2/" % PP, timeout=10)
    print("[INFO] nginx /v2/ on 127.0.0.1:%d -> HTTP %s" % (PP, r.stdout.strip()))

    if ok and update_realm:
        print("[INFO] Updating all registry instances authRealm to 127.0.0.1:%d..." % PP)

        realm_url = "http://127.0.0.1:%d/auth" % PP
        ok = update_inst_via_api(realm_url)


        print("[INFO] Restarting distops container to apply config...")
        sh("docker restart %s" % DISTOPS_CONTAINER, timeout=30)
        time.sleep(8)
        sh("docker exec %s supervisorctl restart all" % DISTOPS_CONTAINER, timeout=30)
        time.sleep(5)
        print("[INFO] All instances restarted")

    return ok


if __name__ == "__main__":
    run()
