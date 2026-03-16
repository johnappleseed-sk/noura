import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listAdminProductSubmissions(params = {}, config = {}) {
  const response = await commerceApiClient.get('/admin/product-submissions', {
    ...config,
    params
  })
  return unwrapApiResponse(response.data)
}

export async function getAdminProductSubmission(submissionId, config = {}) {
  const response = await commerceApiClient.get(`/admin/product-submissions/${submissionId}`, config)
  return unwrapApiResponse(response.data)
}

export async function approveProductSubmission(submissionId, payload, config = {}) {
  const response = await commerceApiClient.post(`/admin/product-submissions/${submissionId}/approve`, payload, config)
  return unwrapApiResponse(response.data)
}

export async function rejectProductSubmission(submissionId, payload, config = {}) {
  const response = await commerceApiClient.post(`/admin/product-submissions/${submissionId}/reject`, payload, config)
  return unwrapApiResponse(response.data)
}
