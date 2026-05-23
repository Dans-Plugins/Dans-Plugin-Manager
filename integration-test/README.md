# Integration Tests

End-to-end tests that deploy DPM to a live Spigot server (via [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure)) and assert that key commands produce the expected output.

## Why this exists

Unit tests can verify parsing logic and service behaviour in isolation, but they cannot catch:

- **Real GitHub API failures** — rate limits, renamed repos, changed release JSON shape
- **Real file-system behaviour** — JAR placement, duplicate detection, version-tag storage
- **Real Bukkit command routing** — whether the command is registered, dispatched, and reaches the right handler on a live server
- **Plugin load failures** — a JAR that passes unit tests can still fail to enable on a real Spigot version due to API incompatibilities

These tests exercise the full stack: Maven build → JAR deploy → Spigot reload → console command → log assertion.

## What the tests cover

| Step | Command | Assertion |
|------|---------|-----------|
| 4 | `dpm list` | `=== Plugins` — confirms plugin loaded and command routes correctly |
| 5 | `dpm get currencies` | `Also downloading required dependency` + `Downloaded` — confirms hard-dependency auto-install triggers when dep is missing |
| 6 | `dpm remove medievalfactions --confirm` | `Removed medievalfactions` — confirms JAR is deleted from plugins folder |
| 7 | `dpm list installed` | `=== Installed Plugins` — confirms installed-only filter |
| 8 | `dpm list available` | `=== Available Plugins` — confirms available-only filter |
| 9 | `dpm get medievalfactions` | `Downloaded` or `already up to date` — confirms real GitHub API call and file write |
| 10 | `dpm search faction` | `=== Search Results` — confirms registry search |
| 11 | `dpm get nonexistentplugin` | `Plugin not found: nonexistentplugin` — confirms error path doesn't crash the server |
| 12 | `dpm stats` | `Available plugins:` — confirms available count line renders after 0.6.0 stat addition |

## What is not yet covered

| Area | Notes |
|------|-------|
| `dpm update` / `dpm update <name>` | Exercises the same download path as `dpm get` but with version comparison; low incremental value |
| `dpm clean [--confirm]` | Requires duplicate JARs to be present; hard to set up reliably in CI |
| `dpm info <name>` | Mainly a display command; low regression risk |
| `dpm reload` | Would confirm `githubToken` config reloads without server restart |
| `dpm get <multiple names>` | Batch mode not tested; same download code path as single |
| Permission enforcement | Console has all permissions; player-level permission checks cannot be exercised without a player login |
| Network failure / download error paths | Hard to simulate reliably in CI |
| Tab-completion | Cannot be tested via console API |
| Config `githubToken` | Authenticated GitHub API not tested; unauthenticated rate limit applies in CI |

## Prerequisites

- Docker with Compose v2 (`docker compose`), Maven, `curl`, Git — `./test-integration.sh` in the repo root verifies these
- Python 3.9+ and `pip install requests`

## Running locally

The repo root's `./up.sh` handles cloning OMCSI, configuring its `.env`, building DPM, starting the stack, and deploying the JAR. See [CONTRIBUTING.md](../CONTRIBUTING.md#local-test-server-via-omcsi) for the full description.

**1. Start the test server** (first run compiles Spigot from BuildTools — allow 10–15 min):

```bash
cp sample.env .env    # one-time; edit values if you like
./up.sh
```

**2. Run the test harness** against the running server:

```bash
# Source .env to pick up DEPLOY_AUTH_TOKEN and OMCSI_API_BASE.
set -a; source .env; set +a
export OMCSI_DEPLOY_TOKEN="$DEPLOY_AUTH_TOKEN"
export DPM_JAR_PATH=$(ls target/DansPluginManager-*.jar | grep -v original | head -1)
python integration-test/test_dpm.py
```

**3. Tear down** when done:

```bash
./down.sh
```

## Required GitHub Actions secrets

| Secret | Purpose |
|--------|---------|
| `OMCSI_GITHUB_TOKEN` | PAT with read access to the OMCSI repository |
| `OMCSI_DEPLOY_TOKEN` | Bearer token for the OMCSI REST API (`DEPLOY_AUTH_TOKEN` in `.env`) |
| `OMCSI_RCON_PASSWORD` | RCON password (`RCON_PASSWORD` in `.env`) |

## OMCSI REST API reference

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| `GET` | `/api/server/status` | none | — | Returns `{"running": true/false}` |
| `POST` | `/api/plugins/deploy` | Bearer | multipart: `pluginName` (filename) + `file` (JAR) | Writes JAR to plugins directory |
| `POST` | `/api/server/command` | none | `text/plain` command string | Sends raw console command via FIFO |
| `GET` | `/api/server/logs?lines=N` | none | — | Returns last N lines as JSON; requires `LOGS_DIAGNOSTIC_ENABLED=true` in container env |
