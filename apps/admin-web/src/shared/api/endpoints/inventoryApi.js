import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listStockLevels(params = {}) {
  const response = await inventoryApiClient.get('/stock-levels', { params })
  return unwrapApiResponse(response.data)
}
