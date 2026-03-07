import { api } from './api'

// === Returns API ===

export async function listReturns(params = {}) {
  const searchParams = new URLSearchParams()
  if (params.status) searchParams.set('status', params.status)
  if (params.customerId) searchParams.set('customerId', params.customerId)
  if (params.page !== undefined) searchParams.set('page', params.page)
  if (params.size !== undefined) searchParams.set('size', params.size)
  if (params.sort) searchParams.set('sort', params.sort)
  if (params.dir) searchParams.set('dir', params.dir)

  return api.get(`/api/admin/returns?${searchParams}`)
}

export async function getReturn(id) {
  return api.get(`/api/admin/returns/${id}`)
}

export async function approveReturn(id, data = {}) {
  return api.post(`/api/admin/returns/${id}/approve`, data)
}

export async function rejectReturn(id, data = {}) {
  return api.post(`/api/admin/returns/${id}/reject`, data)
}

export async function receiveReturn(id, data = {}) {
  return api.post(`/api/admin/returns/${id}/receive`, data)
}

export async function processRefund(id, data = {}) {
  return api.post(`/api/admin/returns/${id}/refund`, data)
}

export async function completeReturn(id, data = {}) {
  return api.post(`/api/admin/returns/${id}/complete`, data)
}

export async function addReturnNote(id, note) {
  return api.post(`/api/admin/returns/${id}/notes`, { note })
}
