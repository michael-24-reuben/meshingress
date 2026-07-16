#!/usr/bin/env bash
# Linux action dispatcher for Meshingress.

set -euo pipefail

SCRIPT_PATH="$(readlink -f -- "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd -- "$(dirname -- "$SCRIPT_PATH")" && pwd -P)"
RUNTIME="$SCRIPT_DIR/private/Runtime.sh"

normalize_action() {
    case "${1,,}" in
        menu) printf 'Menu\n' ;;
        start) printf 'Start\n' ;;
        restart) printf 'Restart\n' ;;
        stop) printf 'Stop\n' ;;
        status) printf 'Status\n' ;;
        logs) printf 'Logs\n' ;;
        *) return 1 ;;
    esac
}

action="Menu"
expect_action_value=false
positional_action_seen=false

for argument in "$@"; do
    if [[ "$expect_action_value" == true ]]; then
        action="$(normalize_action "$argument" 2>/dev/null || true)"
        expect_action_value=false
        continue
    fi

    case "$argument" in
        --action|-Action)
            expect_action_value=true
            ;;
        --action=*)
            action="$(normalize_action "${argument#*=}" 2>/dev/null || true)"
            ;;
        -*)
            ;;
        *)
            if [[ "$positional_action_seen" == false ]]; then
                action="$(normalize_action "$argument" 2>/dev/null || true)"
                positional_action_seen=true
            fi
            ;;
    esac
done

if [[ -n "$action" && -f "$SCRIPT_DIR/actions/$action.sh" ]]; then
    exec bash "$SCRIPT_DIR/actions/$action.sh" "$@"
fi

exec bash "$RUNTIME" "$@"
