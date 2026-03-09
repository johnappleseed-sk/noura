import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listCommerceProducts(params = {}) {
  const response = await commerceApiClient.get('/products', { params })
  return unwrapApiResponse(response.data)
}

export async function getCommerceProduct(productId) {
  const response = await commerceApiClient.get(`/products/${productId}`)
  return unwrapApiResponse(response.data)
}

export async function createCommerceProduct(payload) {
  const response = await commerceApiClient.post('/products', payload)
  return unwrapApiResponse(response.data)
}

export async function updateCommerceProduct(productId, payload) {
  const response = await commerceApiClient.put(`/products/${productId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function patchCommerceProduct(productId, payload) {
  const response = await commerceApiClient.patch(`/products/${productId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteCommerceProduct(productId) {
  const response = await commerceApiClient.delete(`/products/${productId}`)
  return unwrapApiResponse(response.data)
}

export async function addCommerceVariant(productId, payload) {
  const response = await commerceApiClient.post(`/products/${productId}/variants`, payload)
  return unwrapApiResponse(response.data)
}

export async function updateCommerceVariant(variantId, payload) {
  const response = await commerceApiClient.put(`/variants/${variantId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function addCommerceMedia(productId, payload) {
  const response = await commerceApiClient.post(`/products/${productId}/media`, payload)
  return unwrapApiResponse(response.data)
}

export async function upsertCommerceStoreInventory(productId, payload) {
  const response = await commerceApiClient.put(`/products/${productId}/inventory`, payload)
  return unwrapApiResponse(response.data)
}

export async function listCommerceInventories(productId) {
  const response = await commerceApiClient.get(`/products/${productId}/inventory`)
  return unwrapApiResponse(response.data)
}
