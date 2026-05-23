#!/bin/bash
# Send a command to the running OMCSI test server and print recent log output.
# Lets you exercise /dpm commands without joining the server in a Minecraft client.
#
# Usage:
#   ./dpm-cmd.sh "dpm list"
#   ./dpm-cmd.sh "dpm get medievalfactions"

set -euo pipefail
cd "$(dirname "$0")"

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 \"<command>\"" >&2
    echo "Example: $0 \"dpm list\"" >&2
    exit 2
fi

if [ ! -f .env ]; then
    echo "ERROR: .env not found. Run: cp sample.env .env (then ./up.sh)" >&2
    exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

OMCSI_API_BASE="${OMCSI_API_BASE:-http://localhost:8092}"
OMCSI_PATH="${OMCSI_PATH:-../omcsi}"
CMD="$1"
TAIL="${LOG_TAIL:-60}"

if ! curl -fsS "$OMCSI_API_BASE/api/server/status" >/dev/null 2>&1; then
    echo "ERROR: OMCSI wrapper API not reachable at $OMCSI_API_BASE. Did you run ./up.sh?" >&2
    exit 1
fi

echo "> $CMD"
curl -fsS -X POST "$OMCSI_API_BASE/api/server/command" \
    -H "Authorization: Bearer ${DEPLOY_AUTH_TOKEN}" \
    -H "Content-Type: text/plain; charset=utf-8" \
    --data "$CMD" >/dev/null

# Give the server a moment to emit any response lines.
sleep 1

echo "--- last $TAIL log lines ---"
if ! curl -fsS "$OMCSI_API_BASE/api/server/logs?lines=${TAIL}"; then
    echo "(GET /api/server/logs failed. up.sh seeds LOGS_DIAGNOSTIC_ENABLED=true on first run;" >&2
    echo " if you regenerated $OMCSI_PATH/.env from sample.env, set it back to true and restart.)" >&2
    exit 1
fi
