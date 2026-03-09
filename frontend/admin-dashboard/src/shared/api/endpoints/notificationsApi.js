import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listMyNotifications() {
  const response = await commerceApiClient.get('/notifications/me')
  return unwrapApiResponse(response.data)
}

export async function getUnreadCount() {
  const response = await commerceApiClient.get('/notifications/me/unread-count')
  return unwrapApiResponse(response.data)
}

export async function markNotificationRead(notificationId) {
  const response = await commerceApiClient.patch(`/notifications/${notificationId}/read`)
  return unwrapApiResponse(response.data)
}

export async function markAllNotificationsRead() {
  const response = await commerceApiClient.patch('/notifications/me/read-all')
  return unwrapApiResponse(response.data)
}

export async function pushNotificationToUser(userId, payload) {
  const response = await commerceApiClient.post(`/notifications/user/${userId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function broadcastNotification(payload) {
  const response = await commerceApiClient.post('/notifications/broadcast', payload)
  return unwrapApiResponse(response.data)
}
