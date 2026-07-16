#!/usr/bin/env bash
# Convenience entry point for Meshingress on Linux.

set -euo pipefail

SCRIPT_PATH="$(readlink -f -- "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd -- "$(dirname -- "$SCRIPT_PATH")" && pwd -P)"
LINUX_LAUNCHER="$SCRIPT_DIR/linux/Meshingress.sh"

[[ -f "$LINUX_LAUNCHER" ]] || {
    printf 'ERROR: Linux Meshingress launcher not found: %s\n' "$LINUX_LAUNCHER" >&2
    exit 1
}

exec bash "$LINUX_LAUNCHER" "$@"
