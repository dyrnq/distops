#!/usr/bin/env python3
"""Reset distops environment: remove old containers, clean data, rebuild."""
import os
import subprocess

DISTOPS_CONTAINER = os.environ.get("DISTOPS_CONTAINER", "distops-test")
IMAGE = os.environ.get("IMAGE", "dyrnq/distops:latest")


def run(port=None):
    def sh(cmd, timeout=30): return subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True, text=True)

    print("  [RESET] Removing containers...")
    sh(f"docker rm -f nginx 2>/dev/null || true")
    sh(f"docker rm -f {DISTOPS_CONTAINER} 2>/dev/null || true")

    print("  [RESET] Cleaning data...")
    sh("sudo rm -rf /data/distops")

    print(f"  [RESET] Starting {IMAGE}...")
    sh(
        f"docker run -d --name {DISTOPS_CONTAINER} --restart always "
        f"--network host "
        f"-v /data/distops:/data/distops "
        f"-e TZ=Asia/Shanghai "
        f'-e JAVA_OPTS="-server -Xms1g -Xmx1g -Djava.awt.headless=true '
        f'-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai '
        f'-Djava.net.preferIPv4Stack=true -Dspring.flyway.enabled=true" '
        f"-e OTEL_TRACES_EXPORTER=none "
        f"{IMAGE}",
        timeout=30,
    )

    import time
    time.sleep(8)  # give Flyway + supervisor time to initialize
    for _ in range(30):
        time.sleep(2)
        r = sh("curl -s -o /dev/null -w '%{http_code}' http://localhost:12680/", timeout=5)
        if r.stdout.strip() in ("200", "302", "404"):
            print("  [RESET] distops ready")
            break
    else:
        print("  [RESET] distops startup timeout")

    return []
