export const ROLES = {
  ADMIN: 'ADMIN',
  WAREHOUSE_MANAGER: 'WAREHOUSE_MANAGER',
  VIEWER: 'VIEWER'
}

export const INVENTORY_PORTAL_ROLES = [ROLES.ADMIN, ROLES.WAREHOUSE_MANAGER, ROLES.VIEWER]
export const MANAGER_ROLES = [ROLES.ADMIN, ROLES.WAREHOUSE_MANAGER]
export const ADMIN_ROLES = [ROLES.ADMIN]

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
