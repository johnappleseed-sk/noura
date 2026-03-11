export const ROLES = {
  ADMIN: 'ADMIN',
  WAREHOUSE_MANAGER: 'WAREHOUSE_MANAGER',
  VIEWER: 'VIEWER'
}

export const INVENTORY_PORTAL_ROLES = [ROLES.ADMIN, ROLES.WAREHOUSE_MANAGER, ROLES.VIEWER]
export const MANAGER_ROLES = [ROLES.ADMIN, ROLES.WAREHOUSE_MANAGER]
export const ADMIN_ROLES = [ROLES.ADMIN]

export const CAPABILITIES = {
  OVERVIEW_DASHBOARD: 'overview.dashboard',
  OVERVIEW_ANALYTICS: 'overview.analytics',
  COMMERCE_CATALOG: 'commerce.catalog',
  COMMERCE_CAROUSELS: 'commerce.carousels',
  COMMERCE_RECOMMENDATIONS: 'commerce.recommendations',
  COMMERCE_MERCHANDISING: 'commerce.merchandising',
  COMMERCE_ORDERS: 'commerce.orders',
  COMMERCE_RETURNS: 'commerce.returns',
  COMMERCE_STORES: 'commerce.stores',
  COMMERCE_PRICING: 'commerce.pricing',
  COMMERCE_USERS: 'commerce.users',
  COMMERCE_NOTIFICATIONS: 'commerce.notifications',
  WAREHOUSE_CATALOG: 'warehouse.catalog',
  WAREHOUSE_LOCATIONS: 'warehouse.locations',
  WAREHOUSE_STOCK: 'warehouse.stock',
  WAREHOUSE_STOCK_ADJUST: 'warehouse.stock.adjust',
  WAREHOUSE_MOVEMENTS: 'warehouse.movements',
  WAREHOUSE_BATCHES: 'warehouse.batches',
  WAREHOUSE_SERIALS: 'warehouse.serials',
  WAREHOUSE_REPORTS: 'warehouse.reports',
  WAREHOUSE_WEBHOOKS: 'warehouse.webhooks',
  WAREHOUSE_AUDIT_LOGS: 'warehouse.auditLogs',
  TOOLS_CONTROL_CENTER: 'tools.controlCenter',
  TOOLS_PRODUCT_GENERATOR: 'tools.productGenerator'
}

function buildCapabilitiesFromRoles(normalizedRoles) {
  const roleSet = new Set(normalizedRoles)
  const isAdmin = roleSet.has(ROLES.ADMIN)
  const isWarehouseManager = roleSet.has(ROLES.WAREHOUSE_MANAGER)
  const isViewer = roleSet.has(ROLES.VIEWER)
  const canAccessWarehouse = isAdmin || isWarehouseManager || isViewer

  return {
    [CAPABILITIES.OVERVIEW_DASHBOARD]: canAccessWarehouse,
    [CAPABILITIES.OVERVIEW_ANALYTICS]: isAdmin,
    [CAPABILITIES.COMMERCE_CATALOG]: isAdmin,
    [CAPABILITIES.COMMERCE_CAROUSELS]: isAdmin,
    [CAPABILITIES.COMMERCE_RECOMMENDATIONS]: isAdmin,
    [CAPABILITIES.COMMERCE_MERCHANDISING]: isAdmin,
    [CAPABILITIES.COMMERCE_ORDERS]: isAdmin,
    [CAPABILITIES.COMMERCE_RETURNS]: isAdmin,
    [CAPABILITIES.COMMERCE_STORES]: isAdmin,
    [CAPABILITIES.COMMERCE_PRICING]: isAdmin,
    [CAPABILITIES.COMMERCE_USERS]: isAdmin,
    [CAPABILITIES.COMMERCE_NOTIFICATIONS]: isAdmin,
    [CAPABILITIES.WAREHOUSE_CATALOG]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_LOCATIONS]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_STOCK]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_STOCK_ADJUST]: isAdmin || isWarehouseManager,
    [CAPABILITIES.WAREHOUSE_MOVEMENTS]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_BATCHES]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_SERIALS]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_REPORTS]: canAccessWarehouse,
    [CAPABILITIES.WAREHOUSE_WEBHOOKS]: isAdmin,
    [CAPABILITIES.WAREHOUSE_AUDIT_LOGS]: isAdmin,
    [CAPABILITIES.TOOLS_CONTROL_CENTER]: isAdmin,
    [CAPABILITIES.TOOLS_PRODUCT_GENERATOR]: isAdmin
  }
}

export function normalizeRole(role) {
  if (!role) return null
  return role.startsWith('ROLE_') ? role.slice(5) : role
}

export function normalizeRoles(roles) {
  if (!Array.isArray(roles)) {
    return roles ? [normalizeRole(roles)] : []
  }
  return roles.map(normalizeRole).filter(Boolean)
}

export function hasAnyRole(currentRoles, allowedRoles) {
  if (!allowedRoles?.length) {
    return true
  }
  const normalizedCurrent = new Set(normalizeRoles(currentRoles))
  return allowedRoles.some((role) => normalizedCurrent.has(normalizeRole(role)))
}

export function hasRole(currentRoles, role) {
  return hasAnyRole(currentRoles, [role])
}

export function deriveCapabilities(currentRoles) {
  return buildCapabilitiesFromRoles(normalizeRoles(currentRoles))
}

export function hasCapability(auth, capability) {
  if (!capability) {
    return true
  }
  const explicit = auth?.capabilities?.[capability]
  if (typeof explicit === 'boolean') {
    return explicit
  }
  const fallback = deriveCapabilities(auth?.roles || [])
  return Boolean(fallback[capability])
}
