import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getCommerceCategoryTree(locale = 'en') {
  const response = await commerceApiClient.get('/categories/tree', {
    params: locale ? { locale } : undefined
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
