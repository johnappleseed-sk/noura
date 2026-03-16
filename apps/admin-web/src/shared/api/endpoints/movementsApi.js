import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listMovements(params = {}) {
  const response = await inventoryApiClient.get('/movements', { params })
  return unwrapApiResponse(response.data)
}

export async function receiveInbound(payload) {
  const response = await inventoryApiClient.post('/movements/inbound', payload)
  return unwrapApiResponse(response.data)
}

export async function shipOutbound(payload) {
  const response = await inventoryApiClient.post('/movements/outbound', payload)
  return unwrapApiResponse(response.data)
}

export async function transferStock(payload) {
  const response = await inventoryApiClient.post('/movements/transfers', payload)
  return unwrapApiResponse(response.data)
}

export async function adjustStock(payload) {
  const response = await inventoryApiClient.post('/movements/adjustments', payload)
  return unwrapApiResponse(response.data)
}

export async function returnStock(payload) {
  const response = await inventoryApiClient.post('/movements/returns', payload)
  return unwrapApiResponse(response.data)
}
