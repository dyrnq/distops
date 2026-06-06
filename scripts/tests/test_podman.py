#!/usr/bin/env python3
"""
Test podman login, push, pull operations against distops registry.
podman is a daemonless container engine compatible with OCI registries.
"""
import os
import subprocess

HOST = os.environ.get("HOST", "localhost")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")
RU = os.environ.get("RU", "read")
RPW = os.environ.get("RPW", "test")
IMG = os.environ.get("IMG", "podman-test")


def sh(cmd, timeout=60):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port=None):
    results = []
    RP = int(os.environ.get("RP", 5000))
    p = port if port is not None else RP
    reg = f"{HOST}:{p}"

    # 1. Check podman available
    r = sh("command -v podman", timeout=5)
    if r.returncode != 0:
        results.append(("podman not available - skipped", True))
        return results

    # 2. podman login
    sh(f"podman logout {reg} 2>/dev/null", timeout=10)
    r = sh(f"echo '{TP}' | podman login '{reg}' -u '{TU}' --password-stdin --tls-verify=false", timeout=20)
    ok1 = "Login Succeeded" in (r.stdout + r.stderr)
    results.append(("podman login", ok1))

    # 3. Import alpine from docker-daemon (avoids Docker Hub pull)
    sh("podman pull docker-daemon:alpine:3.21 2>&1", timeout=60)
    sh(f"podman tag alpine:3.21 {reg}/{IMG}:latest 2>/dev/null", timeout=10)

    # 4. podman push
    r = sh(f"podman push --tls-verify=false {reg}/{IMG}:latest 2>&1", timeout=120)
    ok2 = r.returncode == 0
    results.append(("podman push", ok2))

    # 5. podman pull back
    if ok2:
        sh(f"podman rmi {reg}/{IMG}:latest 2>/dev/null", timeout=10)
        r = sh(f"podman pull --tls-verify=false {reg}/{IMG}:latest 2>&1", timeout=120)
        ok3 = r.returncode == 0
    else:
        ok3 = False
    results.append(("podman pull back", ok3))

    # 6. Read-only login
    sh(f"podman logout {reg} 2>/dev/null", timeout=10)
    r = sh(f"echo '{RPW}' | podman login '{reg}' -u '{RU}' --password-stdin --tls-verify=false", timeout=20)
    ok4 = "Login Succeeded" in (r.stdout + r.stderr)
    results.append(("podman read-only login", ok4))

    # 7. Read-only push should fail
    r = sh(f"podman push --tls-verify=false {reg}/{IMG}:latest 2>&1", timeout=60)
    outc = (r.stdout + r.stderr).lower()
    ok5 = any(w in outc for w in ["denied", "unauthorized", "forbidden"])
    results.append(("podman read-only push denied", ok5))

    # 8. Read-only pull should succeed
    r = sh(f"podman pull --tls-verify=false {reg}/{IMG}:latest 2>&1", timeout=60)
    ok6 = r.returncode == 0
    results.append(("podman read-only pull ok", ok6))

    # Cleanup
    sh(f"podman rmi {reg}/{IMG}:latest 2>/dev/null", timeout=10)
    sh(f"podman rmi alpine:3.21 2>/dev/null", timeout=10)

    return results


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("RP", 5000))
    for name, ok in run(port):
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
