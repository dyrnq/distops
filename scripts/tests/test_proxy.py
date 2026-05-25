#!/usr/bin/env python3
"""Test distops Proxy Registry instances."""
import json, os, re, subprocess, base64

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin")
HTTPS_PROXY = os.environ.get("HTTPS_PROXY", "")
NO_PROXY = os.environ.get("NO_PROXY", "")


def b64encode(s):
    return base64.b64encode(s.encode()).decode()


def sh(cmd, timeout=30):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def get_admin_token():
    enc_user = b64encode(b64encode(ADMIN_USER))
    enc_pass = b64encode(b64encode(ADMIN_PASS))
    r = sh(f'curl -s "{API}/token/getToken?name={enc_user}&pass={enc_pass}"', timeout=15)
    try:
        data = json.loads(r.stdout)
        if data.get("code") == 200:
            return data.get("data", "")
    except (json.JSONDecodeError, AttributeError):
        pass
    return ""


def run(port=None, proxy_ports=None):
    results = []
    container = sh("docker ps --format '{{.Names}}' | grep distops | head -1", timeout=10).stdout.strip()
    if not container:
        results.append(("distops container running", False))
        return results
    results.append(("distops container running", True))

    # 1. Supervisor status
    r = sh(f"docker exec {container} supervisorctl status", timeout=15)
    proxy_procs = [l for l in r.stdout.split("\n") if "proxy" in l]
    running = [l for l in proxy_procs if "RUNNING" in l]
    results.append((f"Supervisor proxy processes ({len(running)}/{len(proxy_procs)} RUNNING)", len(running) > 0))

    # 2. Ports
    if proxy_ports:
        unique_ports = sorted(proxy_ports)
        results.append((f"Proxy test ports: {unique_ports}", True))
    else:
        r2 = sh(f"docker exec {container} ss -tlnp", timeout=10)
        ports = re.findall(r':(1500[0-5])', r2.stdout)
        unique_ports = sorted(set(ports))
        results.append((f"Proxy ports listening: {unique_ports}", len(unique_ports) > 0))

    # 3. /v2/
    ok_auth = 0
    for p in unique_ports:
        r3 = sh(f"curl -so /dev/null -w '%{{http_code}}' http://{HOST}:{p}/v2/", timeout=10)
        if r3.stdout.strip() == "200":
            ok_auth += 1
    results.append((f"/v2/ 200 OK on {ok_auth}/{len(unique_ports)} ports", ok_auth > 0))

    # 4. Pull-through
    if HTTPS_PROXY:
        results.append(("HTTPS_PROXY configured, testing pull-through...", True))
        token = get_admin_token()
        if not token:
            results.append(("Admin login for env update", False))
            return results

        auth = f"Authorization: Bearer {token}"
        env_parts = [f"HTTPS_PROXY={HTTPS_PROXY}"]
        if NO_PROXY:
            env_parts.append(f"NO_PROXY=" + chr(34) + NO_PROXY + chr(34))
        env_str = "\r\n".join(env_parts)

        # List insts
        r = sh(f'curl -s -H "{auth}" "http://{API}/api/inst?page=1&limit=100"', timeout=15)
        insts = []
        try:
            data = json.loads(r.stdout)
            insts = [i for i in (data.get("data", []) or data.get("list", []))
                     if isinstance(i, dict) and i.get("port", 0) >= 15000]
        except json.JSONDecodeError:
            pass

        # Update env via JSON POST
        ok_update = 0
        for inst in insts:
            payload = json.dumps({
                "id": inst["id"],
                "name": inst["name"],
                "port": inst["port"],
                "env": env_str
            })
            curl_cmd = f"curl -s -X POST -H '{auth}' -H 'Content-Type: application/json' -d '{payload}' 'http://{API}/api/inst/update'"
            r = sh(curl_cmd, timeout=15)
            try:
                if json.loads(r.stdout).get("code") == 200:
                    ok_update += 1
            except json.JSONDecodeError:
                pass

        if ok_update > 0:
            ids = ",".join(str(i["id"]) for i in insts)
            sh(f'curl -s -X POST -H "{auth}" "http://{API}/api/inst/restart?id={ids}"', timeout=30)
            sh(f"docker exec {container} supervisorctl update", timeout=15)
            sh("sleep 5", timeout=10)

            r = sh(f"docker exec {container} supervisorctl status", timeout=15)
            stopped = [l.split()[0] for l in r.stdout.split("\n") if "STOPPED" in l or "FATAL" in l]
            for name in stopped:
                sh(f"docker exec {container} supervisorctl start {name}", timeout=30)
            sh("sleep 3")

        results.append((f"Updated env on {ok_update}/{len(insts)} instances", ok_update > 0))

        # Test pull-through with known images per registry
        pull_images = {
            15000: "library/alpine:latest",
            15001: "pause:3.9",
            15005: "prometheus/busybox:latest",
        }
        ok_pull = 0
        for p, img in pull_images.items():
            r = sh(f"skopeo inspect --tls-verify=false docker://{HOST}:{p}/{img}", timeout=60)
            try:
                if json.loads(r.stdout).get("Digest"):
                    ok_pull += 1
            except json.JSONDecodeError:
                pass
        results.append((f"skopeo pull-through ({ok_pull}/{len(pull_images)})", ok_pull > 0))
    else:
        results.append(("HTTPS_PROXY not set, skipping pull-through test", True))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
