import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getCategoryAnalytics(params = {}) {
  const response = await commerceApiClient.get('/categories/analytics', { params })
  return unwrapApiResponse(response.data)
}

export async function getCommerceAnalyticsOverview(params = {}) {
  const response = await commerceApiClient.get('/admin/analytics/overview', { params })
  return unwrapApiResponse(response.data)
}
