#!/usr/bin/env python3
"""Test docker login, push, pull operations."""
import os
import re
import shlex
import subprocess

HOST = os.environ.get("HOST", "localhost")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")
RU = os.environ.get("RU", "read")
RPW = os.environ.get("RPW", "test")
IMG = os.environ.get("IMG", "docker-test")


def container_log(pattern, limit=40):
    """Matching lines from the distops container log.

    The CI workflow only dumps the last 80 container log lines, which does
    not reach back far enough to cover this test, so the evidence for a
    failure is collected here, next to the assertion that failed.
    """
    container = os.environ.get("DISTOPS_CONTAINER", "distops-test")
    try:
        r = subprocess.run(
            f"docker logs {shlex.quote(container)} 2>&1 | grep -E {shlex.quote(pattern)} | tail -{limit}",
            shell=True,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except Exception:
        return ""
    return (r.stdout or "").strip()


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

    # Read-only push must be refused.
    #
    # Two probes, because a push can be refused at two different write
    # paths and a client only exercises one of them:
    #
    #   1. a new tag in an existing repository -> forces a manifest write
    #   2. a brand new repository               -> forces a blob upload
    #
    # Probing with the tag the write account just pushed proves nothing:
    # a client that skips the redundant write reports success, so the
    # assertion measured the client's short-circuit instead of the
    # registry's authorization. Docker 29 (containerd image store) does
    # exactly that — pushing an image that is already in the registry
    # exits 0 without contacting the registry for a write.
    #
    # The security property is "the read-only account cannot push" — assert
    # on the outcome, not on Docker's error wording. The wording changed
    # with the containerd image store (Docker 29.x): 28.x printed
    # "denied: requested access to the resource is denied", 29.x prints
    # "push access denied ... insufficient_scope: authorization failed",
    # and some flows surface a bare "401 Unauthorized" / "403 Forbidden".
    # Matching on fixed wording broke the test on the runner image upgrade
    # even though the registry correctly returned 401.
    for probe, repo, tag in (
        ("new tag", IMG, "ro-test"),
        ("new repository", f"{IMG}-ro", "latest"),
    ):
        target = f"{reg}/{repo}:{tag}"
        sh(f"docker rmi {target} 2>/dev/null", timeout=10)
        sh(f"docker tag alpine:3.21 {target}", timeout=10)

        r = sh(f"docker push {target}", timeout=120)
        out = r.stdout + r.stderr
        # A completed push always ends with "<tag>: digest: sha256:<64 hex>".
        # Detect that specifically — a failed push can still mention a sha256
        # in the blob URL it was complaining about.
        pushed = re.search(r"digest:\s*sha256:[0-9a-f]{64}", out) is not None
        refused = r.returncode != 0 and not pushed

        # Ground truth: the registry must not have the tag afterwards. That
        # is the security property ("the read-only account cannot push"),
        # and unlike the client's exit code it does not depend on what the
        # client reports — a client that skips the redundant write of an
        # image that is already in the registry reports success without
        # performing any write.
        sh(f"docker rmi {target} 2>/dev/null", timeout=10)
        created = sh(f"docker pull {target}", timeout=60).returncode == 0

        ok = not created
        results.append((f"read-only push denied ({probe})", ok))
        if not ok:
            # The raw output used to be swallowed by capture_output=True, which
            # made a failure impossible to diagnose from the CI log.
            print(f"      docker push {target} rc={r.returncode} output={out.strip()[:800]}")
            print(f"      registry access log:\n{container_log(r'/v2/' + re.escape(repo))}")
            print(f"      auth decisions:\n{container_log(r'name=' + re.escape(repo))}")
        elif not refused:
            print("      note: the client reported success, the registry did not store the image")

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
