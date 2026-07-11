#!/bin/bash
# Rebuild DPM and hot-redeploy it to the running OMCSI test server.
# Requires ./up.sh to be running.

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "ERROR: .env not found. Run: cp sample.env .env (then ./up.sh)" >&2
    exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

OMCSI_API_BASE="${OMCSI_API_BASE:-http://localhost:8092}"

echo "==> Building DPM ..."
mvn clean package -DskipTests

JAR=$(find target -maxdepth 1 -name "DansPluginManager-*.jar" -not -name "original-*" -type f -print -quit)
if [ -z "$JAR" ]; then
    echo "ERROR: No plugin JAR found in target/" >&2
    exit 1
fi
echo "    Built: $JAR"

echo "==> Verifying OMCSI is up at $OMCSI_API_BASE ..."
if ! curl -fsS "$OMCSI_API_BASE/api/server/status" >/dev/null 2>&1; then
    echo "ERROR: OMCSI wrapper API not reachable. Did you run ./up.sh?" >&2
    exit 1
fi

echo "==> Deploying $JAR ..."
curl -fsS -X POST "$OMCSI_API_BASE/api/plugins/deploy" \
    -H "Authorization: Bearer ${DEPLOY_AUTH_TOKEN}" \
    -F "pluginName=DansPluginManager.jar" \
    -F "file=@${JAR};type=application/java-archive" >/dev/null

echo "==> Reloading server to pick up new JAR ..."
curl -fsS -X POST "$OMCSI_API_BASE/api/server/command" \
    -H "Authorization: Bearer ${DEPLOY_AUTH_TOKEN}" \
    -H "Content-Type: text/plain; charset=utf-8" \
    --data "reload confirm" >/dev/null

echo "Done. Try:  ./dpm-cmd.sh \"dpm list\""
