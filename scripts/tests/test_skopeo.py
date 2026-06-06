#!/usr/bin/env python3
"""Test skopeo inspect with token auth."""
import json
import os
import re
import subprocess

HOST = os.environ.get("HOST", "localhost")
IMG = os.environ.get("IMG", "docker-test")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def sh(cmd, timeout=30):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def get_token(port):
    r = sh(f"curl -si http://{HOST}:{port}/v2/_catalog", timeout=10)
    auth = r.stdout + r.stderr
    realm_m = re.search(r'realm="([^"]+)"', auth)
    svc_m = re.search(r'service="([^"]+)"', auth)
    realm = (realm_m.group(1) if realm_m else f"http://{HOST}:{port}/auth/default/").rstrip("/") + "/"
    svc = svc_m.group(1) if svc_m else "registry.docker.io"

    r2 = sh(f'curl -s -u "{TU}:{TP}" "{realm}token?service={svc}&scope=repository:library/{IMG}:pull"', timeout=10)
    try:
        return json.loads(r2.stdout).get("token", "")
    except json.JSONDecodeError:
        return ""


def run(port):
    results = []
    reg = f"{HOST}:{port}"

    token = get_token(port)
    if not token:
        results.append(("skopeo inspect", False))
        return results

    r = sh(
        f"skopeo inspect --tls-verify=false --creds {TU}:{TP} docker://{reg}/{IMG}:latest",
        timeout=60,
    )
    try:
        ok = "Digest" in json.loads(r.stdout)
    except (json.JSONDecodeError, AttributeError):
        ok = False
    results.append(("skopeo inspect", ok))
    return results


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("RP", 5000))
    for name, ok in run(port):
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
