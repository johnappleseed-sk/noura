import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listRecoveryRecords(params = {}) {
  const response = await commerceApiClient.get('/admin/recovery/records', { params })
  return unwrapApiResponse(response.data)
}

export async function listRecoveryVersions(entityType, entityId) {
  const response = await commerceApiClient.get(`/admin/recovery/records/${entityType}/${entityId}/versions`)
  return unwrapApiResponse(response.data)
}

export async function listRecoveryAuditLogs(params = {}) {
  const response = await commerceApiClient.get('/admin/recovery/audit-logs', { params })
  return unwrapApiResponse(response.data)
}

export async function listRecoveryJobs(params = {}) {
  const response = await commerceApiClient.get('/admin/recovery/jobs', { params })
  return unwrapApiResponse(response.data)
}

export async function applyRecoveryAction(payload) {
  const response = await commerceApiClient.post('/admin/recovery/actions', payload)
  return unwrapApiResponse(response.data)
}

export async function submitRecoveryBulkAction(payload) {
  const response = await commerceApiClient.post('/admin/recovery/bulk-actions', payload)
  return unwrapApiResponse(response.data)
}

export async function listRecoveryApprovalRequests(params = {}) {
  const response = await commerceApiClient.get('/admin/recovery/approval-requests', { params })
  return unwrapApiResponse(response.data)
}

export async function requestRecoveryActionApproval(payload) {
  const response = await commerceApiClient.post('/admin/recovery/approval-requests', payload)
  return unwrapApiResponse(response.data)
}

export async function requestRecoveryBulkApproval(payload) {
  const response = await commerceApiClient.post('/admin/recovery/approval-requests/bulk', payload)
  return unwrapApiResponse(response.data)
}

export async function approveRecoveryApproval(approvalId, payload = {}) {
  const response = await commerceApiClient.post(`/admin/recovery/approval-requests/${approvalId}/approve`, payload)
  return unwrapApiResponse(response.data)
}

export async function rejectRecoveryApproval(approvalId, payload = {}) {
  const response = await commerceApiClient.post(`/admin/recovery/approval-requests/${approvalId}/reject`, payload)
  return unwrapApiResponse(response.data)
}

export async function cancelRecoveryJob(jobId) {
  const response = await commerceApiClient.post(`/admin/recovery/jobs/${jobId}/cancel`)
  return unwrapApiResponse(response.data)
}

export async function retryRecoveryJob(jobId, failedOnly = true) {
  const response = await commerceApiClient.post(`/admin/recovery/jobs/${jobId}/retry`, null, {
    params: { failedOnly }
  })
  return unwrapApiResponse(response.data)
}

export async function downloadRecoveryFailureReport(jobId) {
  const response = await commerceApiClient.get(`/admin/recovery/jobs/${jobId}/failure-report`, {
    responseType: 'text'
  })
  return response.data
}
