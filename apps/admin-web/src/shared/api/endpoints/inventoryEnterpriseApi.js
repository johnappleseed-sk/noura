import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listEnterpriseWarehouses() {
  const response = await commerceApiClient.get('/admin/inventory/warehouses')
  return unwrapApiResponse(response.data)
}

export async function listInventoryTransfers() {
  const response = await commerceApiClient.get('/admin/inventory/transfers')
  return unwrapApiResponse(response.data)
}

export async function createInventoryTransfer(payload) {
  const response = await commerceApiClient.post('/admin/inventory/transfers', payload)
  return unwrapApiResponse(response.data)
}

export async function listRestockSchedules() {
  const response = await commerceApiClient.get('/admin/inventory/restock-schedules')
  return unwrapApiResponse(response.data)
}

export async function createRestockSchedule(payload) {
  const response = await commerceApiClient.post('/admin/inventory/restock-schedules', payload)
  return unwrapApiResponse(response.data)
}

export async function listLowStockAlerts() {
  const response = await commerceApiClient.get('/admin/inventory/alerts/low-stock')
  return unwrapApiResponse(response.data)
}

export async function listInventoryReservations() {
  const response = await commerceApiClient.get('/admin/inventory/reservations')
  return unwrapApiResponse(response.data)
}
