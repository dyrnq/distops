#!/usr/bin/env python3
"""Test regctl manifest inspection with token auth via registry config."""
import json
import os
import re
import subprocess
from pathlib import Path

HOST = os.environ.get("HOST", "localhost")
TU = os.environ.get("TU", "test")
TP = os.environ.get("TP", "test")
IMG = os.environ.get("IMG", "itest")


def sh(cmd, timeout=30):
    return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)


def run(port):
    results = []
    reg = f"{HOST}:{port}"

    # Configure regctl via config.json (token auth via user/pass)
    cfg = Path.home() / ".regctl" / "config.json"
    cfg.parent.mkdir(parents=True, exist_ok=True)
    cfg.write_text(json.dumps({"hosts": {reg: {"user": TU, "pass": TP, "tls": "disabled", "regcert": ""}}}))

    r = sh(
        f"regctl manifest head {reg}/{IMG}:latest",
        timeout=30,
    )
    ok = r.returncode == 0 and len(r.stdout.strip()) > 0
    results.append(("regctl manifest head", ok))
    return results


if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("RP", 5000))
    for name, ok in run(port):
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
