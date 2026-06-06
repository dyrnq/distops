#!/usr/bin/env python3
"""Test skopeo copy (push) as alternative to docker push."""
import os
import subprocess
import pathlib

HOST = os.environ.get("HOST", "localhost")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def sh(cmd, timeout=60):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port=None):
    results = []
    RP = int(os.environ.get("RP", 5000))
    p = port if port is not None else RP
    reg = f"{HOST}:{p}"
    IMG = "skopeo-copy-test"

    # Ensure policy.json exists
    policy_dir = pathlib.Path.home() / ".config" / "containers"
    policy_dir.mkdir(parents=True, exist_ok=True)
    (policy_dir / "policy.json").write_text('{"default": [{"type": "insecureAcceptAnything"}]}')

    # Copy alpine to registry
    r = sh(f"skopeo copy --dest-tls-verify=false --dest-creds {TU}:{TP} docker-daemon:alpine:3.21 docker://{reg}/{IMG}:latest", timeout=120)
    results.append(("skopeo copy push", r.returncode == 0))

    # Verify with regctl
    if r.returncode == 0:
        r2 = sh(f"regctl registry set {reg} --tls disabled && regctl manifest head {reg}/{IMG}:latest", timeout=30)
        results.append(("skopeo copy verify (regctl)", r2.returncode == 0))
    else:
        results.append(("skopeo copy verify (regctl)", False))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
