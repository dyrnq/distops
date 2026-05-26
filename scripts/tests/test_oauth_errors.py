#!/usr/bin/env python3
"""
Test OAuth2 POST endpoint error handling.
Covers: invalid credentials, missing fields, expired tokens, unknown grant types.
"""
import json
import os
import subprocess

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def run(port=None):
    results = []
    base = f"http://{API}/auth/default/"

    def post(data, expect_code):
        r = subprocess.run(
            f'curl -s -o /dev/null -w "%{{http_code}}" -X POST "{base}" -d "{data}"',
            shell=True, timeout=10, capture_output=True, text=True,
        )
        code = r.stdout.strip()
        return code == str(expect_code), code

    # 1. Wrong password
    ok, code = post(f"grant_type=password&username={TU}&password=wrong&service=x&scope=repository:a:pull", 401)
    results.append((f"Wrong password → 401 (got {code})", ok))

    # 2. Empty username
    ok, code = post("grant_type=password&username=&password=x&service=x", 401)
    results.append((f"Empty username → 401 (got {code})", ok))

    # 3. Missing grant_type
    ok, code = post(f"username={TU}&password={TP}", 200)
    results.append((f"No grant_type falls through to anonymous (200) (got {code})", ok))

    # 4. Unknown grant_type
    ok, code = post(f"grant_type=client_credentials&username={TU}&password={TP}", 400)
    results.append((f"Unknown grant_type → 400 (got {code})", ok))

    # 5. refresh_token with invalid token
    ok, code = post("grant_type=refresh_token&refresh_token=invalid_base64!!!&service=x", 401)
    results.append((f"Invalid refresh_token → 401 (got {code})", ok))

    # 6. refresh_token missing
    ok, code = post("grant_type=refresh_token&service=x", 400)
    results.append((f"Missing refresh_token → 400 (got {code})", ok))

    # 7. Expired refresh_token (expires=1 = year 1970)
    import base64
    expired_payload = base64.urlsafe_b64encode(b"test:1:refresh").decode().rstrip("=")
    ok, code = post(f"grant_type=refresh_token&refresh_token={expired_payload}&service=x", 401)
    results.append((f"Expired refresh_token → 401 (got {code})", ok))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
