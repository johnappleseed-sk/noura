import { unwrapApiResponse } from '../apiResult'
import { inventoryApiClient } from '../httpClient'

export async function getStockValuationReport(params = {}) {
  const response = await inventoryApiClient.get('/reports/stock-valuation', { params })
  return unwrapApiResponse(response.data)
}

export async function getLowStockReport(params = {}) {
  const response = await inventoryApiClient.get('/reports/low-stock', { params })
  return unwrapApiResponse(response.data)
}

export async function getTurnoverReport(params = {}) {
  const response = await inventoryApiClient.get('/reports/turnover', { params })
  return unwrapApiResponse(response.data)
}

export async function getMovementHistory(params = {}) {
  const response = await inventoryApiClient.get('/reports/movement-history', { params })
  return unwrapApiResponse(response.data)
}

export async function exportReportCsv(params = {}) {
  const response = await inventoryApiClient.get('/reports/export', {
    params,
    responseType: 'blob'
  })
  return response.data
}

export async function listBatchLots(params = {}) {
  const response = await inventoryApiClient.get('/batches', { params })
  return unwrapApiResponse(response.data)
}

export async function getBatchLot(batchId) {
  const response = await inventoryApiClient.get(`/batches/${batchId}`)
  return unwrapApiResponse(response.data)
}

export async function listSerialNumbers(params = {}) {
  const response = await inventoryApiClient.get('/serials', { params })
  return unwrapApiResponse(response.data)
}

export async function getSerialNumber(serialId) {
  const response = await inventoryApiClient.get(`/serials/${serialId}`)
  return unwrapApiResponse(response.data)
}

export async function getBarcodeAsset(resourceType, resourceId, params = {}) {
  const normalized = String(resourceType || '').trim().toLowerCase()
  const path =
    normalized === 'products'
      ? `/barcodes/products/${resourceId}`
      : normalized === 'batches'
        ? `/barcodes/batches/${resourceId}`
        : normalized === 'bins'
          ? `/barcodes/bins/${resourceId}`
          : null
  if (!path) {
    throw new Error('Unsupported barcode resource type.')
  }

  const response = await inventoryApiClient.get(path, {
    params,
    responseType: 'blob'
  })
  return response.data
}
