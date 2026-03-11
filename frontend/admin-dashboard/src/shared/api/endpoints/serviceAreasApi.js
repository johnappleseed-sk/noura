import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listServiceAreas(params = {}) {
  const response = await commerceApiClient.get('/admin/service-areas', { params })
  return unwrapApiResponse(response.data)
}

export async function getServiceArea(serviceAreaId) {
  const response = await commerceApiClient.get(`/admin/service-areas/${serviceAreaId}`)
  return unwrapApiResponse(response.data)
}

export async function createServiceArea(payload) {
  const response = await commerceApiClient.post('/admin/service-areas', payload)
  return unwrapApiResponse(response.data)
}

export async function updateServiceArea(serviceAreaId, payload) {
  const response = await commerceApiClient.put(`/admin/service-areas/${serviceAreaId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteServiceArea(serviceAreaId) {
  const response = await commerceApiClient.delete(`/admin/service-areas/${serviceAreaId}`)
  return unwrapApiResponse(response.data)
}

export async function activateServiceArea(serviceAreaId) {
  const response = await commerceApiClient.post(`/admin/service-areas/${serviceAreaId}/activate`)
  return unwrapApiResponse(response.data)
}

export async function deactivateServiceArea(serviceAreaId) {
  const response = await commerceApiClient.post(`/admin/service-areas/${serviceAreaId}/deactivate`)
  return unwrapApiResponse(response.data)
}

export async function validateServiceAreaRules(payload) {
  const response = await commerceApiClient.post('/admin/service-areas/validate', payload)
  return unwrapApiResponse(response.data)
}
