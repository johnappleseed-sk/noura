import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function generateProduct(payload = {}) {
  const response = await commerceApiClient.post('/admin/product-generator/generate', payload)
  return unwrapApiResponse(response.data)
}
