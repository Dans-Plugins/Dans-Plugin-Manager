#!/bin/bash
# Start a local Spigot test server (via OMCSI) with DPM deployed.
#
# - Clones OMCSI to OMCSI_PATH (default ../omcsi) if missing
# - Seeds OMCSI's .env from this repo's .env if needed
# - Builds DPM (mvn clean package -DskipTests)
# - Starts the OMCSI stack and deploys the freshly-built JAR

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "ERROR: .env not found. Run: cp sample.env .env (then edit as needed)" >&2
    exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

OMCSI_PATH="${OMCSI_PATH:-../omcsi}"
OMCSI_API_BASE="${OMCSI_API_BASE:-http://localhost:8092}"
OMCSI_REPO="${OMCSI_REPO:-https://github.com/dmccoystephenson/open-mc-server-infrastructure.git}"

# 1) Clone OMCSI if it's not there yet
if [ ! -d "$OMCSI_PATH/.git" ]; then
    echo "==> Cloning OMCSI to $OMCSI_PATH ..."
    git clone "$OMCSI_REPO" "$OMCSI_PATH"
else
    echo "==> OMCSI checkout already exists at $OMCSI_PATH"
fi

# 2) Seed OMCSI's .env from this repo's .env if missing
if [ ! -f "$OMCSI_PATH/.env" ]; then
    echo "==> Generating $OMCSI_PATH/.env from local .env"
    cp "$OMCSI_PATH/sample.env" "$OMCSI_PATH/.env"
    # Override the handful of values we care about for DPM dev
    update_env() {
        local key="$1"
        local value="$2"
        if grep -qE "^${key}=" "$OMCSI_PATH/.env"; then
            sed -i.bak -E "s|^${key}=.*|${key}=${value}|" "$OMCSI_PATH/.env" && rm -f "$OMCSI_PATH/.env.bak"
        else
            echo "${key}=${value}" >> "$OMCSI_PATH/.env"
        fi
    }
    update_env OPERATOR_UUID "${OPERATOR_UUID}"
    update_env OPERATOR_NAME "${OPERATOR_NAME}"
    update_env OPERATOR_LEVEL "${OPERATOR_LEVEL}"
    update_env OVERWRITE_EXISTING_SERVER "${OVERWRITE_EXISTING_SERVER}"
    update_env DEPLOY_AUTH_TOKEN "${DEPLOY_AUTH_TOKEN}"
    update_env RCON_PASSWORD "${RCON_PASSWORD}"
    update_env ADMIN_PASSWORD "${ADMIN_PASSWORD}"
    update_env ONLINE_MODE false
    echo "    Edit $OMCSI_PATH/.env directly to tune anything else (memory, MOTD, etc.)"
fi

# 3) Build the plugin
echo "==> Building DPM (mvn clean package -DskipTests)"
mvn clean package -DskipTests

JAR=$(find target -maxdepth 1 -name "DansPluginManager-*.jar" -not -name "original-*" -type f -print -quit)
if [ -z "$JAR" ]; then
    echo "ERROR: No plugin JAR found in target/ after build" >&2
    exit 1
fi
echo "    Built: $JAR"

# 4) Start the OMCSI stack
echo "==> Starting OMCSI stack (this may take 10-15 min on first run while Spigot is built)"
(cd "$OMCSI_PATH" && docker compose up -d --build)

# 5) Wait for the wrapper API
echo "==> Waiting for OMCSI wrapper API at $OMCSI_API_BASE ..."
deadline=$(( $(date +%s) + 600 ))
until curl -fsS "$OMCSI_API_BASE/api/server/status" >/dev/null 2>&1; do
    if [ "$(date +%s)" -ge "$deadline" ]; then
        echo "ERROR: Timed out waiting for OMCSI wrapper API" >&2
        exit 1
    fi
    sleep 5
done
echo "    Wrapper API is up."

echo "==> Waiting for Spigot to report running..."
deadline=$(( $(date +%s) + 300 ))
until curl -fsS "$OMCSI_API_BASE/api/server/status" | grep -q '"running":true'; do
    if [ "$(date +%s)" -ge "$deadline" ]; then
        echo "ERROR: Timed out waiting for Spigot to start" >&2
        exit 1
    fi
    sleep 5
done
echo "    Spigot is running."

# 6) Deploy the plugin
echo "==> Deploying $JAR to OMCSI ..."
curl -fsS -X POST "$OMCSI_API_BASE/api/plugins/deploy" \
    -H "Authorization: Bearer ${DEPLOY_AUTH_TOKEN}" \
    -F "pluginName=DansPluginManager.jar" \
    -F "file=@${JAR};type=application/java-archive" >/dev/null

# 7) Reload to activate the freshly-deployed plugin
echo "==> Reloading server to activate plugin ..."
curl -fsS -X POST "$OMCSI_API_BASE/api/server/command" \
    -H "Authorization: Bearer ${DEPLOY_AUTH_TOKEN}" \
    -H "Content-Type: text/plain; charset=utf-8" \
    --data "reload confirm" >/dev/null

echo
echo "Done. Server is on localhost:25565, wrapper API at $OMCSI_API_BASE."
echo "  - Run  ./dpm-cmd.sh \"dpm list\"  to exercise commands without a Minecraft client"
echo "  - Run  ./reload-plugin.sh        to rebuild + hot-redeploy after code changes"
echo "  - Run  ./down.sh                 to stop the stack"
