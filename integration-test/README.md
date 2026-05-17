# Integration Tests

End-to-end tests that deploy DPM to a live Spigot server (via [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure)) and assert that key commands produce the expected output.

## What the tests cover

| Step | Command | Assertion |
|------|---------|-----------|
| 4 | `dpm list` | `=== Plugins` in logs |
| 5 | `dpm get medievalfactions` | `Downloaded` or `already up to date` in logs |
| 6 | `dpm search faction` | `=== Search Results` in logs |

## Prerequisites

- Docker with Compose v2 (`docker compose`)
- Access to the [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure) repository
- Python 3.9+ and `pip install requests`
- A built DPM JAR (`mvn package -DskipTests`)

## Running locally

**1. Clone OMCSI** next to this repo (or anywhere):

```bash
git clone https://github.com/dmccoystephenson/open-mc-server-infrastructure ../omcsi
```

**2. Create `../omcsi/.env`** with the following values:

```
DEPLOY_AUTH_TOKEN=<your-token>
RCON_PASSWORD=<any-password>
ONLINE_MODE=false
DISCORD_WEBHOOK_URL=
ALERT_EMAIL=
```

**3. Start the stack** (first run compiles Spigot from source — allow 10–15 min):

```bash
cd ../omcsi
docker compose up -d --build
```

**4. Build DPM** from the repo root:

```bash
mvn package -DskipTests
```

**5. Run the test harness**:

```bash
export OMCSI_API_BASE=http://localhost:8092
export OMCSI_DEPLOY_TOKEN=<your-token>
export DPM_JAR_PATH=$(ls target/DansPluginManager-*.jar | grep -v original | head -1)
python integration-test/test_dpm.py
```

**6. Tear down** when done:

```bash
docker compose -f ../omcsi/docker-compose.yml down
```

## Required GitHub Actions secrets

| Secret | Purpose |
|--------|---------|
| `OMCSI_GITHUB_TOKEN` | PAT with read access to the OMCSI repository |
| `OMCSI_DEPLOY_TOKEN` | Bearer token for the OMCSI REST API (`DEPLOY_AUTH_TOKEN` in `.env`) |
| `OMCSI_RCON_PASSWORD` | RCON password (`RCON_PASSWORD` in `.env`) |

## OMCSI REST API reference

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/server/status` | Returns `{"running": true/false}` |
| `POST` | `/api/plugins/deploy` | Multipart upload of JAR; Bearer auth required |
| `POST` | `/api/server/command` | JSON `{"command": "..."}` sent to console; Bearer auth required |
| `GET` | `/api/server/logs?lines=N` | Returns last N lines of `logs/latest.log` |
