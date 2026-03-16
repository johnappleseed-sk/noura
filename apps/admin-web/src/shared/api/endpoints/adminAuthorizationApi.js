/**
 * Module: Admin Authorization API
 * Purpose: API client helpers for admin RBAC policy endpoints.
 * Responsibilities:
 * - Fetch matrix and catalog endpoints used by governance UIs.
 * - Execute role CRUD, preset, and permission assignment workflows.
 * - Execute single and bulk user-role assignment workflows.
 * Related modules:
 * - pages/RolesPermissionsPage.jsx
 * - backend AdminDashboardController (/admin/authorization/matrix)
 * - backend AdminAuthorizationController (/admin/authorization/*)
 */

import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

/**
 * Fetches the versioned role-permission matrix payload.
 *
 * @returns {Promise<object>} Matrix payload with `actionCatalog`, `scopes`, and `roles`.
 */
export async function getAdminAuthorizationMatrix() {
  const response = await commerceApiClient.get('/admin/authorization/matrix')
  return unwrapApiResponse(response.data)
}

/**
 * Lists persisted permission catalog entries.
 *
 * @returns {Promise<Array>} Permission records.
 */
export async function listAdminPermissions() {
  const response = await commerceApiClient.get('/admin/authorization/permissions')
  return unwrapApiResponse(response.data)
}

/**
 * Lists reusable permission presets for role grant workflows.
 *
 * @returns {Promise<Array>} Permission preset records.
 */
export async function listAdminPermissionPresets() {
  const response = await commerceApiClient.get('/admin/authorization/permission-presets')
  return unwrapApiResponse(response.data)
}

/**
 * Lists persisted admin roles with grants and capabilities.
 *
 * @returns {Promise<Array>} Role records.
 */
export async function listAdminRoles() {
  const response = await commerceApiClient.get('/admin/authorization/roles')
  return unwrapApiResponse(response.data)
}

/**
 * Creates a new admin role.
 *
 * @param {object} payload Role creation payload.
 * @returns {Promise<object>} Created role.
 */
export async function createAdminRole(payload) {
  const response = await commerceApiClient.post('/admin/authorization/roles', payload)
  return unwrapApiResponse(response.data)
}

/**
 * Updates mutable role metadata.
 *
 * @param {string} roleId Target role id.
 * @param {object} payload Role update payload.
 * @returns {Promise<object>} Updated role.
 */
export async function updateAdminRole(roleId, payload) {
  const response = await commerceApiClient.patch(`/admin/authorization/roles/${roleId}`, payload)
  return unwrapApiResponse(response.data)
}

/**
 * Deactivates a role.
 *
 * @param {string} roleId Target role id.
 * @returns {Promise<void>}
 */
export async function deactivateAdminRole(roleId) {
  const response = await commerceApiClient.delete(`/admin/authorization/roles/${roleId}`)
  return unwrapApiResponse(response.data)
}

/**
 * Replaces all role grants.
 *
 * @param {string} roleId Target role id.
 * @param {object} payload Permission assignment payload.
 * @returns {Promise<object>} Updated role.
 */
export async function replaceAdminRolePermissions(roleId, payload) {
  const response = await commerceApiClient.put(`/admin/authorization/roles/${roleId}/permissions`, payload)
  return unwrapApiResponse(response.data)
}

/**
 * Applies a preset onto the target role and replaces grants.
 *
 * @param {string} roleId Target role id.
 * @param {string} presetCode Preset code.
 * @returns {Promise<object>} Updated role.
 */
export async function applyAdminRolePermissionPreset(roleId, presetCode) {
  const response = await commerceApiClient.put(
    `/admin/authorization/roles/${roleId}/permission-presets/${encodeURIComponent(presetCode)}`
  )
  return unwrapApiResponse(response.data)
}

/**
 * Reads admin role assignments for a target user.
 *
 * @param {string} userId Target user id.
 * @returns {Promise<object>} User assignment payload.
 */
export async function getAdminUserRoleAssignments(userId) {
  const response = await commerceApiClient.get(`/admin/authorization/users/${userId}/roles`)
  return unwrapApiResponse(response.data)
}

/**
 * Replaces admin role assignments for a target user.
 *
 * @param {string} userId Target user id.
 * @param {object} payload User role assignment payload.
 * @returns {Promise<object>} Updated user assignment payload.
 */
export async function replaceAdminUserRoleAssignments(userId, payload) {
  const response = await commerceApiClient.put(`/admin/authorization/users/${userId}/roles`, payload)
  return unwrapApiResponse(response.data)
}

/**
 * Replaces role assignments for multiple users in one request.
 *
 * @param {object} payload Bulk user role assignment payload.
 * @returns {Promise<object>} Bulk assignment summary.
 */
export async function bulkReplaceAdminUserRoleAssignments(payload) {
  const response = await commerceApiClient.put('/admin/authorization/users/roles/bulk', payload)
  return unwrapApiResponse(response.data)
}

/**
 * Previews role-assignment deltas for a bulk assignment request.
 *
 * @param {object} payload Bulk user role assignment payload.
 * @returns {Promise<object>} Preview summary with per-user changes.
 */
export async function previewBulkAdminUserRoleAssignments(payload) {
  const response = await commerceApiClient.post('/admin/authorization/users/roles/bulk/preview', payload)
  return unwrapApiResponse(response.data)
}

/**
 * Lists saved bulk-assignment views for the current admin actor.
 *
 * @returns {Promise<Array>} Saved view records.
 */
export async function listAdminBulkUserRoleViews() {
  const response = await commerceApiClient.get('/admin/authorization/users/roles/bulk/views')
  return unwrapApiResponse(response.data)
}

/**
 * Creates or updates a saved bulk-assignment view.
 *
 * @param {object} payload Saved-view payload.
 * @returns {Promise<object>} Persisted saved view.
 */
export async function upsertAdminBulkUserRoleView(payload) {
  const response = await commerceApiClient.post('/admin/authorization/users/roles/bulk/views', payload)
  return unwrapApiResponse(response.data)
}

/**
 * Deletes a saved bulk-assignment view by id.
 *
 * @param {string} viewId Saved view id.
 * @returns {Promise<void>}
 */
export async function deleteAdminBulkUserRoleView(viewId) {
  const response = await commerceApiClient.delete(`/admin/authorization/users/roles/bulk/views/${viewId}`)
  return unwrapApiResponse(response.data)
}

/**
 * Lists RBAC governance audit logs.
 *
 * @param {object} params Optional filtering and pagination parameters.
 * @returns {Promise<object>} Paged audit-log payload.
 */
export async function listAdminAuthorizationAuditLogs(params = {}) {
  const response = await commerceApiClient.get('/admin/authorization/audit-logs', { params })
  return unwrapApiResponse(response.data)
}

/**
 * Exports RBAC governance audit logs as CSV.
 *
 * @param {object} params Optional filter parameters.
 * @returns {Promise<Blob>} CSV file blob.
 */
export async function exportAdminAuthorizationAuditLogsCsv(params = {}) {
  const response = await commerceApiClient.get('/admin/authorization/audit-logs/export', {
    params,
    responseType: 'blob'
  })
  return response.data
}
