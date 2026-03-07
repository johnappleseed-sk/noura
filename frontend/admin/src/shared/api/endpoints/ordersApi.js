import { api } from './api'

// === Orders API ===

export async function listOrders(params = {}) {
  const searchParams = new URLSearchParams()
  if (params.status) searchParams.set('status', params.status)
  if (params.customerId) searchParams.set('customerId', params.customerId)
  if (params.q) searchParams.set('q', params.q)
  if (params.page !== undefined) searchParams.set('page', params.page)
  if (params.size !== undefined) searchParams.set('size', params.size)
  if (params.sort) searchParams.set('sort', params.sort)
  if (params.dir) searchParams.set('dir', params.dir)
  if (params.fromDate) searchParams.set('fromDate', params.fromDate)
  if (params.toDate) searchParams.set('toDate', params.toDate)

  return api.get(`/api/admin/orders?${searchParams}`)
}

export async function getOrder(id) {
  return api.get(`/api/admin/orders/${id}`)
}

export async function updateOrderStatus(id, status) {
  return api.post(`/api/admin/orders/${id}/status`, { status })
}

export async function cancelOrder(id, reason) {
  return api.post(`/api/admin/orders/${id}/cancel`, { reason })
}

export async function markShipped(id, data) {
  return api.post(`/api/admin/orders/${id}/ship`, data)
}

export async function markDelivered(id) {
  return api.post(`/api/admin/orders/${id}/deliver`, {})
}

export async function addOrderNote(id, note) {
  return api.post(`/api/admin/orders/${id}/notes`, { note })
}

export async function getOrderTimeline(id) {
  return api.get(`/api/admin/orders/${id}/timeline`)
}

export async function refundOrder(id, data) {
  return api.post(`/api/admin/orders/${id}/refund`, data)
}
