import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getAdminCapabilities() {
  const response = await commerceApiClient.get('/admin/capabilities')
  return unwrapApiResponse(response.data)
}
