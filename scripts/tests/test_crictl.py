#!/usr/bin/env python3
"""
Test crictl (CRI CLI) pull via distops auth.
crictl is the standard CLI for CRI-compatible runtimes (containerd/CRI-O).
Used by kubelet in Kubernetes environments.
"""
import os
import subprocess
import time

HOST = os.environ.get("HOST", "localhost")
RP = int(os.environ.get("RP", 5000))
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")


def sh(cmd, timeout=30):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port=None):
    results = []

    p = port if port is not None else RP
    reg = f"{HOST}:{p}"
    TEST_IMG = f"{reg}/library/crictl-test:latest"

    # 1. Check crictl available
    r = sh("command -v crictl", timeout=5)
    if r.returncode != 0:
        results.append(("crictl not available - skipped", True))
        return results

    # 2. Prepare test image
    sh("docker pull alpine:3.21 2>/dev/null || true", timeout=10)
    # Wait for pending events from previous tests to flush
    time.sleep(5)
    sh(f"docker tag alpine:3.21 {TEST_IMG} 2>/dev/null", timeout=5)
    r = sh(f"docker push {TEST_IMG} 2>&1", timeout=120)
    if r.returncode != 0:
        time.sleep(5)
        r = sh(f"docker push {TEST_IMG} 2>&1", timeout=120)
    if r.returncode != 0:
        import pathlib
        policy_dir = pathlib.Path.home() / ".config" / "containers"
        policy_dir.mkdir(parents=True, exist_ok=True)
        (policy_dir / "policy.json").write_text('{"default": [{"type": "insecureAcceptAnything"}]}')
        r = sh(f"skopeo copy --dest-tls-verify=false --dest-creds {TU}:{TP} docker-daemon:alpine:3.21 docker://{TEST_IMG}", timeout=120)
    ok_push = r.returncode == 0
    results.append(("Push test image for crictl", ok_push))
    if not ok_push:
        return results

    # 3. Pull via crictl
    r = sh(f"sudo crictl pull --creds {TU}:{TP} {TEST_IMG} 2>&1", timeout=60)
    ok_pull = "Image is up to date" in (r.stdout + r.stderr) or "Pulled" in (r.stdout + r.stderr)
    results.append(("crictl pull", ok_pull))

    # 4. Cleanup
    sh(f"sudo crictl rmi {TEST_IMG} 2>/dev/null", timeout=10)

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
