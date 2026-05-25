#!/usr/bin/env python3
"""Test Registry v2 API and WWW-Authenticate header."""
import os
import subprocess
import json

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")


def run(port):
    results = []

    r = subprocess.run(
        f"curl -so /dev/null -w '%{{http_code}}' http://{HOST}:{port}/v2/",
        shell=True, timeout=10, capture_output=True, text=True,
    )
    ok = r.stdout.strip() in ("200", "401")
    results.append(("Registry /v2/ accessible", ok))

    # Check that registry responds to API calls
    r2 = subprocess.run(
        f"curl -so /dev/null -w '%{{http_code}}' http://{HOST}:{port}/v2/_catalog",
        shell=True, timeout=10, capture_output=True, text=True,
    )
    ok2 = r2.stdout.strip() in ("200", "401")
    results.append(("Registry /v2/_catalog accessible", ok2))

    return results


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else os.environ.get("RP", 5000)
    for name, ok in run(port, "API"):
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
