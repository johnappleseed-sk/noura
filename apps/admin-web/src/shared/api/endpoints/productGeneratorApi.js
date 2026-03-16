import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function generateProduct(payload = {}) {
  const response = await commerceApiClient.post('/admin/product-generator/generate', payload)
  return unwrapApiResponse(response.data)
}

export async function searchExistingProducts(query) {
  const response = await commerceApiClient.get('/products/search', {
    params: { q: query }
  })
  return unwrapApiResponse(response.data)
}

export async function getExistingProduct(productId) {
  const response = await commerceApiClient.get(`/products/${productId}`)
  return unwrapApiResponse(response.data)
}

export async function generateMissingFields(productId) {
  const response = await commerceApiClient.post(`/products/${productId}/generate-missing`)
  return unwrapApiResponse(response.data)
}

export async function generateProductDescription(productId) {
  const response = await commerceApiClient.post(`/products/${productId}/generate-description`)
  return unwrapApiResponse(response.data)
}

export async function generateProductBarcode(productId) {
  const response = await commerceApiClient.post(`/products/${productId}/generate-barcode`)
  return unwrapApiResponse(response.data)
}

export async function generateProductQr(productId) {
  const response = await commerceApiClient.post(`/products/${productId}/generate-qr`)
  return unwrapApiResponse(response.data)
}

export async function fetchProductBarcodeImage(productId) {
  const response = await commerceApiClient.get(`/products/${productId}/barcode-image`, {
    responseType: 'blob'
  })
  return URL.createObjectURL(response.data)
}

export async function fetchProductQrImage(productId) {
  const response = await commerceApiClient.get(`/products/${productId}/qr-image`, {
    responseType: 'blob'
  })
  return URL.createObjectURL(response.data)
}
