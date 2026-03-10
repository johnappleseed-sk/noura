import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function listCarousels(params = {}) {
  const response = await commerceApiClient.get('/admin/carousels', { params })
  return unwrapApiResponse(response.data)
}

export async function getCarousel(carouselId, params = { includeDeleted: true }) {
  const response = await commerceApiClient.get(`/admin/carousels/${carouselId}`, { params })
  return unwrapApiResponse(response.data)
}

export async function createCarousel(payload) {
  const response = await commerceApiClient.post('/admin/carousels', payload)
  return unwrapApiResponse(response.data)
}

export async function updateCarousel(carouselId, payload) {
  const response = await commerceApiClient.put(`/admin/carousels/${carouselId}`, payload)
  return unwrapApiResponse(response.data)
}

export async function deleteCarousel(carouselId) {
  const response = await commerceApiClient.delete(`/admin/carousels/${carouselId}`)
  return unwrapApiResponse(response.data)
}

export async function restoreCarousel(carouselId) {
  const response = await commerceApiClient.post(`/admin/carousels/${carouselId}/restore`)
  return unwrapApiResponse(response.data)
}

export async function updateCarouselStatus(carouselId, status) {
  const response = await commerceApiClient.patch(`/admin/carousels/${carouselId}/status`, { status })
  return unwrapApiResponse(response.data)
}

export async function publishCarousel(carouselId, payload) {
  const response = await commerceApiClient.patch(`/admin/carousels/${carouselId}/publish`, payload)
  return unwrapApiResponse(response.data)
}

export async function reorderCarousels(items) {
  const response = await commerceApiClient.patch('/admin/carousels/reorder', { items })
  return unwrapApiResponse(response.data)
}

export async function duplicateCarousel(carouselId) {
  const response = await commerceApiClient.post(`/admin/carousels/${carouselId}/duplicate`)
  return unwrapApiResponse(response.data)
}

export async function bulkCarouselAction(payload) {
  const response = await commerceApiClient.post('/admin/carousels/bulk-action', payload)
  return unwrapApiResponse(response.data)
}

export async function getCarouselPreview(carouselId) {
  const response = await commerceApiClient.get(`/admin/carousels/${carouselId}/preview`)
  return unwrapApiResponse(response.data)
}
