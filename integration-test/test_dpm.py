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

import datetime
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
    """Send a console command and return a timestamp cursor for scoped log assertions."""
    # Subtract 1 s so logs emitted in the same second as the command are included.
    cursor = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(seconds=1)
    requests.post(
        f"{API_BASE}/api/server/command",
        headers={**_HEADERS, "Content-Type": "text/plain; charset=utf-8"},
        data=cmd.encode("utf-8"),
        timeout=30,
    ).raise_for_status()
    print(f"  > {cmd}")
    return cursor


def _docker_logs(cursor=None, tail=500):
    """Return container log output, scoped to lines after *cursor* when provided."""
    cmd = ["docker", "logs"]
    if cursor is not None:
        cmd += ["--since", cursor.isoformat()]
    else:
        cmd += ["--tail", str(tail)]
    cmd.append(CONTAINER)
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
    # Minecraft server output may appear on either stream depending on the wrapper
    return result.stdout + result.stderr


def assert_log_contains(expected, cursor=None, tail=500, retries=6, delay=5):
    for attempt in range(1, retries + 1):
        if expected in _docker_logs(cursor=cursor, tail=tail):
            print(f"  PASS: found {expected!r}")
            return
        if attempt < retries:
            print(f"  attempt {attempt}: {expected!r} not in logs yet, retrying in {delay}s...")
            time.sleep(delay)
    print("--- last container logs ---")
    print(_docker_logs(cursor=cursor, tail=tail))
    sys.exit(f"FAIL: {expected!r} not found after {retries} attempts")


def assert_log_contains_any(candidates, cursor=None, tail=500, retries=6, delay=5):
    for attempt in range(1, retries + 1):
        log = _docker_logs(cursor=cursor, tail=tail)
        for expected in candidates:
            if expected in log:
                print(f"  PASS: found {expected!r}")
                return
        if attempt < retries:
            print(f"  attempt {attempt}: none of {candidates!r} in logs yet, retrying in {delay}s...")
            time.sleep(delay)
    print("--- last container logs ---")
    print(_docker_logs(cursor=cursor, tail=tail))
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

    print("\n[4] /dpm list — confirm plugin loaded and command routes correctly...")
    cursor = send_command("dpm list")
    assert_log_contains("=== Plugins", cursor=cursor)

    # Test dependency auto-install: currencies hard-depends on medievalfactions, which is
    # not yet installed. DPM should detect this and download medievalfactions automatically.
    print("\n[5] /dpm get currencies — confirm hard-dependency auto-install...")
    cursor = send_command("dpm get currencies")
    assert_log_contains("Also downloading required dependency", cursor=cursor, retries=3, delay=3)
    assert_log_contains_any(["Downloaded", "already up to date"], cursor=cursor, retries=8, delay=5)

    print("\n[6] /dpm remove medievalfactions --confirm — confirm file deletion...")
    cursor = send_command("dpm remove medievalfactions --confirm")
    assert_log_contains("Removed medievalfactions", cursor=cursor)

    print("\n[7] /dpm list installed — confirm filter shows only installed plugins...")
    cursor = send_command("dpm list installed")
    assert_log_contains("=== Installed Plugins", cursor=cursor)

    print("\n[8] /dpm list available — confirm filter shows only uninstalled plugins...")
    cursor = send_command("dpm list available")
    assert_log_contains("=== Available Plugins", cursor=cursor)

    print("\n[9] /dpm get medievalfactions — confirm standard download...")
    cursor = send_command("dpm get medievalfactions")
    assert_log_contains_any(["Downloaded", "already up to date"], cursor=cursor, retries=8, delay=5)

    print("\n[10] /dpm search faction — confirm registry search...")
    cursor = send_command("dpm search faction")
    assert_log_contains("=== Search Results", cursor=cursor)

    print("\n[11] /dpm get nonexistentplugin — confirm error path...")
    cursor = send_command("dpm get nonexistentplugin")
    assert_log_contains("Plugin not found: nonexistentplugin", cursor=cursor)

    print("\n=== All integration tests passed ===")


if __name__ == "__main__":
    main()
