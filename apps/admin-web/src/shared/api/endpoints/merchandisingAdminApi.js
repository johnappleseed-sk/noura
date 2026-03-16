import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function getMerchandisingSettings() {
  const response = await commerceApiClient.get('/admin/merchandising/settings')
  return unwrapApiResponse(response.data)
}

export async function updateMerchandisingSettings(payload) {
  const response = await commerceApiClient.put('/admin/merchandising/settings', payload)
  return unwrapApiResponse(response.data)
}

export async function getMerchandisingPreview(params = {}) {
  const response = await commerceApiClient.get('/admin/merchandising/preview', { params })
  return unwrapApiResponse(response.data)
}

export async function listMerchandisingBoosts() {
  const response = await commerceApiClient.get('/admin/merchandising/boosts')
  return unwrapApiResponse(response.data)
}

export async function createMerchandisingBoost(payload) {
  const response = await commerceApiClient.post('/admin/merchandising/boosts', payload)
  return unwrapApiResponse(response.data)
}

export async function updateMerchandisingBoost(boostId, payload) {
  const response = await commerceApiClient.put(`/admin/merchandising/boosts/${boostId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteMerchandisingBoost(boostId) {
  const response = await commerceApiClient.delete(`/admin/merchandising/boosts/${boostId}`)
  return unwrapApiResponse(response.data)
}
