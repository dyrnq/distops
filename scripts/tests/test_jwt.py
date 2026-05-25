#!/usr/bin/env python3
"""Test JWT CLI tool inside the distops container."""
import subprocess


def run():
    results = []

    r = subprocess.run(
        "docker exec $(docker ps --format '{{.Names}}' | grep distops | head -1) "
        "java -jar /distops.jar cli jwt",
        shell=True, timeout=30, capture_output=True, text=True,
    )
    ok = len(r.stdout.strip()) > 10
    results.append(("JWT CLI", ok))

    return results


if __name__ == "__main__":
    for name, ok in run():
        print(f"  [{'PASS' if ok else 'FAIL'}] {name}")
