#!/usr/bin/env python3
"""
Test OAuth2 POST endpoint (grant_type=password / refresh_token / offline).
Target: POST /auth/{instName}/?  with form-encoded body.
"""
import json
import os
import subprocess

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def run(port=None):
    """port is unused here; kept for runner compatibility."""
    results = []
    base = f"http://{API}/auth/default/"

    # 1. password grant
    r = subprocess.run(
        f'curl -s -X POST "{base}" -d "grant_type=password&username={TU}&password={TP}&service=registry.docker.io&scope=repository:alpine:pull"',
        shell=True, timeout=10, capture_output=True, text=True,
    )
    try:
        d = json.loads(r.stdout)
        ok1 = "access_token" in d
    except json.JSONDecodeError:
        ok1 = False
    results.append(("OAuth password grant", ok1))

    # 2. offline mode returns refresh_token
    r = subprocess.run(
        f'curl -s -X POST "{base}" -d "grant_type=password&username={TU}&password={TP}&service=registry.docker.io&scope=repository:alpine:pull&access_type=offline"',
        shell=True, timeout=10, capture_output=True, text=True,
    )
    try:
        d = json.loads(r.stdout)
        ok2 = "access_token" in d and "refresh_token" in d
        refresh = d.get("refresh_token", "")
    except json.JSONDecodeError:
        ok2 = False
        refresh = ""
    results.append(("OAuth offline returns refresh_token", ok2))

    # 3. refresh_token grant
    if refresh:
        r = subprocess.run(
            f'curl -s -X POST "{base}" -d "grant_type=refresh_token&refresh_token={refresh}&service=registry.docker.io&scope=repository:alpine:pull"',
            shell=True, timeout=10, capture_output=True, text=True,
        )
        try:
            d = json.loads(r.stdout)
            ok3 = "access_token" in d
        except json.JSONDecodeError:
            ok3 = False
        results.append(("OAuth refresh_token grant", ok3))
    else:
        results.append(("OAuth refresh_token grant (skipped, no refresh)", False))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
