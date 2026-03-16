import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listWebhookSubscriptions() {
  const response = await inventoryApiClient.get('/webhooks')
  return unwrapApiResponse(response.data)
}

export async function getWebhookSubscription(subscriptionId) {
  const response = await inventoryApiClient.get(`/webhooks/${subscriptionId}`)
  return unwrapApiResponse(response.data)
}

export async function createWebhookSubscription(payload) {
  const response = await inventoryApiClient.post('/webhooks', payload)
  return unwrapApiResponse(response.data)
}

export async function updateWebhookSubscription(subscriptionId, payload) {
  const response = await inventoryApiClient.put(`/webhooks/${subscriptionId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteWebhookSubscription(subscriptionId) {
  const response = await inventoryApiClient.delete(`/webhooks/${subscriptionId}`)
  return unwrapApiResponse(response.data)
}
