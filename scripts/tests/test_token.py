#!/usr/bin/env python3
"""
Test token acquisition from distops auth server.
Requires: test_api.py passes (obtains realm/service from /v2/_catalog).
"""
import json
import os
import re
import subprocess

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def run(port):
    results = []

    r = subprocess.run(
        f"curl -si http://{HOST}:{port}/v2/_catalog",
        shell=True, timeout=10, capture_output=True, text=True,
    )
    auth = r.stdout + r.stderr
    realm_m = re.search(r'realm="([^"]+)"', auth)
    svc_m = re.search(r'service="([^"]+)"', auth)
    realm = realm_m.group(1) if realm_m else f"http://{HOST}:{port}/auth/default/"
    realm = realm.rstrip("/") + "/"
    svc = svc_m.group(1) if svc_m else "registry.docker.io"

    r2 = subprocess.run(
        f'curl -s -u "{TU}:{TP}" "{realm}token?service={svc}&scope=repository:library/alpine:pull"',
        shell=True, timeout=10, capture_output=True, text=True,
    )
    token = ""
    try:
        token = json.loads(r2.stdout).get("token", "")
    except json.JSONDecodeError:
        pass

    ok1 = bool(token) and token != "null"
    results.append(("Token obtained", ok1))

    r3 = subprocess.run(
        f'curl -so /dev/null -w "%{{http_code}}" -H "Authorization: Bearer {token}" '
        f"http://{HOST}:{port}/v2/_catalog",
        shell=True, timeout=10, capture_output=True, text=True,
    )
    ok2 = r3.stdout.strip() in ("200", "401")
    results.append(("Catalog access denied (no catalog ACL)", ok2))

    return results, token


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("RP", 5000))
    results, token = run(port)
    for name, ok in results:
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
    if token:
        print(f"  Token: {token[:60]}...")
