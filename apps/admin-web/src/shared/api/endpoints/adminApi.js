import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listApprovalQueue() {
  const response = await commerceApiClient.get('/admin/b2b/approvals')
  return unwrapApiResponse(response.data)
}

export async function updateApproval(approvalId, payload) {
  const response = await commerceApiClient.patch(`/admin/b2b/approvals/${approvalId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function listAdminUsers(params = {}) {
  const response = await commerceApiClient.get('/admin/users', { params })
  return unwrapApiResponse(response.data)
}

export async function updateAdminUser(userId, payload) {
  const response = await commerceApiClient.patch(`/admin/users/${userId}`, payload)
  return unwrapApiResponse(response.data)
}
