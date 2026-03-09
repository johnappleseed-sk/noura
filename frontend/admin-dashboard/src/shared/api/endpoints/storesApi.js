import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listStores(params = {}) {
  const response = await commerceApiClient.get('/stores', { params })
  return unwrapApiResponse(response.data)
}

export async function createStore(payload) {
  const response = await commerceApiClient.post('/stores', payload)
  return unwrapApiResponse(response.data)
}

export async function updateStore(storeId, payload) {
  const response = await commerceApiClient.put(`/stores/${storeId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteStore(storeId) {
  const response = await commerceApiClient.delete(`/stores/${storeId}`)
  return unwrapApiResponse(response.data)
}
