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

    # --- Helper to get admin token for API calls ---
    def get_admin_token():
        import base64
        enc_user = base64.b64encode(base64.b64encode(b"admin")).decode()
        enc_pass = base64.b64encode(base64.b64encode(b"admin")).decode()
        r = subprocess.run(
            f'curl -s "http://{API}/token/getToken?name={enc_user}&pass={enc_pass}"',
            shell=True, timeout=10, capture_output=True, text=True,
        )
        try:
            return json.loads(r.stdout).get("data", "")
        except json.JSONDecodeError:
            return ""

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

    
    # 4. Revoke via API: call account/revoke then verify token is rejected
    if refresh and ok3:
        try:
            import base64 as _b64
            admin_enc = _b64.b64encode(_b64.b64encode(b"admin")).decode()
            r = subprocess.run(
                f'curl -s "http://{API}/token/getToken?name={admin_enc}&pass={admin_enc}"',
                shell=True, timeout=10, capture_output=True, text=True,
            )
            admin_jwt = json.loads(r.stdout).get("data", "")
            if admin_jwt:
                # Find test account id
                r = subprocess.run(
                    f'curl -s -H "Authorization: Bearer {admin_jwt}" "http://{API}/api/account?page=1&limit=50"',
                    shell=True, timeout=10, capture_output=True, text=True,
                )
                accts = json.loads(r.stdout).get("data", [])
                test_id = None
                for a in accts:
                    if a.get("username") == TU:
                        test_id = a.get("id")
                        break
                if test_id:
                    # Revoke tokens
                    subprocess.run(
                        f'curl -s -X POST -H "Authorization: Bearer {admin_jwt}" "http://{API}/api/account/revoke?id={test_id}"',
                        shell=True, timeout=10, capture_output=True, text=True,
                    )
                    # Verify rejected
                    r = subprocess.run(
                        f'curl -s -X POST "{base}" -d "grant_type=refresh_token&refresh_token={refresh}&service=registry.docker.io&scope=repository:alpine:pull"',
                        shell=True, timeout=10, capture_output=True, text=True,
                    )
                    try:
                        d = json.loads(r.stdout)
                        ok4 = "access_token" not in d
                    except json.JSONDecodeError:
                        ok4 = True  # empty response = rejected
                    results.append(("OAuth revoked refresh_token rejected", ok4))
                else:
                    results.append(("OAuth revoke test (account not found)", False))
            else:
                results.append(("OAuth revoke test (no admin token)", False))
        except Exception as e:
            results.append(("OAuth revoke test (error)", False))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
