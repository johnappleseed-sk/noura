import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listProducts(params = {}) {
  const response = await inventoryApiClient.get('/products', { params })
  return unwrapApiResponse(response.data)
}

export async function createProduct(payload) {
  const response = await inventoryApiClient.post('/products', payload)
  return unwrapApiResponse(response.data)
}

export async function updateProduct(productId, payload) {
  const response = await inventoryApiClient.put(`/products/${productId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteProduct(productId) {
  const response = await inventoryApiClient.delete(`/products/${productId}`)
  return unwrapApiResponse(response.data)
}
