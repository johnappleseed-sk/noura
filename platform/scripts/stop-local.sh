#!/usr/bin/env bash

set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/local-common.sh"

load_local_env
ensure_runtime_dirs

down_infra="false"
if [[ "${1:-}" == "--down-infra" ]]; then
  down_infra="true"
fi

declare -a managed_processes=(
  "admin-web|${ADMIN_WEB_PORT}"
  "storefront-web|${STOREFRONT_PORT}"
  "api-gateway|${GATEWAY_PORT}"
  "checkout-service|${CHECKOUT_SERVICE_PORT}"
  "search-service|${SEARCH_SERVICE_PORT}"
  "review-service|${REVIEW_SERVICE_PORT}"
  "shipping-service|${SHIPPING_SERVICE_PORT}"
  "payment-service|${PAYMENT_SERVICE_PORT}"
  "order-service|${ORDER_SERVICE_PORT}"
  "cart-service|${CART_SERVICE_PORT}"
  "promotion-service|${PROMOTION_SERVICE_PORT}"
  "customer-service|${CUSTOMER_SERVICE_PORT}"
  "pricing-service|${PRICING_SERVICE_PORT}"
  "inventory-service|${INVENTORY_SERVICE_PORT}"
  "catalog-service|${CATALOG_SERVICE_PORT}"
  "notification-service|${NOTIFICATION_SERVICE_PORT}"
)

for process_entry in "${managed_processes[@]}"; do
  process_name="${process_entry%%|*}"
  process_port="${process_entry##*|}"
  cleanup_stale_pid "${process_name}"
  pid_file="$(pid_file_for "${process_name}")"
  pid=""

  if [[ ! -f "${pid_file}" ]]; then
    session_name="$(screen_session_name "${process_name}")"
    if screen_session_exists "${session_name}"; then
      log "Stopping ${process_name} screen session"
      screen -S "${session_name}" -X quit >/dev/null 2>&1 || true
    fi
  else
    pid="$(pid_from_file "${pid_file}")"
    if [[ -n "${pid}" ]] && screen_session_exists "${pid}"; then
      log "Stopping ${process_name} screen session"
      screen -S "${pid}" -X quit >/dev/null 2>&1 || true
    elif [[ -n "${pid}" ]] && is_pid_running "${pid}"; then
      log "Stopping ${process_name} (pid ${pid})"
      kill "${pid}" 2>/dev/null || true
      sleep 1
      if is_pid_running "${pid}"; then
        kill -9 "${pid}" 2>/dev/null || true
      fi
    fi
    rm -f "${pid_file}"
  fi

  if command -v lsof >/dev/null 2>&1; then
    port_pids="$(lsof -t -iTCP:"${process_port}" -sTCP:LISTEN 2>/dev/null || true)"
    if [[ -n "${port_pids}" ]]; then
      log "Stopping ${process_name} listeners on port ${process_port}"
      echo "${port_pids}" | xargs kill 2>/dev/null || true
      sleep 1
      remaining_pids="$(lsof -t -iTCP:"${process_port}" -sTCP:LISTEN 2>/dev/null || true)"
      if [[ -n "${remaining_pids}" ]]; then
        echo "${remaining_pids}" | xargs kill -9 2>/dev/null || true
      fi
    fi
  fi
done

if [[ "${down_infra}" == "true" ]]; then
  log "Stopping local infrastructure containers"
  docker_compose_local down >/dev/null
fi

log "Local managed processes stopped"
