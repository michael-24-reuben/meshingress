#!/usr/bin/env bash
#
# Starts and manages the Meshingress MCP server and artifact repository service.
#
# The packaged Spring Boot JARs run as managed background processes. Each process
# has separate stdout/stderr logs under var/logs/meshingress. The JSON state file
# under var/run records only processes started by this launcher.
#
# Use --debug for Spring Boot debug logging. Use --enable-jvm-debug only for local
# debugger attachment; JDWP listeners bind to 127.0.0.1.

set -uo pipefail

ACTION="Menu"
HEADLESS=false
DETACHED=false
DEBUG_MODE=false
BUILD=false
SKIP_TESTS=false
ENABLE_JVM_DEBUG=false
SERVER_DEBUG_PORT=5005
REPOSITORY_DEBUG_PORT=5006
FOREGROUND=false
FORCE_RESTART=false
STARTUP_TIMEOUT_SECONDS=120
SERVER_ADDRESS=""
SERVER_PORT=0
REPOSITORY_ADDRESS=""
REPOSITORY_PORT=0
SKIP_SERVER_HEALTH_CHECK=false

SCRIPT_PATH="$(readlink -f -- "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd -- "$(dirname -- "$SCRIPT_PATH")" && pwd -P)"
PROJECT_ROOT=""
LOG_DIRECTORY=""
RUN_DIRECTORY=""
STATE_PATH=""
ASCII_ART=""
LAST_ERROR=""
STARTUP_ACTIVE=false
STARTED_SERVICE_NAMES=()
declare -A SERVICE_PID=()
declare -A SERVICE_JAR=()
declare -A SERVICE_STDOUT=()
declare -A SERVICE_STDERR=()
declare -A SERVICE_STARTED_AT=()
STATE_STARTED_AT_VALUE=""
declare -A STATE_PID=()
declare -A STATE_JAR=()
declare -A STATE_STDOUT=()
declare -A STATE_STDERR=()
declare -A STATE_SERVICE_STARTED_AT=()

usage() {
    cat <<'USAGE'
Usage:
  ./Meshingress.sh [ACTION] [OPTIONS]
  ./Meshingress.sh --action ACTION [OPTIONS]

Actions:
  Menu       Launch the control center (default)
  Start      Start the services
  Restart    Stop and start the services
  Stop       Stop services owned by this launcher
  Status     Show managed service status
  Logs       Follow managed service logs

Options:
  --headless                       Relaunch the start operation detached
  --detached                       Internal flag for the detached child
  --debug                          Enable Spring Boot debug logging
  --build                          Build service JARs before starting
  --skip-tests                     Skip tests during Maven packaging
  --enable-jvm-debug               Enable loopback-only JDWP listeners
  --server-debug-port PORT         Server JDWP port (default: 5005)
  --repository-debug-port PORT     Repository JDWP port (default: 5006)
  --foreground                     Follow logs after successful startup
  --force-restart                  Replace already managed processes
  --startup-timeout-seconds SEC    Startup timeout, 10-900 (default: 120)
  --server-address ADDRESS         Override server.address
  --server-port PORT               Override server.port; 0 uses configuration
  --repository-address ADDRESS     Override repository server.address
  --repository-port PORT           Override repository server.port
  --skip-server-health-check       Probe only the server TCP port
  -h, --help                       Show this help

Examples:
  ./Meshingress.sh Start --build
  ./Meshingress.sh Start --headless
  ./Meshingress.sh Restart --force-restart
  ./Meshingress.sh Logs
USAGE
}

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

require_value() {
    local option="$1"
    local value="${2-}"
    if [[ -z "$value" ]]; then
        printf 'Missing value for %s.\n' "$option" >&2
        usage >&2
        exit 2
    fi
}

POSITIONAL_ACTION_SET=false
while (($# > 0)); do
    case "$1" in
        --action|-Action)
            require_value "$1" "${2-}"
            ACTION="$(normalize_action "$2")" || {
                printf 'Invalid action: %s\n' "$2" >&2
                exit 2
            }
            POSITIONAL_ACTION_SET=true
            shift 2
            ;;
        --action=*)
            ACTION="$(normalize_action "${1#*=}")" || {
                printf 'Invalid action: %s\n' "${1#*=}" >&2
                exit 2
            }
            POSITIONAL_ACTION_SET=true
            shift
            ;;
        --headless|-Headless) HEADLESS=true; shift ;;
        --detached|-Detached) DETACHED=true; shift ;;
        --debug|-Debug) DEBUG_MODE=true; shift ;;
        --build|-Build) BUILD=true; shift ;;
        --skip-tests|-SkipTests) SKIP_TESTS=true; shift ;;
        --enable-jvm-debug|-EnableJvmDebug) ENABLE_JVM_DEBUG=true; shift ;;
        --foreground|-Foreground) FOREGROUND=true; shift ;;
        --force-restart|-ForceRestart) FORCE_RESTART=true; shift ;;
        --skip-server-health-check|-SkipServerHealthCheck) SKIP_SERVER_HEALTH_CHECK=true; shift ;;
        --server-debug-port|-ServerDebugPort)
            require_value "$1" "${2-}"; SERVER_DEBUG_PORT="$2"; shift 2 ;;
        --server-debug-port=*) SERVER_DEBUG_PORT="${1#*=}"; shift ;;
        --repository-debug-port|-RepositoryDebugPort)
            require_value "$1" "${2-}"; REPOSITORY_DEBUG_PORT="$2"; shift 2 ;;
        --repository-debug-port=*) REPOSITORY_DEBUG_PORT="${1#*=}"; shift ;;
        --startup-timeout-seconds|-StartupTimeoutSeconds)
            require_value "$1" "${2-}"; STARTUP_TIMEOUT_SECONDS="$2"; shift 2 ;;
        --startup-timeout-seconds=*) STARTUP_TIMEOUT_SECONDS="${1#*=}"; shift ;;
        --server-address|-ServerAddress)
            require_value "$1" "${2-}"; SERVER_ADDRESS="$2"; shift 2 ;;
        --server-address=*) SERVER_ADDRESS="${1#*=}"; shift ;;
        --server-port|-ServerPort)
            require_value "$1" "${2-}"; SERVER_PORT="$2"; shift 2 ;;
        --server-port=*) SERVER_PORT="${1#*=}"; shift ;;
        --repository-address|-RepositoryAddress)
            require_value "$1" "${2-}"; REPOSITORY_ADDRESS="$2"; shift 2 ;;
        --repository-address=*) REPOSITORY_ADDRESS="${1#*=}"; shift ;;
        --repository-port|-RepositoryPort)
            require_value "$1" "${2-}"; REPOSITORY_PORT="$2"; shift 2 ;;
        --repository-port=*) REPOSITORY_PORT="${1#*=}"; shift ;;
        -h|--help)
            usage
            exit 0
            ;;
        --)
            shift
            break
            ;;
        -*)
            printf 'Unknown option: %s\n' "$1" >&2
            usage >&2
            exit 2
            ;;
        *)
            if [[ "$POSITIONAL_ACTION_SET" == true ]]; then
                printf 'Unexpected argument: %s\n' "$1" >&2
                exit 2
            fi
            ACTION="$(normalize_action "$1")" || {
                printf 'Invalid action: %s\n' "$1" >&2
                exit 2
            }
            POSITIONAL_ACTION_SET=true
            shift
            ;;
    esac
done

is_integer() {
    [[ "$1" =~ ^[0-9]+$ ]]
}

validate_range() {
    local name="$1" value="$2" minimum="$3" maximum="$4"
    if ! is_integer "$value" || (( value < minimum || value > maximum )); then
        printf '%s must be an integer from %s through %s; received: %s\n' \
            "$name" "$minimum" "$maximum" "$value" >&2
        exit 2
    fi
}

validate_range "server debug port" "$SERVER_DEBUG_PORT" 1024 65535
validate_range "repository debug port" "$REPOSITORY_DEBUG_PORT" 1024 65535
validate_range "startup timeout" "$STARTUP_TIMEOUT_SECONDS" 10 900
validate_range "server port" "$SERVER_PORT" 0 65535
validate_range "repository port" "$REPOSITORY_PORT" 0 65535

find_project_root() {
    local directory
    directory="$(readlink -f -- "$1")"

    while [[ -n "$directory" && "$directory" != "/" ]]; do
        if [[ -f "$directory/pom.xml" && \
              -d "$directory/app/meshingress-server" && \
              -d "$directory/app/meshingress-repository" ]]; then
            printf '%s\n' "$directory"
            return 0
        fi
        directory="$(dirname -- "$directory")"
    done

    if [[ -f "/pom.xml" && -d "/app/meshingress-server" && -d "/app/meshingress-repository" ]]; then
        printf '/\n'
        return 0
    fi

    return 1
}

PROJECT_ROOT="$(find_project_root "$SCRIPT_DIR")" || {
    printf 'ERROR: Could not find the Meshingress project root.\n' >&2
    exit 1
}
LOG_DIRECTORY="$PROJECT_ROOT/var/logs/meshingress"
RUN_DIRECTORY="$PROJECT_ROOT/var/run"
STATE_PATH="$RUN_DIRECTORY/meshingress-services.json"

mkdir -p -- "$LOG_DIRECTORY" "$RUN_DIRECTORY" || {
    printf 'ERROR: Could not create runtime directories under %s.\n' "$PROJECT_ROOT" >&2
    exit 1
}

for required_command in python3 timeout; do
    if ! command -v "$required_command" >/dev/null 2>&1; then
        printf 'ERROR: %s is required by the Meshingress Linux launcher.\n' "$required_command" >&2
        exit 1
    fi
done

ASCII_PATH="$SCRIPT_DIR/../data/assets/logo/meshingress-bloody.ascii.txt"
if [[ -f "$ASCII_PATH" ]]; then
    ASCII_ART="$(cat -- "$ASCII_PATH")"
elif [[ -f "$PROJECT_ROOT/data/assets/logo/meshingress-bloody.ascii.txt" ]]; then
    ASCII_ART="$(cat -- "$PROJECT_ROOT/data/assets/logo/meshingress-bloody.ascii.txt")"
fi

local_timestamp() {
    date '+%Y-%m-%dT%H:%M:%S.%3N%:z'
}

utc_timestamp() {
    date -u '+%Y-%m-%dT%H:%M:%S.%3NZ'
}

write_host_logged() {
    local message="$1"
    local level="${2:-INFO}"
    local component="${3:-launcher}"
    local event="${4:-MESSAGE}"
    local normalized line level_field timestamp

    normalized="${message//$'\r\n'/\\n}"
    normalized="${normalized//$'\r'/\\n}"
    normalized="${normalized//$'\n'/\\n}"
    printf -v level_field '%-5s' "$level"
    timestamp="$(local_timestamp)"
    line="$timestamp [$level_field] [$component] [$event] pid=$$ $normalized"

    mkdir -p -- "$LOG_DIRECTORY" || return 1
    printf '%s\n' "$line" >> "$LOG_DIRECTORY/console.log" || return 1

    if [[ "$HEADLESS" != true ]]; then
        if [[ -t 1 ]]; then
            case "$level" in
                WARN) printf '\033[33m%s\033[0m\n' "$message" ;;
                ERROR|FATAL) printf '\033[31m%s\033[0m\n' "$message" ;;
                DEBUG|TRACE) printf '\033[90m%s\033[0m\n' "$message" ;;
                *) printf '%s\n' "$message" ;;
            esac
        else
            printf '%s\n' "$message"
        fi
    fi
}

resolve_java_path() {
    local java_candidate=""

    if [[ -n "${JAVA_HOME:-}" ]]; then
        java_candidate="${JAVA_HOME%/}/bin/java"
        if [[ -x "$java_candidate" ]]; then
            printf '%s\n' "$java_candidate"
            return 0
        fi
    fi

    command -v java 2>/dev/null || return 1
}

get_java_version() {
    local java_path=""
    local version_output=""

    java_path="$(resolve_java_path 2>/dev/null || true)"

    if [[ -z "$java_path" ]]; then
        printf '%s' "-1"
        return 0
    fi

    version_output="$("$java_path" -version 2>&1 || true)"

    if [[ -z "$version_output" ]]; then
        printf '%s' "-1"
        return 0
    fi

    printf '%s' "${version_output%%$'\n'*}"
}

die() {
    local message="$1"
    write_host_logged "ERROR: $message" "ERROR" "launcher" "FAILURE" || \
        printf 'ERROR: %s\n' "$message" >&2
    if [[ "$DEBUG_MODE" == true ]]; then
        write_host_logged "source=${BASH_SOURCE[1]-unknown} line=${BASH_LINENO[0]-unknown} function=${FUNCNAME[1]-main}" \
            "DEBUG" "launcher" "STACK" || true
    fi
    exit 1
}

get_properties_value() {
    local path="$1" name="$2" default_value="$3" value
    if [[ ! -f "$path" ]]; then
        printf '%s\n' "$default_value"
        return 0
    fi

    value="$(awk -v wanted="$name" '
        /^[[:space:]]*[#!]/ { next }
        {
            pos = index($0, "=")
            if (pos == 0) next
            key = substr($0, 1, pos - 1)
            val = substr($0, pos + 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", val)
            if (key == wanted) { print val; exit }
        }
    ' "$path")"

    if [[ -z "$value" ]]; then
        printf '%s\n' "$default_value"
    else
        resolve_spring_placeholder "$value"
    fi
}

resolve_spring_placeholder() {
    local value="$1" variable fallback
    if [[ "$value" =~ ^\$\{([A-Za-z_][A-Za-z0-9_]*):([^}]*)\}$ ]]; then
        variable="${BASH_REMATCH[1]}"
        fallback="${BASH_REMATCH[2]}"
        printf '%s\n' "${!variable:-$fallback}"
    elif [[ "$value" =~ ^\$\{([A-Za-z_][A-Za-z0-9_]*)\}$ ]]; then
        variable="${BASH_REMATCH[1]}"
        printf '%s\n' "${!variable-}"
    else
        printf '%s\n' "$value"
    fi
}

get_probe_address() {
    local address="$1"
    if [[ -z "$address" || "$address" == "0.0.0.0" || "$address" == "::" || "$address" == "[::]" ]]; then
        printf '127.0.0.1\n'
    else
        printf '%s\n' "$address"
    fi
}

test_tcp_endpoint() {
    local host="$1" port="$2"
    timeout 1 bash -c 'exec 3<>"/dev/tcp/$1/$2"' _ "$host" "$port" >/dev/null 2>&1
}

test_health_endpoint() {
    local url="$1"
    if command -v curl >/dev/null 2>&1; then
        [[ "$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time 3 "$url" 2>/dev/null || true)" == "200" ]]
    elif command -v wget >/dev/null 2>&1; then
        wget --quiet --spider --timeout=3 "$url" >/dev/null 2>&1
    else
        python3 - "$url" <<'PY' >/dev/null 2>&1
import sys
from urllib.request import Request, urlopen
try:
    request = Request(sys.argv[1], headers={"User-Agent": "meshingress-launcher/1"})
    with urlopen(request, timeout=3) as response:
        raise SystemExit(0 if response.status == 200 else 1)
except Exception:
    raise SystemExit(1)
PY
    fi
}

get_listening_pid() {
    local port="$1" line="" pid=""

    if command -v ss >/dev/null 2>&1; then
        line="$(ss -H -ltnp "sport = :$port" 2>/dev/null | head -n 1 || true)"
    elif command -v lsof >/dev/null 2>&1; then
        pid="$(lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1 || true)"
        if [[ -n "$pid" ]]; then
            printf '%s\n' "$pid"
            return 0
        fi
    elif command -v netstat >/dev/null 2>&1; then
        line="$(netstat -ltnp 2>/dev/null | awk -v port=":$port" '$4 ~ port "$" {print; exit}' || true)"
    fi

    if [[ -z "$line" ]]; then
        if test_tcp_endpoint "127.0.0.1" "$port"; then
            printf 'unknown\n'
            return 0
        fi
        return 1
    fi

    if [[ "$line" =~ pid=([0-9]+) ]]; then
        pid="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ([0-9]+)/[^[:space:]]+ ]]; then
        pid="${BASH_REMATCH[1]}"
    else
        pid="unknown"
    fi

    printf '%s\n' "$pid"
}

assert_port_available() {
    local port="$1" purpose="$2" process_id
    if process_id="$(get_listening_pid "$port")"; then
        die "$purpose cannot start because port $port is already listening (PID $process_id)."
    fi
}

load_state() {
    [[ -f "$STATE_PATH" ]] || return 1

    local assignments
    assignments="$(python3 - "$STATE_PATH" <<'PY'
import json, shlex, sys
try:
    with open(sys.argv[1], "r", encoding="utf-8-sig") as stream:
        state = json.load(stream)
except Exception:
    raise SystemExit(1)

def emit(name, value):
    print(f"{name}={shlex.quote(str(value))}")

emit("STATE_STARTED_AT_VALUE", state.get("startedAt", ""))
services = state.get("services", {})
for name in ("server", "repository"):
    service = services.get(name) or {}
    key = shlex.quote(name)
    print(f"STATE_PID[{key}]={shlex.quote(str(service.get('pid', '')))}")
    print(f"STATE_JAR[{key}]={shlex.quote(str(service.get('jar', '')))}")
    print(f"STATE_STDOUT[{key}]={shlex.quote(str(service.get('stdout', '')))}")
    print(f"STATE_STDERR[{key}]={shlex.quote(str(service.get('stderr', '')))}")
    print(f"STATE_SERVICE_STARTED_AT[{key}]={shlex.quote(str(service.get('startedAt', '')))}")
PY
)" || die "The managed-service state file is unreadable: $STATE_PATH. Inspect or remove it before continuing."

    STATE_STARTED_AT_VALUE=""
    STATE_PID=()
    STATE_JAR=()
    STATE_STDOUT=()
    STATE_STDERR=()
    STATE_SERVICE_STARTED_AT=()
    eval "$assignments"
    return 0
}

process_is_zombie() {
    local pid="$1" state
    [[ -r "/proc/$pid/stat" ]] || return 1
    state="$(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null || true)"
    [[ "$state" == "Z" ]]
}

is_managed_process() {
    local pid="$1" jar="$2" command_line
    [[ "$pid" =~ ^[0-9]+$ ]] || return 1
    kill -0 "$pid" 2>/dev/null || return 1
    process_is_zombie "$pid" && return 1
    [[ -r "/proc/$pid/cmdline" ]] || return 1
    command_line="$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)"
    [[ -n "$command_line" && "$command_line" == *"$jar"* ]]
}

state_service_is_running() {
    local name="$1" pid="${STATE_PID[$1]-}" jar="${STATE_JAR[$1]-}"
    [[ -n "$pid" && -n "$jar" ]] && is_managed_process "$pid" "$jar"
}

wait_for_process_exit() {
    local pid="$1" timeout_seconds="$2" deadline
    deadline=$((SECONDS + timeout_seconds))
    while (( SECONDS < deadline )); do
        if ! kill -0 "$pid" 2>/dev/null || process_is_zombie "$pid"; then
            return 0
        fi
        sleep 0.2
    done
    return 1
}

stop_managed_services() {
    local name pid jar
    load_state || return 0

    for name in server repository; do
        pid="${STATE_PID[$name]-}"
        jar="${STATE_JAR[$name]-}"
        [[ -n "$pid" && -n "$jar" ]] || continue

        if ! is_managed_process "$pid" "$jar"; then
            write_host_logged "$name is not running as the recorded managed process." "INFO" "service" "STOP_SKIPPED"
            continue
        fi

        write_host_logged "Stopping $name (PID $pid)..." "INFO" "service" "STOPPING"
        if ! kill -TERM "$pid" 2>/dev/null; then
            die "Failed to signal $name (PID $pid)."
        fi
        if ! wait_for_process_exit "$pid" 15; then
            die "$name did not exit within 15 seconds (PID $pid)."
        fi
    done

    rm -f -- "$STATE_PATH"
    write_host_logged "Managed Meshingress services stopped." "INFO" "service" "STOPPED"
}

invoke_build() {
    local maven_wrapper="$PROJECT_ROOT/mvnw"
    local -a arguments=(-pl "app/meshingress-server,app/meshingress-repository" -am)

    [[ -f "$maven_wrapper" ]] || die "Maven wrapper not found: $maven_wrapper"
    [[ "$SKIP_TESTS" == true ]] && arguments+=(-DskipTests)
    arguments+=(package)

    write_host_logged "Building Meshingress service jars..." "INFO" "runtime" "BUILD_START"
    if [[ -x "$maven_wrapper" ]]; then
        (cd -- "$PROJECT_ROOT" && "$maven_wrapper" "${arguments[@]}")
    else
        (cd -- "$PROJECT_ROOT" && bash "$maven_wrapper" "${arguments[@]}")
    fi
    local exit_code=$?
    (( exit_code == 0 )) || die "Maven package failed with exit code $exit_code."
}

start_service_process() {
    local name="$1" java_path="$2" jar_path="$3" timestamp="$4"
    local app_array_name="$5" jvm_array_name="$6"
    local -n application_arguments="$app_array_name"
    local -n jvm_arguments="$jvm_array_name"
    local stdout_path="$LOG_DIRECTORY/$name-$timestamp.stdout.log"
    local stderr_path="$LOG_DIRECTORY/$name-$timestamp.stderr.log"
    local pid started_at

    write_host_logged "Starting $name..." "INFO" "service" "STARTING"

    (
        cd -- "$PROJECT_ROOT" || exit 1
        exec nohup "$java_path" "${jvm_arguments[@]}" -jar "$jar_path" "${application_arguments[@]}" \
            >"$stdout_path" 2>"$stderr_path" < /dev/null
    ) &
    pid=$!
    disown "$pid" 2>/dev/null || true
    sleep 0.1

    if ! kill -0 "$pid" 2>/dev/null; then
        LAST_ERROR="$name failed to launch."$'\n'"$(get_recent_logs_paths "$stdout_path" "$stderr_path")"
        return 1
    fi

    started_at="$(utc_timestamp)"
    SERVICE_PID["$name"]="$pid"
    SERVICE_JAR["$name"]="$jar_path"
    SERVICE_STDOUT["$name"]="$stdout_path"
    SERVICE_STDERR["$name"]="$stderr_path"
    SERVICE_STARTED_AT["$name"]="$started_at"
    STARTED_SERVICE_NAMES+=("$name")
    return 0
}

get_recent_logs_paths() {
    local stdout_path="$1" stderr_path="$2" path
    for path in "$stdout_path" "$stderr_path"; do
        if [[ -f "$path" ]]; then
            printf '%s\n' "--- $path ---"
            tail -n 30 -- "$path" 2>/dev/null || true
        fi
    done
}

get_recent_logs_service() {
    local name="$1"
    get_recent_logs_paths "${SERVICE_STDOUT[$name]}" "${SERVICE_STDERR[$name]}"
}

wait_for_service() {
    local name="$1" probe_address="$2" port="$3" timeout_seconds="$4" health_url="${5:-}"
    local deadline=$((SECONDS + timeout_seconds)) pid jar
    pid="${SERVICE_PID[$name]}"
    jar="${SERVICE_JAR[$name]}"

    while (( SECONDS < deadline )); do
        if ! is_managed_process "$pid" "$jar"; then
            LAST_ERROR="$name exited during startup."$'\n'"$(get_recent_logs_service "$name")"
            return 1
        fi

        if test_tcp_endpoint "$probe_address" "$port"; then
            if [[ -z "$health_url" ]] || test_health_endpoint "$health_url"; then
                write_host_logged "$name is ready on $probe_address:$port (PID $pid)." "INFO" "service" "READY"
                return 0
            fi
        fi
        sleep 0.5
    done

    LAST_ERROR="$name did not become ready within $timeout_seconds seconds."$'\n'"$(get_recent_logs_service "$name")"
    return 1
}

cleanup_started_services() {
    local name pid jar
    for name in "${STARTED_SERVICE_NAMES[@]}"; do
        pid="${SERVICE_PID[$name]-}"
        jar="${SERVICE_JAR[$name]-}"
        if [[ -n "$pid" && -n "$jar" ]] && is_managed_process "$pid" "$jar"; then
            kill -TERM "$pid" 2>/dev/null || true
        fi
    done
}

handle_startup_signal() {
    if [[ "$STARTUP_ACTIVE" == true ]]; then
        cleanup_started_services
    fi
    exit 130
}

write_state() {
    local state_started_at="$1"
    python3 - \
        "$STATE_PATH" "$state_started_at" \
        "${SERVICE_PID[server]}" "${SERVICE_JAR[server]}" "${SERVICE_STDOUT[server]}" "${SERVICE_STDERR[server]}" "${SERVICE_STARTED_AT[server]}" \
        "${SERVICE_PID[repository]}" "${SERVICE_JAR[repository]}" "${SERVICE_STDOUT[repository]}" "${SERVICE_STDERR[repository]}" "${SERVICE_STARTED_AT[repository]}" <<'PY'
import json, os, sys, tempfile
(
    path, started_at,
    server_pid, server_jar, server_stdout, server_stderr, server_started_at,
    repository_pid, repository_jar, repository_stdout, repository_stderr, repository_started_at,
) = sys.argv[1:]
state = {
    "version": 1,
    "startedAt": started_at,
    "services": {
        "server": {
            "name": "server",
            "pid": int(server_pid),
            "jar": server_jar,
            "stdout": server_stdout,
            "stderr": server_stderr,
            "startedAt": server_started_at,
        },
        "repository": {
            "name": "repository",
            "pid": int(repository_pid),
            "jar": repository_jar,
            "stdout": repository_stdout,
            "stderr": repository_stderr,
            "startedAt": repository_started_at,
        },
    },
}
directory = os.path.dirname(path)
os.makedirs(directory, exist_ok=True)
fd, temporary = tempfile.mkstemp(prefix=".meshingress-services-", suffix=".json", dir=directory)
try:
    with os.fdopen(fd, "w", encoding="utf-8") as stream:
        json.dump(state, stream, indent=2)
        stream.write("\n")
    os.replace(temporary, path)
finally:
    if os.path.exists(temporary):
        os.unlink(temporary)
PY
}

follow_one_log() {
    local service_name="$1" stream_name="$2" log_path="$3"
    tail -n 40 -- "$log_path" 2>/dev/null | while IFS= read -r line; do
        printf '[%s][%s] %s\n' "$service_name" "$stream_name" "$line"
    done
    tail --pid="$$" -n 0 -F -- "$log_path" 2>/dev/null | while IFS= read -r line; do
        printf '[%s][%s] %s\n' "$service_name" "$stream_name" "$line"
    done
}

follow_logs() {
    local name path stream
    local -a follower_pids=()
    load_state || die "No managed Meshingress services are recorded. Start them first."

    for name in server repository; do
        for stream in stdout stderr; do
            if [[ "$stream" == "stdout" ]]; then path="${STATE_STDOUT[$name]-}"; else path="${STATE_STDERR[$name]-}"; fi
            [[ -f "$path" ]] || continue
            follow_one_log "$name" "$stream" "$path" &
            follower_pids+=("$!")
        done
    done

    ((${#follower_pids[@]} > 0)) || die "No managed log files are available to follow."

    write_host_logged "Following managed logs. Press Ctrl+C to stop following; the services will keep running." \
        "INFO" "launcher" "LOG_FOLLOW"

    local interrupted=false
    trap 'interrupted=true' INT TERM
    while [[ "$interrupted" != true ]]; do
        sleep 0.25
    done

    kill "${follower_pids[@]}" 2>/dev/null || true
    wait "${follower_pids[@]}" 2>/dev/null || true
    trap - INT TERM
}

format_started_and_uptime() {
    local started_at="$1" running="$2"
    python3 - "$started_at" "$running" <<'PY'
import sys
from datetime import datetime, timezone
raw, running = sys.argv[1], sys.argv[2] == "true"
try:
    started = datetime.fromisoformat(raw.replace("Z", "+00:00"))
except Exception:
    print("__\t__")
    raise SystemExit
local = started.astimezone().strftime("%Y-%m-%d %H:%M:%S")
if not running:
    print(f"{local}\t__")
    raise SystemExit
elapsed = max(0, int((datetime.now(timezone.utc) - started.astimezone(timezone.utc)).total_seconds()))
days, remainder = divmod(elapsed, 86400)
hours, remainder = divmod(remainder, 3600)
minutes, seconds = divmod(remainder, 60)
print(f"{local}\t{days:02d}.{hours:02d}:{minutes:02d}:{seconds:02d}")
PY
}

repeat_char() {
    local character="$1" count="$2" index
    (( count <= 0 )) && return 0
    for ((index = 0; index < count; index++)); do
        printf '%s' "$character"
    done
}

write_meshingress_panel() {
    local title="$1" width="${2:-93}"
    shift 2
    local -a rows=("$@")
    local inner_width=$((width - 4))
    local title_text=" $title " remaining left_length right_length row display
    remaining=$((width - 2 - ${#title_text}))
    (( remaining < 0 )) && remaining=0
    left_length=$((remaining / 2))
    right_length=$((remaining - left_length))

    printf '╔'
    repeat_char '═' "$left_length"
    printf '%s' "$title_text"
    repeat_char '═' "$right_length"
    printf '╗\n'
    for row in "${rows[@]}"; do
        display="$row"
        if ((${#display} > inner_width)); then
            display="${display:0:inner_width-1}…"
        fi
        printf '║ %-*s ║\n' "$inner_width" "$display"
    done
    printf '╚'
    repeat_char '═' "$((width - 2))"
    printf '╝\n'
}

show_meshingress_control_center() {
    local runtime_state started_at="__" uptime="__" java_version running=false state_present=false
    local started_data choice

    while true; do
        if [[ -t 1 ]]; then
            clear 2>/dev/null || printf '\033c'
        fi
        if load_state; then
            state_present=true
            if state_service_is_running server || state_service_is_running repository; then
                running=true
                runtime_state="RUNNING"
            else
                running=false
                runtime_state="STALE STATE"
            fi
            local raw_started="$STATE_STARTED_AT_VALUE"
            if [[ -n "$raw_started" ]]; then
                started_data="$(format_started_and_uptime "$raw_started" "$running")"
                IFS=$'\t' read -r started_at uptime <<< "$started_data"
            fi
        else
            state_present=false
            running=false
            runtime_state="STOPPED"
            started_at="__"
            uptime="__"
        fi

        [[ -n "$ASCII_ART" ]] && printf '%s\n' "$ASCII_ART"

        java_version="$(get_java_version)"

        write_meshingress_panel "MESHINGRESS CONTROL CENTER" 93 \
            "Runtime       $runtime_state" \
            "Server        http://$(get_probe_address "$SERVER_ADDRESS"):$SERVER_PORT" \
            "Repository    http://$(get_probe_address "$REPOSITORY_ADDRESS"):$REPOSITORY_PORT" \
            "Started       $started_at" \
            "Uptime        $uptime" \
            "Java          $([[ "$java_version" == "-1" ]] && printf 'Java not found' || printf '%s' "$java_version")" \
            "Logs          $LOG_DIRECTORY"

        printf '\n'
        printf '  [1] Start              [2] Start headless\n'
        printf '  [3] Status             [4] Follow logs\n'
        printf '  [5] Restart            [6] Stop\n'
        printf '  [Q] Exit\n\n'
        read -r -p 'Select: ' choice
        case "${choice^^}" in
            1) ACTION="Start"; return ;;
            2) HEADLESS=true; ACTION="Start"; return ;;
            3) ACTION="Status"; return ;;
            4) ACTION="Logs"; return ;;
            5) ACTION="Restart"; return ;;
            6) ACTION="Stop"; return ;;
            Q) ACTION="Exit"; return ;;
        esac
    done
}

write_host_logged "Starting Meshingress services - [$(date '+%Y-%m-%d %H:%M:%S')]" "INFO" "launcher" "LAUNCH"

SERVER_PROPERTIES_PATH="$PROJECT_ROOT/app/meshingress-server/src/main/resources/application.properties"
REPOSITORY_PROPERTIES_PATH="$PROJECT_ROOT/app/meshingress-repository/src/main/resources/application.properties"

if [[ -z "$SERVER_ADDRESS" ]]; then
    SERVER_ADDRESS="$(get_properties_value "$SERVER_PROPERTIES_PATH" "server.address" "127.0.0.1")"
    write_host_logged "Server address not specified. Using default: $SERVER_ADDRESS" "INFO" "config" "DEFAULT"
fi
if [[ -z "$REPOSITORY_ADDRESS" ]]; then
    REPOSITORY_ADDRESS="$(get_properties_value "$REPOSITORY_PROPERTIES_PATH" "server.address" "127.0.0.1")"
    write_host_logged "Repository address not specified. Using default: $REPOSITORY_ADDRESS" "INFO" "config" "DEFAULT"
fi
if (( SERVER_PORT == 0 )); then
    SERVER_PORT="$(get_properties_value "$SERVER_PROPERTIES_PATH" "server.port" "4737")"
    is_integer "$SERVER_PORT" || die "Configured server.port is not numeric: $SERVER_PORT"
    write_host_logged "Server port not specified. Using default: $SERVER_PORT" "INFO" "config" "DEFAULT"
fi
if (( REPOSITORY_PORT == 0 )); then
    REPOSITORY_PORT="$(get_properties_value "$REPOSITORY_PROPERTIES_PATH" "server.port" "4738")"
    is_integer "$REPOSITORY_PORT" || die "Configured repository server.port is not numeric: $REPOSITORY_PORT"
    write_host_logged "Repository port not specified. Using default: $REPOSITORY_PORT" "INFO" "config" "DEFAULT"
fi
validate_range "server port" "$SERVER_PORT" 1 65535
validate_range "repository port" "$REPOSITORY_PORT" 1 65535

if [[ "$ACTION" == "Menu" ]]; then
    show_meshingress_control_center
    [[ "$ACTION" == "Exit" ]] && exit 0
fi

if [[ "$ACTION" == "Restart" ]]; then
    if [[ -f "$STATE_PATH" ]]; then
        stop_managed_services
    fi
    ACTION="Start"
fi

if [[ "$ACTION" == "Stop" ]]; then
    if [[ ! -f "$STATE_PATH" ]]; then
        write_host_logged "No managed Meshingress services are recorded." "INFO" "service" "NOT_FOUND"
        exit 0
    fi
    stop_managed_services
    exit 0
fi

if [[ "$ACTION" == "Status" ]]; then
    if ! load_state; then
        write_host_logged "No managed Meshingress services are recorded." "INFO" "service" "NOT_FOUND"
        exit 0
    fi
    for name in server repository; do
        pid="${STATE_PID[$name]-}"
        if state_service_is_running "$name"; then
            stdout_path="${STATE_STDOUT[$name]-}"
            stderr_path="${STATE_STDERR[$name]-}"
            write_host_logged "$name: running (PID $pid), stdout: $stdout_path, stderr: $stderr_path" \
                "INFO" "service" "STATUS"
        else
            write_host_logged "$name: stopped or no longer owned by this launcher" "INFO" "service" "STATUS"
        fi
    done
    exit 0
fi

if [[ "$ACTION" == "Logs" ]]; then
    follow_logs
    exit 0
fi

[[ "$ACTION" == "Start" ]] || die "Unsupported action after dispatch: $ACTION"

if [[ "$HEADLESS" == true && "$DETACHED" != true ]]; then
    launcher_stdout="$LOG_DIRECTORY/headless-launcher.stdout.log"
    launcher_stderr="$LOG_DIRECTORY/headless-launcher.stderr.log"
    detached_arguments=(
        --action Start
        --headless
        --detached
        --startup-timeout-seconds "$STARTUP_TIMEOUT_SECONDS"
        --server-address "$SERVER_ADDRESS"
        --server-port "$SERVER_PORT"
        --repository-address "$REPOSITORY_ADDRESS"
        --repository-port "$REPOSITORY_PORT"
        --server-debug-port "$SERVER_DEBUG_PORT"
        --repository-debug-port "$REPOSITORY_DEBUG_PORT"
    )
    [[ "$DEBUG_MODE" == true ]] && detached_arguments+=(--debug)
    [[ "$BUILD" == true ]] && detached_arguments+=(--build)
    [[ "$SKIP_TESTS" == true ]] && detached_arguments+=(--skip-tests)
    [[ "$ENABLE_JVM_DEBUG" == true ]] && detached_arguments+=(--enable-jvm-debug)
    [[ "$FORCE_RESTART" == true ]] && detached_arguments+=(--force-restart)
    [[ "$SKIP_SERVER_HEALTH_CHECK" == true ]] && detached_arguments+=(--skip-server-health-check)

    (
        cd -- "$PROJECT_ROOT" || exit 1
        exec nohup bash "$SCRIPT_PATH" "${detached_arguments[@]}" \
            >>"$launcher_stdout" 2>>"$launcher_stderr" < /dev/null
    ) &
    detached_pid=$!
    disown "$detached_pid" 2>/dev/null || true

    message="Started headless Meshingress process. PID: $detached_pid"
    printf '%s\n' "$message"
    write_host_logged "$message" "INFO" "launcher" "DETACHED"
    exit 0
fi

if [[ "$HEADLESS" != true ]]; then
    clear 2>/dev/null || true
    [[ -n "$ASCII_ART" ]] && printf '%s\n' "$ASCII_ART"
fi

SERVER_JAR="$PROJECT_ROOT/app/meshingress-server/target/meshingress.jar"
REPOSITORY_JAR="$PROJECT_ROOT/app/meshingress-repository/target/meshingress-repository.jar"
if [[ "$BUILD" == true || ! -f "$SERVER_JAR" || ! -f "$REPOSITORY_JAR" ]]; then
    invoke_build
fi
[[ -f "$SERVER_JAR" && -f "$REPOSITORY_JAR" ]] || die "Packaged service JARs are missing after the build."

JAVA_PATH="$(command -v java 2>/dev/null || true)"
[[ -n "$JAVA_PATH" ]] || die "Java was not found on PATH. Meshingress requires Java 25."

if load_state; then
    running_count=0
    state_service_is_running server && ((running_count += 1))
    state_service_is_running repository && ((running_count += 1))
    if (( running_count > 0 )); then
        if [[ "$FORCE_RESTART" != true ]]; then
            die "Managed Meshingress services are already running. Use Status, Logs, or --force-restart."
        fi
        stop_managed_services
    else
        rm -f -- "$STATE_PATH"
    fi
fi

assert_port_available "$SERVER_PORT" "Meshingress server"
assert_port_available "$REPOSITORY_PORT" "Meshingress repository"
if [[ "$ENABLE_JVM_DEBUG" == true ]]; then
    assert_port_available "$SERVER_DEBUG_PORT" "Meshingress server JVM debugger"
    assert_port_available "$REPOSITORY_DEBUG_PORT" "Meshingress repository JVM debugger"
fi

SERVER_ARGUMENTS=("--server.address=$SERVER_ADDRESS" "--server.port=$SERVER_PORT")
REPOSITORY_ARGUMENTS=("--server.address=$REPOSITORY_ADDRESS" "--server.port=$REPOSITORY_PORT")
if [[ "$DEBUG_MODE" == true ]]; then
    SERVER_ARGUMENTS+=(--debug --logging.level.dev.mrk.meshingress=DEBUG)
    REPOSITORY_ARGUMENTS+=(--debug --logging.level.dev.mrk.meshingress=DEBUG)
fi

SERVER_JVM_ARGUMENTS=()
REPOSITORY_JVM_ARGUMENTS=()
if [[ "$ENABLE_JVM_DEBUG" == true ]]; then
    SERVER_JVM_ARGUMENTS+=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$SERVER_DEBUG_PORT")
    REPOSITORY_JVM_ARGUMENTS+=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$REPOSITORY_DEBUG_PORT")
fi

trap handle_startup_signal INT TERM
STARTUP_ACTIVE=true
FILE_TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"

if ! start_service_process "repository" "$JAVA_PATH" "$REPOSITORY_JAR" "$FILE_TIMESTAMP" \
    REPOSITORY_ARGUMENTS REPOSITORY_JVM_ARGUMENTS; then
    cleanup_started_services
    die "$LAST_ERROR"
fi
if ! wait_for_service "repository" "$(get_probe_address "$REPOSITORY_ADDRESS")" "$REPOSITORY_PORT" "$STARTUP_TIMEOUT_SECONDS"; then
    cleanup_started_services
    die "$LAST_ERROR"
fi

if ! start_service_process "server" "$JAVA_PATH" "$SERVER_JAR" "$FILE_TIMESTAMP" \
    SERVER_ARGUMENTS SERVER_JVM_ARGUMENTS; then
    cleanup_started_services
    die "$LAST_ERROR"
fi

HEALTH_URL=""
if [[ "$SKIP_SERVER_HEALTH_CHECK" != true ]]; then
    HEALTH_URL="http://$(get_probe_address "$SERVER_ADDRESS"):$SERVER_PORT/actuator/health"
fi
if ! wait_for_service "server" "$(get_probe_address "$SERVER_ADDRESS")" "$SERVER_PORT" "$STARTUP_TIMEOUT_SECONDS" "$HEALTH_URL"; then
    cleanup_started_services
    die "$LAST_ERROR"
fi

STATE_STARTED_AT="$(utc_timestamp)"
if ! write_state "$STATE_STARTED_AT"; then
    cleanup_started_services
    die "Could not write managed-service state: $STATE_PATH"
fi

STARTUP_ACTIVE=false
trap - INT TERM

write_host_logged "Meshingress services are running." "INFO" "service" "RUNNING"
write_host_logged "Server health: http://$(get_probe_address "$SERVER_ADDRESS"):$SERVER_PORT/actuator/health" \
    "INFO" "service" "HEALTH"
write_host_logged "Logs: $LOG_DIRECTORY" "INFO" "launcher" "LOG_PATH"
write_host_logged "Stop: ./scripts/Meshingress.sh Stop" "INFO" "launcher" "STOP_HINT"

if [[ "$FOREGROUND" == true ]]; then
    follow_logs
fi
