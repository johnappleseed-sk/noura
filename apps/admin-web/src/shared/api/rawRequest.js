import { getAccessToken } from '../auth/tokenStorage'
import { apiHost } from './httpClient'

function appendSearchParams(url, payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return
  }

  const clampPageSize = (value) => {
    const numeric = Number(value)
    if (!Number.isFinite(numeric)) {
      return value
    }
    return Math.min(Math.max(numeric, 1), 100)
  }

  const maybeClamp = (key, value) => {
    if (key === 'size' || key === 'users.size') {
      return clampPageSize(value)
    }
    return value
  }

  Object.entries(payload).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    if (Array.isArray(value)) {
      value.forEach((entry) => {
        if (entry !== undefined && entry !== null && entry !== '') {
          url.searchParams.append(key, String(maybeClamp(key, entry)))
        }
      })
      return
    }

    url.searchParams.set(key, String(maybeClamp(key, value)))
  })
}

function toFormBody(payload) {
  const params = new URLSearchParams()

  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return params.toString()
  }

  Object.entries(payload).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    if (Array.isArray(value)) {
      value.forEach((entry) => {
        if (entry !== undefined && entry !== null && entry !== '') {
          params.append(key, String(entry))
        }
      })
      return
    }

    params.append(key, typeof value === 'object' ? JSON.stringify(value) : String(value))
  })

  return params.toString()
}

export async function executeRawApiRequest({
  method,
  path,
  query,
  body,
  bodyMode = 'json',
  withAuth = true
}) {
  const url = new URL(path, apiHost)
  appendSearchParams(url, query)

  const headers = {
    Accept: 'application/json'
  }

  if (withAuth) {
    const token = getAccessToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  let requestBody
  if (bodyMode === 'json' && body !== undefined) {
    headers['Content-Type'] = 'application/json'
    requestBody = JSON.stringify(body)
  }

  if (bodyMode === 'form' && body !== undefined) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'
    requestBody = toFormBody(body)
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: requestBody
  })

  const contentType = response.headers.get('content-type') || ''
  let data = null

  if (response.status !== 204) {
    if (contentType.includes('application/json')) {
      data = await response.json()
    } else {
      data = await response.text()
    }
  }

  return {
    ok: response.ok,
    status: response.status,
    statusText: response.statusText,
    contentType,
    data
  }
}
