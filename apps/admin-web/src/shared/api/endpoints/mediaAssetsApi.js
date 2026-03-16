import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function uploadMediaAsset(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await commerceApiClient.post('/media-assets/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: options.onUploadProgress,
    signal: options.signal
  })
  return unwrapApiResponse(response.data)
}

export async function importMediaAssetFromUrl(url, options = {}) {
  const response = await commerceApiClient.post('/media-assets/import-url', { url }, {
    signal: options.signal
  })
  return unwrapApiResponse(response.data)
}
