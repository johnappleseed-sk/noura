#!/usr/bin/env bash

set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/local-common.sh"

load_local_env
ensure_runtime_dirs

require_command curl
require_command docker
require_command mvn
require_command npm

declare -a java_services=(
  "notification-service|services/notification-service|${NOTIFICATION_SERVICE_PORT}"
  "catalog-service|services/catalog-service|${CATALOG_SERVICE_PORT}"
  "inventory-service|services/inventory-service|${INVENTORY_SERVICE_PORT}"
  "pricing-service|services/pricing-service|${PRICING_SERVICE_PORT}"
  "customer-service|services/customer-service|${CUSTOMER_SERVICE_PORT}"
  "promotion-service|services/promotion-service|${PROMOTION_SERVICE_PORT}"
  "cart-service|services/cart-service|${CART_SERVICE_PORT}"
  "order-service|services/order-service|${ORDER_SERVICE_PORT}"
  "payment-service|services/payment-service|${PAYMENT_SERVICE_PORT}"
  "shipping-service|services/shipping-service|${SHIPPING_SERVICE_PORT}"
  "review-service|services/review-service|${REVIEW_SERVICE_PORT}"
  "search-service|services/search-service|${SEARCH_SERVICE_PORT}"
  "checkout-service|services/checkout-service|${CHECKOUT_SERVICE_PORT}"
  "api-gateway|apps/api-gateway|${GATEWAY_PORT}"
)

start_infrastructure() {
  log "Starting shared local infrastructure"
  docker_compose_local up -d postgres redis kafka keycloak >/dev/null
  wait_for_container_health noura-postgres
  wait_for_container_health noura-redis
  wait_for_container_health noura-kafka

  if [[ "${GATEWAY_AUTH_ENABLED:-false}" == "true" ]]; then
    wait_for_container_health noura-keycloak 120 2
  else
    log "Keycloak container started; auth is disabled locally, so Keycloak health is not treated as a startup blocker"
  fi
}

configure_java_service_env() {
  local name="$1"

  export DB_URL="jdbc:postgresql://localhost:5432/${APP_DB_NAME}"
  export DB_USERNAME="${APP_DB_USER}"
  export DB_PASSWORD="${APP_DB_PASSWORD}"
  export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${LOCAL_JAVA_HIKARI_MAXIMUM_POOL_SIZE}"
  export SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE="${LOCAL_JAVA_HIKARI_MINIMUM_IDLE}"
  export SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT="${LOCAL_JAVA_HIKARI_IDLE_TIMEOUT}"

  case "${name}" in
    catalog-service)
      export API_VERSION_PREFIX="/api/v1"
      ;;
    search-service)
      export API_VERSION_PREFIX="/api/v1"
      export APP_INTERNAL_API_KEY="${LOCAL_INTERNAL_API_KEY}"
      ;;
    customer-service|order-service|notification-service|promotion-service)
      export APP_INTERNAL_API_KEY="${LOCAL_INTERNAL_API_KEY}"
      ;;
    checkout-service)
      export CART_SERVICE_BASE_URL="http://localhost:${CART_SERVICE_PORT}"
      export CUSTOMER_SERVICE_BASE_URL="http://localhost:${CUSTOMER_SERVICE_PORT}"
      export PRICING_SERVICE_BASE_URL="http://localhost:${PRICING_SERVICE_PORT}"
      export INVENTORY_SERVICE_BASE_URL="http://localhost:${INVENTORY_SERVICE_PORT}"
      export ORDER_SERVICE_BASE_URL="http://localhost:${ORDER_SERVICE_PORT}"
      export PAYMENT_SERVICE_BASE_URL="http://localhost:${PAYMENT_SERVICE_PORT}"
      export NOTIFICATION_SERVICE_BASE_URL="http://localhost:${NOTIFICATION_SERVICE_PORT}"
      export CUSTOMER_SERVICE_INTERNAL_API_KEY="${LOCAL_INTERNAL_API_KEY}"
      export ORDER_SERVICE_INTERNAL_API_KEY="${LOCAL_INTERNAL_API_KEY}"
      export NOTIFICATION_SERVICE_INTERNAL_API_KEY="${LOCAL_INTERNAL_API_KEY}"
      ;;
    api-gateway)
      export GATEWAY_AUTH_ENABLED="false"
      export GATEWAY_FORWARDED_HEADERS_ENABLED="false"
      export GATEWAY_X_FORWARDED_ENABLED="false"
      export KEYCLOAK_URL="http://localhost:${KEYCLOAK_PORT}"
      export APP_SERVICE_URL="http://localhost:${APP_PORT}"
      export NOTIFICATION_SERVICE_URL="http://localhost:${NOTIFICATION_SERVICE_PORT}"
      export CATALOG_SERVICE_URL="http://localhost:${CATALOG_SERVICE_PORT}"
      export SEARCH_SERVICE_URL="http://localhost:${SEARCH_SERVICE_PORT}"
      export INVENTORY_SERVICE_URL="http://localhost:${INVENTORY_SERVICE_PORT}"
      export PRICING_SERVICE_URL="http://localhost:${PRICING_SERVICE_PORT}"
      export CART_SERVICE_URL="http://localhost:${CART_SERVICE_PORT}"
      export CUSTOMER_SERVICE_URL="http://localhost:${CUSTOMER_SERVICE_PORT}"
      export ORDER_SERVICE_URL="http://localhost:${ORDER_SERVICE_PORT}"
      export CHECKOUT_SERVICE_URL="http://localhost:${CHECKOUT_SERVICE_PORT}"
      export PAYMENT_SERVICE_URL="http://localhost:${PAYMENT_SERVICE_PORT}"
      export SHIPPING_SERVICE_URL="http://localhost:${SHIPPING_SERVICE_PORT}"
      export PROMOTION_SERVICE_URL="http://localhost:${PROMOTION_SERVICE_PORT}"
      export REVIEW_SERVICE_URL="http://localhost:${REVIEW_SERVICE_PORT}"
      ;;
  esac
}

start_detached_command() {
  local name="$1"
  local workdir="$2"
  local command_line="$3"
  local log_file="$4"
  local pid_file="$5"
  local session_name

  session_name="$(screen_session_name "${name}")"

  # `screen` is used as the local process supervisor when available because plain
  # background jobs proved unreliable for long-running dev servers on this machine.
  if command -v screen >/dev/null 2>&1; then
    screen -wipe >/dev/null 2>&1 || true
    screen -dmS "${session_name}" \
      bash -lc "cd '${workdir}' && exec ${command_line} >> '${log_file}' 2>&1"
    echo "${session_name}" > "${pid_file}"
    return 0
  fi

  (
    cd "${workdir}"
    nohup bash -lc "exec ${command_line}" < /dev/null > "${log_file}" 2>&1 &
    started_pid=$!
    disown "${started_pid}" 2>/dev/null || true
    echo "${started_pid}" > "${pid_file}"
  )
}

start_java_service() {
  local name="$1"
  local relative_dir="$2"
  local port="$3"
  local readiness_url="http://localhost:${port}/actuator/health/readiness"
  local pid_file
  local log_file

  cleanup_stale_pid "${name}"
  pid_file="$(pid_file_for "${name}")"
  log_file="$(log_file_for "${name}")"

  if url_responds "${readiness_url}"; then
    log "${name} is already responding on port ${port}; leaving the existing process untouched"
    return 0
  fi

  if [[ ! -f "${pid_file}" ]] && port_in_use "${port}"; then
    fail "Port ${port} is already in use for ${name}. Stop the conflicting process before running the local stack."
  fi

  log "Starting ${name} on port ${port}"
  (
    export SERVER_PORT="${port}"
    configure_java_service_env "${name}"
    if [[ "${name}" == "api-gateway" ]]; then
      # Java 25 + `spring-boot:run` is unreliable for this gateway module, so we
      # build a runtime classpath once and launch the main class directly.
      (
        cd "${REPO_ROOT}/${relative_dir}"
        mvn -q -Dmaven.test.skip=true compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
      )
      start_detached_command \
        "${name}" \
        "${REPO_ROOT}/${relative_dir}" \
        "java -cp \"target/classes:\$(tr -d '\\n' < target/runtime-classpath.txt)\" com.company.platform.gateway.EdgeGatewayApplication" \
        "${log_file}" \
        "${pid_file}"
    else
      start_detached_command \
        "${name}" \
        "${REPO_ROOT}/${relative_dir}" \
        "mvn -q -Dmaven.test.skip=true spring-boot:run" \
        "${log_file}" \
        "${pid_file}"
    fi
  )

  if ! wait_for_url "${readiness_url}" "${name}" 180 2; then
    warn "Recent ${name} log output:"
    tail -n 60 "${log_file}" >&2 || true
    fail "${name} did not become ready"
  fi

  log "${name} is ready"
}

configure_frontend_env() {
  local name="$1"
  case "${name}" in
    storefront-web)
      export NEXT_PUBLIC_API_BASE_URL="http://localhost:${GATEWAY_PORT}"
      export API_BASE_URL="http://localhost:${GATEWAY_PORT}"
      ;;
    admin-web)
      export VITE_API_BASE_URL="http://localhost:${GATEWAY_PORT}"
      export VITE_COMMERCE_API_BASE_URL="http://localhost:${GATEWAY_PORT}"
      ;;
  esac
}

start_frontend() {
  local name="$1"
  local relative_dir="$2"
  local ready_url="$3"
  local port="$4"
  local pid_file
  local log_file

  cleanup_stale_pid "${name}"
  pid_file="$(pid_file_for "${name}")"
  log_file="$(log_file_for "${name}")"

  if url_responds "${ready_url}"; then
    log "${name} is already responding on port ${port}; leaving the existing process untouched"
    return 0
  fi

  if [[ ! -f "${pid_file}" ]] && port_in_use "${port}"; then
    fail "Port ${port} is already in use for ${name}. Stop the conflicting process before running the local stack."
  fi

  if [[ ! -d "${REPO_ROOT}/${relative_dir}/node_modules" ]]; then
    log "Installing npm dependencies for ${name}"
    (
      cd "${REPO_ROOT}/${relative_dir}"
      npm install
    )
  fi

  log "Starting ${name} on port ${port}"
  (
    configure_frontend_env "${name}"
    start_detached_command \
      "${name}" \
      "${REPO_ROOT}/${relative_dir}" \
      "npm run dev" \
      "${log_file}" \
      "${pid_file}"
  )

  if ! wait_for_url "${ready_url}" "${name}" 180 2; then
    warn "Recent ${name} log output:"
    tail -n 60 "${log_file}" >&2 || true
    fail "${name} did not become ready"
  fi

  log "${name} is ready"
}

rebuild_search_projection() {
  local rebuild_url="http://localhost:${SEARCH_SERVICE_PORT}/internal/search/index/products/rebuild"
  log "Rebuilding the search projection from the local sample catalog"
  if ! curl -fsS \
    -X POST \
    -H "X-Internal-Api-Key: ${LOCAL_INTERNAL_API_KEY}" \
    "${rebuild_url}" >/dev/null; then
    warn "Search rebuild failed. The platform is still running, but /api/v1/search may stay empty."
  fi
}

print_summary() {
  cat <<EOF

Local NOURA stack is ready.

URLs
- Storefront: http://localhost:${STOREFRONT_PORT}
- Admin: http://localhost:${ADMIN_WEB_PORT}
- API gateway: http://localhost:${GATEWAY_PORT}
- Gateway health: http://localhost:${GATEWAY_PORT}/actuator/health
- Keycloak: http://localhost:${KEYCLOAK_PORT}

Runtime files
- Logs: ${LOCAL_LOG_DIR}
- Process state: ${LOCAL_PID_DIR}

Stop the managed processes with:
- ./platform/scripts/stop-local.sh
- ./platform/scripts/stop-local.sh --down-infra
EOF
}

start_infrastructure
"${LOCAL_SCRIPT_DIR}/bootstrap-local-db.sh"

for service_entry in "${java_services[@]}"; do
  name="${service_entry%%|*}"
  remainder="${service_entry#*|}"
  relative_dir="${remainder%%|*}"
  port="${remainder##*|}"
  start_java_service "${name}" "${relative_dir}" "${port}"
done

rebuild_search_projection
start_frontend "storefront-web" "apps/storefront-web" "http://localhost:${STOREFRONT_PORT}" "${STOREFRONT_PORT}"
start_frontend "admin-web" "apps/admin-web" "http://localhost:${ADMIN_WEB_PORT}" "${ADMIN_WEB_PORT}"
print_summary
