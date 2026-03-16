#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${BACKEND_DIR}/logs"
LOG_FILE="${LOG_DIR}/backend.jsonl"
DEFAULT_PROFILE="local-postgres"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/logs.sh run [profile]
  ./scripts/logs.sh pretty
  ./scripts/logs.sh errors
  ./scripts/logs.sh corr <correlation-id>
  ./scripts/logs.sh lnav
  ./scripts/logs.sh fields

Commands:
  run      Start Spring Boot and persist JSON logs to logs/backend.jsonl.
  pretty   Pretty-print live JSON logs (tail -F | jq .).
  errors   Show live WARN/ERROR logs only.
  corr     Show live logs for a specific correlationId.
  lnav     Open logs/backend.jsonl in lnav.
  fields   Print required logging fields from the latest log entry.
EOF
}

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}" >&2
    exit 1
  fi
}

ensure_log_file() {
  mkdir -p "${LOG_DIR}"
  touch "${LOG_FILE}"
}

run_backend() {
  local profile="${1:-${DEFAULT_PROFILE}}"
  ensure_log_file
  cd "${BACKEND_DIR}"
  echo "Writing JSON logs to ${LOG_FILE}"
  ./mvnw -q -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.profiles="${profile}" 2>&1 \
    | awk '/^\{/' \
    | tee -a "${LOG_FILE}"
}

pretty_logs() {
  require_cmd jq
  ensure_log_file
  tail -F "${LOG_FILE}" | jq .
}

error_logs() {
  require_cmd jq
  ensure_log_file
  tail -F "${LOG_FILE}" \
    | jq -rc 'select(.level=="WARN" or .level=="ERROR") | {timestamp,level,logger,message,correlationId,exception}'
}

correlation_logs() {
  require_cmd jq
  ensure_log_file
  local correlation_id="${1:-}"
  if [[ -z "${correlation_id}" ]]; then
    echo "Usage: ./scripts/logs.sh corr <correlation-id>" >&2
    exit 1
  fi
  tail -F "${LOG_FILE}" \
    | jq -rc --arg cid "${correlation_id}" 'select(.correlationId==$cid)'
}

open_lnav() {
  require_cmd lnav
  ensure_log_file
  lnav "${LOG_FILE}"
}

show_required_fields() {
  require_cmd jq
  ensure_log_file
  if [[ ! -s "${LOG_FILE}" ]]; then
    echo "No log entries found in ${LOG_FILE}. Start logs first: ./scripts/logs.sh run" >&2
    exit 1
  fi
  local latest_json
  latest_json="$(tail -n 500 "${LOG_FILE}" | jq -Rrc 'try fromjson catch empty' | tail -n 1)"
  if [[ -z "${latest_json}" ]]; then
    echo "No valid JSON log entries found in ${LOG_FILE}." >&2
    exit 1
  fi
  printf '%s\n' "${latest_json}" \
    | jq '{timestamp,level,app,env,logger,thread,message,correlationId,exception}'
}

main() {
  local command="${1:-help}"
  case "${command}" in
    run)
      run_backend "${2:-${DEFAULT_PROFILE}}"
      ;;
    pretty)
      pretty_logs
      ;;
    errors)
      error_logs
      ;;
    corr)
      correlation_logs "${2:-}"
      ;;
    lnav)
      open_lnav
      ;;
    fields)
      show_required_fields
      ;;
    help|-h|--help)
      usage
      ;;
    *)
      echo "Unknown command: ${command}" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
