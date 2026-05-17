#!/usr/bin/env python3
"""Integration test harness for Dans Plugin Manager.

Deploys the DPM JAR to a live OMCSI Spigot server via REST API and
asserts that key commands produce the expected console output.

Required environment variables:
  OMCSI_DEPLOY_TOKEN    Bearer token accepted by the OMCSI REST API
  DPM_JAR_PATH          Path to the built DansPluginManager-*.jar

Optional:
  OMCSI_API_BASE        Base URL of the OMCSI REST API (default: http://localhost:8092)
  OMCSI_CONTAINER_NAME  Docker container name for log reading (default: open-mc-server)
"""

import os
import subprocess
import sys
import time

import requests

API_BASE = os.getenv("OMCSI_API_BASE", "http://localhost:8092")
TOKEN = os.environ["OMCSI_DEPLOY_TOKEN"]
JAR_PATH = os.environ["DPM_JAR_PATH"]
CONTAINER = os.getenv("OMCSI_CONTAINER_NAME", "open-mc-server")

_HEADERS = {"Authorization": f"Bearer {TOKEN}"}


def _api(method, path, **kwargs):
    resp = requests.request(
        method, f"{API_BASE}{path}", headers=_HEADERS, timeout=30, **kwargs
    )
    resp.raise_for_status()
    return resp


def wait_until_running(timeout=300, poll_interval=10, label="server"):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if _api("GET", "/api/server/status").json().get("running"):
                print(f"  {label} ready.")
                return
        except Exception as exc:
            print(f"  status check error: {exc}")
        remaining = int(deadline - time.time())
        print(f"  waiting for {label} ({remaining}s remaining)...")
        time.sleep(poll_interval)
    sys.exit(f"Timed out waiting for {label}")


def send_command(cmd):
    _api("POST", "/api/server/command", json={"command": cmd})
    print(f"  > {cmd}")


def _docker_logs(tail=200):
    """Read recent container log lines via docker logs."""
    result = subprocess.run(
        ["docker", "logs", "--tail", str(tail), CONTAINER],
        capture_output=True, text=True, timeout=15,
    )
    # Minecraft server output may appear on either stream depending on the wrapper
    return result.stdout + result.stderr


def assert_log_contains(expected, tail=200, retries=6, delay=5):
    for attempt in range(1, retries + 1):
        if expected in _docker_logs(tail):
            print(f"  PASS: found {expected!r}")
            return
        if attempt < retries:
            print(f"  attempt {attempt}: {expected!r} not in logs yet, retrying in {delay}s...")
            time.sleep(delay)
    print("--- last container logs ---")
    print(_docker_logs(tail))
    sys.exit(f"FAIL: {expected!r} not found after {retries} attempts")


def assert_log_contains_any(candidates, tail=200, retries=6, delay=5):
    for attempt in range(1, retries + 1):
        log = _docker_logs(tail)
        for expected in candidates:
            if expected in log:
                print(f"  PASS: found {expected!r}")
                return
        if attempt < retries:
            print(f"  attempt {attempt}: none of {candidates!r} in logs yet, retrying in {delay}s...")
            time.sleep(delay)
    print("--- last container logs ---")
    print(_docker_logs(tail))
    sys.exit(f"FAIL: none of {candidates!r} found after {retries} attempts")


def deploy_jar(path):
    with open(path, "rb") as jar:
        _api(
            "POST",
            "/api/plugins/deploy",
            data={"pluginName": "DansPluginManager.jar"},
            files={"file": ("DansPluginManager.jar", jar, "application/java-archive")},
        )
    print(f"  deployed {path}")


def main():
    print("=== DPM Integration Tests ===\n")

    print("[1] Waiting for Spigot server to start...")
    wait_until_running(timeout=300)

    print("\n[2] Deploying DPM JAR...")
    deploy_jar(JAR_PATH)

    print("\n[3] Reloading server to activate plugin...")
    send_command("reload confirm")
    time.sleep(15)
    wait_until_running(timeout=120, label="server after reload")

    print("\n[4] /dpm list — expect '=== Plugins'...")
    send_command("dpm list")
    assert_log_contains("=== Plugins")

    print("\n[5] /dpm get medievalfactions — expect download confirmation...")
    send_command("dpm get medievalfactions")
    # "Downloaded" on first run; "already up to date" on subsequent runs
    assert_log_contains_any(["Downloaded", "already up to date"], retries=8, delay=5)

    print("\n[6] /dpm search faction — expect '=== Search Results'...")
    send_command("dpm search faction")
    assert_log_contains("=== Search Results")

    print("\n=== All integration tests passed ===")


if __name__ == "__main__":
    main()
