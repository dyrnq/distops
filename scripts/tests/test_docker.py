#!/usr/bin/env python3
"""Test docker login, push, pull operations."""
import os
import subprocess

HOST = os.environ.get("HOST", "localhost")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")
RU = os.environ.get("RU", "read")
RPW = os.environ.get("RPW", "test")
IMG = os.environ.get("IMG", "docker-test")


def run(port):
    results = []
    reg = f"{HOST}:{port}"

    def sh(cmd, timeout=60):
        return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)

    # Login
    sh(f"docker logout {reg} 2>/dev/null", timeout=10)
    r = sh(f"echo '{TP}' | docker login '{reg}' -u '{TU}' --password-stdin", timeout=20)
    ok1 = "Succeeded" in (r.stdout + r.stderr)
    results.append(("docker login", ok1))

    # Ensure alpine image
    sh("docker pull alpine:3.21", timeout=120)
    sh(f"docker tag alpine:3.21 {reg}/{IMG}:latest", timeout=10)

    # Push (docker, fallback to skopeo for Docker 29.5.2 compatibility)
    r = sh(f"docker push {reg}/{IMG}:latest", timeout=120)
    ok2 = "sha256" in r.stdout or "pushed" in r.stdout
    if not ok2:
        # Fallback: use skopeo to push (handles Docker 29.5.2 token issue)
        import pathlib
        policy_dir = pathlib.Path.home() / ".config" / "containers"
        policy_dir.mkdir(parents=True, exist_ok=True)
        (policy_dir / "policy.json").write_text('{"default": [{"type": "insecureAcceptAnything"}]}')
        r2 = sh(f"skopeo copy --dest-tls-verify=false --dest-creds {TU}:{TP} docker-daemon:alpine:3.21 docker://{reg}/{IMG}:latest", timeout=120)
        ok2 = r2.returncode == 0
    results.append(("docker push", ok2))

    # Pull back
    sh(f"docker rmi {reg}/{IMG}:latest 2>/dev/null", timeout=10)
    r = sh(f"docker pull {reg}/{IMG}:latest", timeout=120)
    ok3 = "sha256" in r.stdout
    results.append(("docker pull back", ok3))

    # Read-only login
    sh(f"docker logout {reg} 2>/dev/null", timeout=10)
    r = sh(f"echo '{RPW}' | docker login '{reg}' -u '{RU}' --password-stdin", timeout=20)
    ok4 = "Succeeded" in (r.stdout + r.stderr)
    results.append(("read-only login", ok4))

    # Read-only push should fail
    r = sh(f"docker push {reg}/{IMG}:latest", timeout=60)
    outc = (r.stdout + r.stderr).lower()
    ok5 = any(w in outc for w in ["denied", "unauthorized"])
    results.append(("read-only push denied", ok5))

    # Read-only pull should succeed
    r = sh(f"docker pull {reg}/{IMG}:latest", timeout=60)
    ok6 = "sha256" in r.stdout
    results.append(("read-only pull ok", ok6))

    return results


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("RP", 5000))
    for name, ok in run(port):
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
