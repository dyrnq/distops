#!/usr/bin/env python3
"""Check /etc/docker/daemon.json and add insecure-registries entries if needed."""
import json
import os
import subprocess
import time
from pathlib import Path

HOST = os.environ.get("HOST", "localhost")
RP = int(os.environ.get("RP", 5000))
PP = int(os.environ.get("PP", 34000))


def run():
    daemon_path = Path("/etc/docker/daemon.json")
    if not daemon_path.exists():
        print("[INFO] daemon.json not found, skipping")
        return

    content = daemon_path.read_text()
    config = json.loads(content)

    regs = config.get("insecure-registries", [])
    needed = [f"{HOST}:{RP}", f"{HOST}:{PP}"]
    changed = False

    for entry in needed:
        if entry not in regs:
            regs.append(entry)
            changed = True

    if changed:
        config["insecure-registries"] = regs
        new_content = json.dumps(config, indent=4) + "\n"
        try:
            # Try writing via sudo
            import tempfile
            tmp = tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json")
            tmp.write(new_content)
            tmp.close()
            subprocess.run(
                f"sudo cp {tmp.name} {daemon_path} && sudo rm -f {tmp.name}",
                shell=True, timeout=15, capture_output=True, text=True,
            )
            print(f"[INFO] Added {needed} to insecure-registries, reloading docker...")
            subprocess.run(
                "sudo systemctl reload docker",
                shell=True, timeout=30, capture_output=True, text=True,
            )
            time.sleep(3)
            print("[INFO] Docker reloaded")
        except Exception as e:
            print(f"[WARN] Could not update daemon.json: {e}")
    else:
        print("[INFO] insecure-registries already configured")


if __name__ == "__main__":
    run()
