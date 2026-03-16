/**
 * Module: Admin Authorization Utilities
 * Purpose: Central role and capability constants with fallback derivation for route/action guards.
 * Responsibilities:
 * - Normalize backend roles.
 * - Derive deterministic capabilities when runtime capability payload is unavailable.
 * - Provide shared role/capability helpers used by router, layout, and feature pages.
 * Related modules:
 * - features/auth/AuthProvider.jsx
 * - app/router.jsx
 * - app/navigation.js
 */

export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  STORE_MANAGER: 'STORE_MANAGER',
  MANAGER: 'MANAGER',
  CONTENT_MANAGER: 'CONTENT_MANAGER',
  PRODUCT_MANAGER: 'PRODUCT_MANAGER',
  INVENTORY_MANAGER: 'INVENTORY_MANAGER',
  ORDER_MANAGER: 'ORDER_MANAGER',
  SUPPORT_AGENT: 'SUPPORT_AGENT',
  CUSTOMER_SUPPORT: 'CUSTOMER_SUPPORT',
  FINANCE: 'FINANCE',
  FINANCE_OFFICER: 'FINANCE_OFFICER',
  MARKETING_MANAGER: 'MARKETING_MANAGER',
  ANALYST: 'ANALYST',
  STAFF: 'STAFF',
  WAREHOUSE_MANAGER: 'WAREHOUSE_MANAGER',
  VIEWER: 'VIEWER',
  CUSTOMER: 'CUSTOMER',
  B2B: 'B2B'
}

export const INVENTORY_PORTAL_ROLES = [
  ROLES.SUPER_ADMIN,
  ROLES.ADMIN,
  ROLES.STORE_MANAGER,
  ROLES.MANAGER,
  ROLES.CONTENT_MANAGER,
  ROLES.PRODUCT_MANAGER,
  ROLES.INVENTORY_MANAGER,
  ROLES.ORDER_MANAGER,
  ROLES.SUPPORT_AGENT,
  ROLES.CUSTOMER_SUPPORT,
  ROLES.FINANCE,
  ROLES.FINANCE_OFFICER,
  ROLES.MARKETING_MANAGER,
  ROLES.ANALYST,
  ROLES.STAFF,
  ROLES.WAREHOUSE_MANAGER,
  ROLES.VIEWER
]
export const MANAGER_ROLES = [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.STORE_MANAGER, ROLES.MANAGER, ROLES.WAREHOUSE_MANAGER, ROLES.INVENTORY_MANAGER]
export const ADMIN_ROLES = [ROLES.SUPER_ADMIN, ROLES.ADMIN]

export const CAPABILITIES = {
  OVERVIEW_DASHBOARD: 'overview.dashboard',
  OVERVIEW_ANALYTICS: 'overview.analytics',
  NETWORK_MERCHANTS: 'network.merchants',
  NETWORK_STORES: 'network.stores',
  COMMERCE_CATALOG: 'commerce.catalog',
  GOVERNANCE_PRODUCT_SUBMISSIONS: 'governance.productSubmissions',
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
  GOVERNANCE_RECOVERY: 'governance.recovery',
  GOVERNANCE_RECOVERY_PURGE: 'governance.recovery.purge',
  GOVERNANCE_RBAC: 'governance.rbac',
  TOOLS_CONTROL_CENTER: 'tools.controlCenter',
  TOOLS_PRODUCT_GENERATOR: 'tools.productGenerator'
}

function buildCapabilitiesFromRoles(normalizedRoles) {
  const roleSet = new Set(normalizedRoles)
  const isSuperAdmin = roleSet.has(ROLES.SUPER_ADMIN)
  const isAdmin = isSuperAdmin || roleSet.has(ROLES.ADMIN)
  const isStoreManager = roleSet.has(ROLES.STORE_MANAGER)
  const isManager = isStoreManager || roleSet.has(ROLES.MANAGER)
  const isCatalogManager = roleSet.has(ROLES.PRODUCT_MANAGER) || roleSet.has(ROLES.CONTENT_MANAGER)
  const isWarehouseManager = roleSet.has(ROLES.WAREHOUSE_MANAGER) || roleSet.has(ROLES.INVENTORY_MANAGER)
  const isOrderManager = roleSet.has(ROLES.ORDER_MANAGER) || roleSet.has(ROLES.SUPPORT_AGENT) || roleSet.has(ROLES.CUSTOMER_SUPPORT)
  const isFinance = roleSet.has(ROLES.FINANCE) || roleSet.has(ROLES.FINANCE_OFFICER)
  const isMarketingManager = roleSet.has(ROLES.MARKETING_MANAGER)
  const isAnalyst = roleSet.has(ROLES.ANALYST)
  const isViewer = roleSet.has(ROLES.VIEWER)
  const isStaff = roleSet.has(ROLES.STAFF)

  const canAccessDashboard =
    isAdmin ||
    isManager ||
    isCatalogManager ||
    isWarehouseManager ||
    isOrderManager ||
    isFinance ||
    isAnalyst ||
    isViewer ||
    isStaff
  const canAccessAnalytics = isAdmin || isManager || isFinance || isAnalyst
  const canAccessCommerceCatalog = isAdmin || isManager || isCatalogManager
  const canAccessMarketing = isAdmin || isManager || isCatalogManager || isMarketingManager
  const canAccessCommerceOrders = isAdmin || isManager || isOrderManager
  const canAccessMerchantNetwork = isAdmin || isManager || isWarehouseManager
  const canAccessWarehouse = isAdmin || isManager || isWarehouseManager || isAnalyst || isViewer || isStaff

  return {
    [CAPABILITIES.OVERVIEW_DASHBOARD]: canAccessDashboard,
    [CAPABILITIES.OVERVIEW_ANALYTICS]: canAccessAnalytics,
    [CAPABILITIES.NETWORK_MERCHANTS]: canAccessMerchantNetwork,
    [CAPABILITIES.NETWORK_STORES]: canAccessMerchantNetwork,
    [CAPABILITIES.COMMERCE_CATALOG]: canAccessCommerceCatalog,
    [CAPABILITIES.GOVERNANCE_PRODUCT_SUBMISSIONS]: isAdmin || isCatalogManager,
    [CAPABILITIES.COMMERCE_CAROUSELS]: canAccessMarketing,
    [CAPABILITIES.COMMERCE_RECOMMENDATIONS]: canAccessMarketing,
    [CAPABILITIES.COMMERCE_MERCHANDISING]: canAccessMarketing,
    [CAPABILITIES.COMMERCE_ORDERS]: canAccessCommerceOrders,
    [CAPABILITIES.COMMERCE_RETURNS]: canAccessCommerceOrders,
    [CAPABILITIES.COMMERCE_STORES]: isAdmin || isManager || isWarehouseManager,
    [CAPABILITIES.COMMERCE_PRICING]: isAdmin || isManager || isFinance,
    [CAPABILITIES.COMMERCE_USERS]: isAdmin || isManager,
    [CAPABILITIES.COMMERCE_NOTIFICATIONS]: canAccessCommerceOrders,
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
    [CAPABILITIES.GOVERNANCE_RECOVERY]: isAdmin,
    [CAPABILITIES.GOVERNANCE_RECOVERY_PURGE]: isAdmin,
    [CAPABILITIES.GOVERNANCE_RBAC]: isAdmin,
    [CAPABILITIES.TOOLS_CONTROL_CENTER]: isAdmin || isManager,
    [CAPABILITIES.TOOLS_PRODUCT_GENERATOR]: canAccessCommerceCatalog
  }
}

export function normalizeRole(role) {
  if (!role) return null
  const raw = String(role)
  const normalized = raw.startsWith('ROLE_') ? raw.slice(5) : raw
  return normalized.toUpperCase()
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
