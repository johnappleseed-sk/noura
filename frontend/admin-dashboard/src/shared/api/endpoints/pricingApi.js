import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listPriceLists() {
  const response = await commerceApiClient.get('/price-lists')
  return unwrapApiResponse(response.data)
}

export async function createPriceList(payload) {
  const response = await commerceApiClient.post('/price-lists', payload)
  return unwrapApiResponse(response.data)
}

export async function upsertPrice(payload) {
  const response = await commerceApiClient.post('/prices', payload)
  return unwrapApiResponse(response.data)
}

export async function quoteVariantPrice(variantId, params = {}) {
  const response = await commerceApiClient.get(`/prices/variants/${variantId}`, { params })
  return unwrapApiResponse(response.data)
}

export async function listActivePromotions() {
  const response = await commerceApiClient.get('/promotions/active')
  return unwrapApiResponse(response.data)
}

export async function createPromotion(payload) {
  const response = await commerceApiClient.post('/promotions', payload)
  return unwrapApiResponse(response.data)
}
