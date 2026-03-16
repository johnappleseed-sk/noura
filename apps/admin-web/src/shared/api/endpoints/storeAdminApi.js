import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listAdminStores(params = {}, config = {}) {
  const response = await commerceApiClient.get('/admin/stores', {
    ...config,
    params
  })
  return unwrapApiResponse(response.data)
}

export async function getAdminStore(storeId, config = {}) {
  const response = await commerceApiClient.get(`/admin/stores/${storeId}`, config)
  return unwrapApiResponse(response.data)
}

export async function createAdminStore(payload, config = {}) {
  const response = await commerceApiClient.post('/admin/stores', payload, config)
  return unwrapApiResponse(response.data)
}

export async function updateAdminStoreStatus(storeId, payload, config = {}) {
  const response = await commerceApiClient.patch(`/admin/stores/${storeId}/status`, payload, config)
  return unwrapApiResponse(response.data)
}
