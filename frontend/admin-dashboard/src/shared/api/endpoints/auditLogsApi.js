import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listAuditLogs(params = {}) {
  const response = await inventoryApiClient.get('/audit-logs', { params })
  return unwrapApiResponse(response.data)
}
