import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listAdminMerchants(params = {}, config = {}) {
  const response = await commerceApiClient.get('/admin/merchants', {
    ...config,
    params
  })
  return unwrapApiResponse(response.data)
}

export async function getAdminMerchant(merchantId, config = {}) {
  const response = await commerceApiClient.get(`/admin/merchants/${merchantId}`, config)
  return unwrapApiResponse(response.data)
}

export async function createAdminMerchant(payload, config = {}) {
  const response = await commerceApiClient.post('/admin/merchants', payload, config)
  return unwrapApiResponse(response.data)
}

export async function updateAdminMerchantStatus(merchantId, payload, config = {}) {
  const response = await commerceApiClient.patch(`/admin/merchants/${merchantId}/status`, payload, config)
  return unwrapApiResponse(response.data)
}
