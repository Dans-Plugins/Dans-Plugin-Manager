#!/bin/bash
# Sanity check that the prerequisites for running the local test server
# (driven by OMCSI) are in place.

set -uo pipefail
cd "$(dirname "$0")"

fail=0

check_command() {
    local cmd="$1"
    if command -v "$cmd" >/dev/null 2>&1; then
        echo "✓ $cmd is installed"
    else
        echo "✗ $cmd not found in PATH"
        fail=1
    fi
}

check_file() {
    local file="$1"
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
        fail=1
    fi
}

echo "=== DPM Test Server Sanity Check ==="
echo

echo "Checking required commands..."
check_command docker
check_command mvn
check_command curl
check_command git

echo
echo "Checking required files..."
check_file sample.env

if [ ! -f .env ]; then
    echo "! .env not found — run: cp sample.env .env"
fi

echo
OMCSI_PATH="$(grep -E '^OMCSI_PATH=' sample.env | cut -d= -f2-)"
[ -f .env ] && OMCSI_PATH="$(grep -E '^OMCSI_PATH=' .env | cut -d= -f2- || true)"
OMCSI_PATH="${OMCSI_PATH:-../omcsi}"
if [ -d "$OMCSI_PATH/.git" ]; then
    echo "✓ OMCSI checkout present at $OMCSI_PATH"
else
    echo "! OMCSI checkout not found at $OMCSI_PATH — up.sh will clone it"
fi

echo
if [ "$fail" -eq 0 ]; then
    echo "All checks passed."
    echo
    echo "Next steps:"
    echo "  1. cp sample.env .env (if you haven't)"
    echo "  2. ./up.sh        # clone OMCSI, build DPM, start the stack"
    echo "  3. ./dpm-cmd.sh \"dpm list\""
    echo "  4. ./reload-plugin.sh   # rebuild + hot-redeploy after code changes"
    echo "  5. ./down.sh      # stop the stack"
    exit 0
else
    echo "Missing prerequisites — install the items marked with ✗ and re-run."
    exit 1
fi
