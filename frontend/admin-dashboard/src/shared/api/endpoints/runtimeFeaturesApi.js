import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getRuntimeFeatures() {
  const response = await commerceApiClient.get('/runtime/features')
  return unwrapApiResponse(response.data)
}

