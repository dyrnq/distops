#!/usr/bin/env python3
"""
Test distops Admin API endpoints.
Uses admin/admin credentials (double base64 encoded) to:
  - Login and obtain JWT
  - Query instances, accounts, repos, artifacts, users
"""
import base64
import json
import os
import subprocess

HOST = os.environ.get("HOST", "localhost")
API = os.environ.get("API", f"{HOST}:12680")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin")


def b64encode(s):
    return base64.b64encode(s.encode()).decode()


def sh(cmd, timeout=15):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port=None):
    results = []
    base = f"http://{API}"

    # Admin login - credentials are double base64 encoded
    enc_user = b64encode(b64encode(ADMIN_USER))
    enc_pass = b64encode(b64encode(ADMIN_PASS))
    r = sh(
        f'curl -s "{base}/token/getToken?name={enc_user}&pass={enc_pass}"',
        timeout=15,
    )
    token = ""
    code = ""
    try:
        data = json.loads(r.stdout)
        code = data.get("code", 0)
        if code == 200:
            token = data.get("data", "")
    except (json.JSONDecodeError, AttributeError):
        token = ""

    ok1 = bool(token) and len(token) > 20
    results.append(("Admin login (get JWT)", ok1))

    if not ok1:
        results.append((f"API: list instances (login failed code={code})", False))
        results.append(("API: list accounts", False))
        results.append(("API: list repos", False))
        results.append(("API: list artifacts", False))
        results.append(("API: list users", False))
        results.append(("API: get global config templates", False))
        return results

    auth_header = f"Authorization: Bearer {token}"

    endpoints = [
        ("list instances", "/api/inst"),
        ("list accounts", "/api/account"),
        ("list repos", "/api/repo"),
        ("list artifacts", "/api/artifact"),
        ("list users", "/api/user"),
        ("get global config templates", "/api/globalConfig/getTemplates"),
    ]

    for label, path in endpoints:
        r = sh(f'curl -s -H "{auth_header}" "{base}{path}"', timeout=15)
        try:
            data = json.loads(r.stdout)
            ok = data.get("code") == 200
        except (json.JSONDecodeError, AttributeError):
            ok = False
        results.append((f"API: {label}", ok))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
