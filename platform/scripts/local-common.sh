#!/usr/bin/env bash

set -euo pipefail

LOCAL_SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${LOCAL_SCRIPT_DIR}/../.." && pwd)"
LOCAL_ENV_FILE="${LOCAL_SCRIPT_DIR}/.env"
LOCAL_ENV_TEMPLATE="${LOCAL_SCRIPT_DIR}/.env.example"
LOCAL_RUNTIME_DIR="${REPO_ROOT}/platform/.local"
LOCAL_LOG_DIR="${LOCAL_RUNTIME_DIR}/logs"
LOCAL_PID_DIR="${LOCAL_RUNTIME_DIR}/pids"

log() {
  printf '[local] %s\n' "$*"
}

warn() {
  printf '[local][warn] %s\n' "$*" >&2
}

fail() {
  printf '[local][error] %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

load_local_env() {
  local env_source="${LOCAL_ENV_TEMPLATE}"
  if [[ -f "${LOCAL_ENV_FILE}" ]]; then
    env_source="${LOCAL_ENV_FILE}"
  fi

  set -a
  # shellcheck disable=SC1090
  source "${env_source}"
  set +a

  : "${APP_DB_NAME:=commerce_platform}"
  : "${APP_DB_USER:=commerce}"
  : "${APP_DB_PASSWORD:=commerce}"
  : "${POSTGRES_SUPER_USER:=postgres}"
  : "${POSTGRES_SUPER_PASSWORD:=postgres}"
  : "${GATEWAY_PORT:=8080}"
  : "${APP_PORT:=8082}"
  : "${NOTIFICATION_SERVICE_PORT:=8083}"
  : "${CATALOG_SERVICE_PORT:=8084}"
  : "${SEARCH_SERVICE_PORT:=8085}"
  : "${INVENTORY_SERVICE_PORT:=8086}"
  : "${PRICING_SERVICE_PORT:=8087}"
  : "${CART_SERVICE_PORT:=8088}"
  : "${CUSTOMER_SERVICE_PORT:=8089}"
  : "${ORDER_SERVICE_PORT:=8090}"
  : "${CHECKOUT_SERVICE_PORT:=8091}"
  : "${PAYMENT_SERVICE_PORT:=8092}"
  : "${SHIPPING_SERVICE_PORT:=8093}"
  : "${PROMOTION_SERVICE_PORT:=8094}"
  : "${REVIEW_SERVICE_PORT:=8095}"
  : "${REDIS_PORT:=6379}"
  : "${KAFKA_HOST_PORT:=29092}"
  : "${KEYCLOAK_PORT:=18080}"
  : "${LOCAL_INTERNAL_API_KEY:=noura-local-internal-key}"
  : "${LOCAL_JAVA_HIKARI_MAXIMUM_POOL_SIZE:=3}"
  : "${LOCAL_JAVA_HIKARI_MINIMUM_IDLE:=0}"
  : "${LOCAL_JAVA_HIKARI_IDLE_TIMEOUT:=10000}"
  : "${STOREFRONT_PORT:=3001}"
  : "${ADMIN_WEB_PORT:=5173}"
}

ensure_runtime_dirs() {
  mkdir -p "${LOCAL_RUNTIME_DIR}" "${LOCAL_LOG_DIR}" "${LOCAL_PID_DIR}"
}

docker_compose_local() {
  docker compose -f "${LOCAL_SCRIPT_DIR}/docker-compose.local.yml" "$@"
}

wait_for_container_health() {
  local container_name="$1"
  local attempts="${2:-90}"
  local delay_seconds="${3:-2}"
  local attempt

  for ((attempt = 1; attempt <= attempts; attempt += 1)); do
    local status=""
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
      "${container_name}" 2>/dev/null || true)"
    case "${status}" in
      healthy|running)
        return 0
        ;;
      *)
        sleep "${delay_seconds}"
        ;;
    esac
  done

  fail "Container ${container_name} did not become healthy"
}

db_query() {
  local sql="$1"
  docker exec -i noura-postgres \
    psql -tA -v ON_ERROR_STOP=1 \
    -U "${POSTGRES_SUPER_USER}" \
    -d "${APP_DB_NAME}" \
    -c "${sql}"
}

table_exists() {
  local table_name="$1"
  [[ "$(db_query "SELECT to_regclass('public.${table_name}') IS NOT NULL;")" == "t" ]]
}

run_sql() {
  local sql="$1"
  docker exec -i noura-postgres \
    psql -v ON_ERROR_STOP=1 \
    -U "${POSTGRES_SUPER_USER}" \
    -d "${APP_DB_NAME}" \
    -c "${sql}"
}

run_sql_file() {
  local sql_file="$1"
  docker exec -i noura-postgres \
    psql -v ON_ERROR_STOP=1 \
    -U "${POSTGRES_SUPER_USER}" \
    -d "${APP_DB_NAME}" < "${sql_file}"
}

url_responds() {
  local url="$1"
  curl -fsS --max-time 5 "${url}" >/dev/null 2>&1
}

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="${3:-120}"
  local delay_seconds="${4:-2}"
  local attempt

  for ((attempt = 1; attempt <= attempts; attempt += 1)); do
    if url_responds "${url}"; then
      return 0
    fi
    sleep "${delay_seconds}"
  done

  warn "${label} did not become ready at ${url}"
  return 1
}

pid_file_for() {
  local name="$1"
  printf '%s/%s.pid' "${LOCAL_PID_DIR}" "${name}"
}

log_file_for() {
  local name="$1"
  printf '%s/%s.log' "${LOCAL_LOG_DIR}" "${name}"
}

pid_from_file() {
  local pid_file="$1"
  if [[ -f "${pid_file}" ]]; then
    tr -d '[:space:]' < "${pid_file}"
  fi
}

is_pid_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

screen_session_name() {
  local name="$1"
  printf 'noura-%s' "${name}"
}

screen_session_exists() {
  local session_name="$1"
  local sessions=""
  if ! command -v screen >/dev/null 2>&1; then
    return 1
  fi
  sessions="$(screen -list 2>/dev/null || true)"
  grep -q "[.]${session_name}[[:space:]]" <<< "${sessions}"
}

state_entry_alive() {
  local entry="$1"
  if [[ -z "${entry}" ]]; then
    return 1
  fi
  if [[ "${entry}" =~ ^[0-9]+$ ]]; then
    is_pid_running "${entry}"
    return
  fi
  screen_session_exists "${entry}"
}

cleanup_stale_pid() {
  local name="$1"
  local pid_file
  pid_file="$(pid_file_for "${name}")"

  if [[ -f "${pid_file}" ]]; then
    local pid=""
    pid="$(pid_from_file "${pid_file}")"
    if [[ -z "${pid}" ]] || ! state_entry_alive "${pid}"; then
      rm -f "${pid_file}"
    fi
  fi
}

port_in_use() {
  local port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    return 1
  fi
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}
