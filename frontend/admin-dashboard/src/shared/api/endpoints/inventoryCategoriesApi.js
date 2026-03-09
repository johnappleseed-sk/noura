import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listCategories(params = {}) {
  const response = await inventoryApiClient.get('/categories', { params })
  return unwrapApiResponse(response.data)
}

export async function getCategoryTree(activeOnly = true) {
  const response = await inventoryApiClient.get('/categories/tree', {
    params: { activeOnly }
  })
  return unwrapApiResponse(response.data)
}

export async function createCategory(payload) {
  const response = await inventoryApiClient.post('/categories', payload)
  return unwrapApiResponse(response.data)
}

export async function updateCategory(categoryId, payload) {
  const response = await inventoryApiClient.put(`/categories/${categoryId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteCategory(categoryId) {
  const response = await inventoryApiClient.delete(`/categories/${categoryId}`)
  return unwrapApiResponse(response.data)
}
