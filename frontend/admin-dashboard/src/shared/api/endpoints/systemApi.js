import { inventoryApiClient } from '../httpClient'

export async function getInventorySystemStatus() {
  const response = await inventoryApiClient.get('/system/status')
  return response.data
}
