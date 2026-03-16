import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function listWarehouses(params = {}) {
  const response = await inventoryApiClient.get('/warehouses', { params })
  return unwrapApiResponse(response.data)
}

export async function createWarehouse(payload) {
  const response = await inventoryApiClient.post('/warehouses', payload)
  return unwrapApiResponse(response.data)
}

export async function updateWarehouse(warehouseId, payload) {
  const response = await inventoryApiClient.put(`/warehouses/${warehouseId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteWarehouse(warehouseId) {
  const response = await inventoryApiClient.delete(`/warehouses/${warehouseId}`)
  return unwrapApiResponse(response.data)
}

export async function listWarehouseBins(warehouseId, params = {}) {
  const response = await inventoryApiClient.get(`/warehouses/${warehouseId}/bins`, { params })
  return unwrapApiResponse(response.data)
}

export async function listBins(params = {}) {
  const response = await inventoryApiClient.get('/bins', { params })
  return unwrapApiResponse(response.data)
}

export async function createWarehouseBin(warehouseId, payload) {
  const response = await inventoryApiClient.post(`/warehouses/${warehouseId}/bins`, payload)
  return unwrapApiResponse(response.data)
}

export async function updateWarehouseBin(binId, payload) {
  const response = await inventoryApiClient.put(`/bins/${binId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteWarehouseBin(binId) {
  const response = await inventoryApiClient.delete(`/bins/${binId}`)
  return unwrapApiResponse(response.data)
}
