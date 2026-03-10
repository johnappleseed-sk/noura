import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getRecommendationSettings() {
  const response = await commerceApiClient.get('/admin/recommendations/settings')
  return unwrapApiResponse(response.data)
}

export async function updateRecommendationSettings(payload) {
  const response = await commerceApiClient.put('/admin/recommendations/settings', payload)
  return unwrapApiResponse(response.data)
}

export async function getRecommendationPreview(params = {}) {
  const response = await commerceApiClient.get('/admin/recommendations/preview', { params })
  return unwrapApiResponse(response.data)
}
