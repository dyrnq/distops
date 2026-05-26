#!/usr/bin/env python3
"""
distops Integration Test Suite - Entry Point
  Mode 1: Direct (client -> registry:5000, no nginx)
  Mode 2: Proxy  (client -> nginx:34000 -> registry:5000)

Usage:
  python3 scripts/tests/run.py              # Mode 1 only
  python3 scripts/tests/run.py --nginx      # Mode 2 only (uses 127.0.0.1)
  python3 scripts/tests/run.py --all        # Both modes
  python3 scripts/tests/run.py --list       # List available test modules
"""

import argparse
import importlib.util
import os
import sys
import time
from pathlib import Path

SCRIPTS_DIR = Path(__file__).parent
HOST = os.environ.get("HOST", "localhost")
RP = int(os.environ.get("RP", 5000))
PP = int(os.environ.get("PP", 34000))

PASS = 0
skip = set()
FAIL = 0
TESTS_RUN = []


def log(msg):
    print(f"  {msg}")


def header(title):
    print(f"\n{'=' * 60}")
    print(f" {title}")
    print(f"{'=' * 60}")


def load_module(name):
    path = SCRIPTS_DIR / f"{name}.py"
    if not path.exists():
        print(f"  [FAIL] Module not found: {path}")
        return None
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def run_module(name, label, port=None, **kwargs):
    global PASS, FAIL
    if name in skip:
        print(f"\n--- {label} ---")
        print(f"  [SKIP] {label} (in SKIP list)")
        return
    mod = load_module(name)
    if mod is None:
        FAIL += 1
        return

    print(f"\n--- {label} ---")
    try:
        if port is not None:
            ret = mod.run(port, **kwargs); results = ret if isinstance(ret, list) else ret[0]
        else:
            results = mod.run(**kwargs)
    except Exception as e:
        print(f"  [ERROR] {e}")
        results = [("CRASH", False)]

    for test_name, ok in results:
        status = "PASS" if ok else "FAIL"
        print(f"  [{status}] {test_name}")
        if ok:
            PASS += 1
        else:
            FAIL += 1
    TESTS_RUN.append(label)


def list_modules():
    print("Available test modules:")
    for f in sorted(SCRIPTS_DIR.glob("test_*.py")):
        name = f.stem.replace("test_", "").replace("_", " ")
        print(f"  {f.stem:30s} - {name}")


def main():
    global PASS, FAIL
    parser = argparse.ArgumentParser(description="distops Integration Test Suite")
    parser.add_argument("--nginx", action="store_true", help="Test via nginx proxy")
    parser.add_argument("--all", action="store_true", help="Test both modes")
    parser.add_argument("--list", action="store_true", help="List test modules")
    args = parser.parse_args()

    if args.list:
        list_modules()
        return

    global skip; skip = set(os.environ.get("SKIP", "").split(",")) if os.environ.get("SKIP") else set()

    mode1 = not args.nginx or args.all
    mode2 = args.nginx or args.all

    # Mode 1: Direct
    if mode1:
        # Clean start for Mode 1
        load_module("reset").run()

        # Pre-flight
        header("Pre-flight (Mode 1)")
        load_module("check_daemon").run()
        header("MODE 1: Direct (client -> {HOST}:{RP})")
        run_module("test_api", "Registry API", port=RP)
        run_module("test_token", "Token Auth", port=RP)
        run_module("test_docker", "Docker Operations", port=RP)
        run_module("test_regctl", "regctl", port=RP)
        run_module("test_skopeo", "skopeo", port=RP)
        run_module("test_jwt", "JWT CLI")
        run_module("test_admin", "Admin API", port=RP)
        run_module("test_oauth", "OAuth2 POST Endpoint", port=RP)
        run_module("test_oauth_errors", "OAuth2 Error Handling")
        run_module("test_containerd", "containerd ctr Pull via OAuth2")
        run_module("test_crictl", "crictl Pull")
        run_module("test_proxy", "Proxy Registry")
        run_module("test_proxy_push_denied", "Proxy Registry Push Denied")

    # Mode 2: Proxy - use 127.0.0.1 since nginx is on --network host
    if mode2:
        # Clean start for Mode 2
        load_module("reset").run()

        # Pre-flight
        header("Pre-flight (Mode 2)")
        load_module("check_daemon").run()
        header("MODE 2: Proxy (client -> nginx:34000 -> registry:5000)")
        load_module("setup_nginx").run()
        # Override HOST to 127.0.0.1 for nginx mode to avoid external IP routing issues
        os.environ["HOST"] = "127.0.0.1"
        PP_LOCAL = 34000
        run_module("test_api", "Registry API via Proxy", port=PP_LOCAL)
        run_module("test_token", "Token Auth via Proxy", port=PP_LOCAL)
        run_module("test_docker", "Docker Push via Proxy", port=PP_LOCAL)
        run_module("test_regctl", "regctl via Proxy", port=PP_LOCAL)
        run_module("test_skopeo", "skopeo via Proxy", port=PP_LOCAL)
        run_module("test_oauth", "OAuth2 POST Endpoint via Proxy")
        run_module("test_oauth_errors", "OAuth2 Error Handling via Proxy")
        run_module("test_containerd", "containerd ctr Pull via OAuth2", port=PP_LOCAL)
        run_module("test_crictl", "crictl Pull", port=PP_LOCAL)
        run_module("test_proxy", "Proxy Registry via Nginx", proxy_ports=[35000,35001,35002,35003,35004,35005])
        run_module("test_proxy_push_denied", "Proxy Push Denied via Nginx", proxy_ports=[35000,35001,35002,35003,35004,35005])

    # Summary
    header("SUMMARY")
    print(f"  PASS: {PASS}  FAIL: {FAIL}")
    if FAIL == 0:
        print("  All tests passed!")
    else:
        print(f"  {FAIL} test(s) failed")
        # Print container logs on failure for debugging
        import subprocess
        print("")
        print("=== distops container logs (last 60 lines) ===")
        subprocess.run(
            f"docker logs {os.environ.get('DISTOPS_CONTAINER', 'distops-test')} --tail 60 2>&1 || true",
            shell=True, timeout=10,
        )
        print("=== docker ps ===")
        subprocess.run("docker ps -a --format 'table {{.Names}}\t{{.Status}}' 2>&1 || true", shell=True, timeout=10)
    print(f"  {time.strftime('%Y-%m-%d %H:%M:%S')}")

    sys.exit(1 if FAIL > 0 else 0)


if __name__ == "__main__":
    main()
