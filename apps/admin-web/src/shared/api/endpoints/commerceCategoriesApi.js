import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getCommerceCategoryTree(locale = 'en', config = {}) {
  const response = await commerceApiClient.get('/categories/tree', {
    ...config,
    params: locale ? { ...(config.params || {}), locale } : config.params
  })
  return unwrapApiResponse(response.data)
}

export async function createCommerceCategory(payload) {
  const response = await commerceApiClient.post('/categories', payload)
  return unwrapApiResponse(response.data)
}

export async function updateCommerceCategory(categoryId, payload) {
  const response = await commerceApiClient.put(`/categories/${categoryId}`, payload)
  return unwrapApiResponse(response.data)
}
