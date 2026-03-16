import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listCommerceProducts(params = {}, config = {}) {
  const response = await commerceApiClient.get('/products', { ...config, params })
  return unwrapApiResponse(response.data)
}

export async function getCommerceProduct(productId, config = {}) {
  const response = await commerceApiClient.get(`/products/${productId}`, config)
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

export async function updateCommerceMedia(productId, mediaId, payload) {
  const response = await commerceApiClient.put(`/products/${productId}/media/${mediaId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteCommerceMedia(productId, mediaId) {
  const response = await commerceApiClient.delete(`/products/${productId}/media/${mediaId}`)
  return unwrapApiResponse(response.data)
}

export async function listCommerceProductReviews(productId, config = {}) {
  const response = await commerceApiClient.get(`/products/${productId}/reviews`, config)
  return unwrapApiResponse(response.data)
}

export async function addCommerceProductReview(productId, payload) {
  const response = await commerceApiClient.post(`/products/${productId}/reviews`, payload)
  return unwrapApiResponse(response.data)
}

export async function upsertCommerceStoreInventory(productId, payload) {
  const response = await commerceApiClient.put(`/products/${productId}/inventory`, payload)
  return unwrapApiResponse(response.data)
}

export async function listCommerceInventories(productId, config = {}) {
  const response = await commerceApiClient.get(`/products/${productId}/inventory`, config)
  return unwrapApiResponse(response.data)
}
