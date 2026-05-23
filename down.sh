#!/bin/bash
# Stop the OMCSI test stack.

set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
fi

OMCSI_PATH="${OMCSI_PATH:-../omcsi}"

if [ ! -d "$OMCSI_PATH" ]; then
    echo "OMCSI checkout not found at $OMCSI_PATH — nothing to stop."
    exit 0
fi

(cd "$OMCSI_PATH" && docker compose down --remove-orphans)
