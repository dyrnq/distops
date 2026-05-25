#!/usr/bin/env python3
"""
Test that push to proxy registry is denied.
Proxy registries are pull-through caches and must reject write operations
with HTTP 405 Method Not Allowed.
Tests only on ports that are actually listening.
"""
import os
import re
import subprocess

HOST = os.environ.get("HOST", "localhost")


def sh(cmd, timeout=10):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port=None, proxy_ports=None):
    results = []

    container = sh("docker ps --format '{{.Names}}' | grep distops | head -1", timeout=5).stdout.strip()
    if not container:
        results.append(("distops container not found", False))
        return results

    # Determine ports to test
    if proxy_ports:
        live_ports = sorted(proxy_ports)
    else:
        r = sh(f"docker exec {container} ss -tlnp", timeout=5)
        live_ports = sorted(set(re.findall(r':(1500[0-5])', r.stdout)))

    if not live_ports:
        results.append(("No live proxy ports to test", False))
        return results

    for p in live_ports:
        # POST blob upload should return 405 on proxy registries
        r = sh(
            f"curl -s -X POST -o /dev/null -w '%{{http_code}}' "
            f"'http://{HOST}:{p}/v2/test-push-verify-project/blobs/uploads/'",
            timeout=5,
        )
        code = r.stdout.strip()
        ok = code == "405"
        results.append((f"POST blob upload to :{p} = {code}", ok))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
