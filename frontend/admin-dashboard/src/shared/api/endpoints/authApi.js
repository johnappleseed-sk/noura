import { unwrapApiResponse } from '../apiResult'
import { commerceApiClient } from '../httpClient'

export async function loginPassword(payload) {
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

export async function me() {
  const response = await commerceApiClient.get('/account/profile')
  return unwrapApiResponse(response.data)
}
