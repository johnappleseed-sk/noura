import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listOrders(params = {}) {
  const response = await commerceApiClient.get('/orders', { params })
  return unwrapApiResponse(response.data)
}

export async function getOrder(orderId) {
  const response = await commerceApiClient.get(`/orders/${encodeURIComponent(orderId)}`)
  return unwrapApiResponse(response.data)
}

export async function getOrderTimeline(orderId) {
  const response = await commerceApiClient.get(`/orders/${encodeURIComponent(orderId)}/timeline`)
  return unwrapApiResponse(response.data)
}

export async function updateOrderStatus(orderId, payload) {
  const response = await commerceApiClient.patch(`/orders/${encodeURIComponent(orderId)}/status`, payload)
  return unwrapApiResponse(response.data)
}
