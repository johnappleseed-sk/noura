import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function login(payload) {
  const response = await commerceApiClient.post('/auth/login', {
    email: payload.email || payload.login || payload.username,
    password: payload.password
  })
  return unwrapApiResponse(response.data)
}

export async function registerUser(payload) {
  const response = await commerceApiClient.post('/auth/register', {
    email: payload.email,
    password: payload.password,
    fullName: payload.fullName
  })
  return unwrapApiResponse(response.data)
}

export async function getCurrentUser() {
  const response = await commerceApiClient.get('/account/profile')
  return unwrapApiResponse(response.data)
}

export async function refreshSession(payload) {
  const refreshToken = typeof payload === 'string' ? payload : payload?.refreshToken
  const response = await commerceApiClient.post('/auth/refresh', { refreshToken })
  return unwrapApiResponse(response.data)
}

// Backward-compatible exports.
export const loginPassword = login
export const me = getCurrentUser
