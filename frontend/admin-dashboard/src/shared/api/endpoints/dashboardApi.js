import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getDashboardSummary() {
  const response = await commerceApiClient.get('/admin/dashboard/summary')
  return unwrapApiResponse(response.data)
}
