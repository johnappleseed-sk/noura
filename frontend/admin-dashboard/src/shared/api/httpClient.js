import axios from 'axios'
import { clearAuthSnapshot, getAccessToken } from '../auth/tokenStorage'

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function normalizeBaseUrl(base, suffix) {
  if (!base) return suffix
  const trimmed = base.replace(/\/+$/, '')
  if (trimmed.endsWith(suffix)) {
    return trimmed
  }
  return `${trimmed}${suffix}`
}

function resolveApiHost(baseUrl) {
  return (baseUrl || '')
    .replace(/\/api\/inventory\/v1\/?$/, '')
    .replace(/\/api\/v1\/?$/, '')
    .replace(/\/+$/, '')
}

export const apiHost = resolveApiHost(rawBaseUrl) || 'http://localhost:8080'
const commerceBase = normalizeBaseUrl(
  import.meta.env.VITE_COMMERCE_API_BASE_URL || apiHost,
  '/api/v1'
)
const inventoryBase = normalizeBaseUrl(
  import.meta.env.VITE_INVENTORY_API_BASE_URL || apiHost,
  '/api/inventory/v1'
)

function clampPageSize(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return value
  }
  return Math.min(Math.max(numeric, 1), 100)
}

function normalizeParams(params) {
  if (!params || typeof params !== 'object' || params instanceof URLSearchParams) {
    return params
  }

  const next = { ...params }

  if (next.size !== undefined) {
    next.size = clampPageSize(next.size)
  }

  if (next['users.size'] !== undefined) {
    next['users.size'] = clampPageSize(next['users.size'])
  }

  if (next.users && typeof next.users === 'object' && !Array.isArray(next.users)) {
    next.users = {
      ...next.users,
      size: next.users.size !== undefined ? clampPageSize(next.users.size) : next.users.size
    }
  }

  return next
}

function attachInterceptors(client) {
  client.interceptors.request.use((config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['X-Requested-With'] = 'XMLHttpRequest'
    if (config.params) {
      config.params = normalizeParams(config.params)
    }
    return config
  })

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error?.response?.status === 401) {
        clearAuthSnapshot()
      }
      const serverMessage =
        error?.response?.data?.error?.detail ||
        error?.response?.data?.message ||
        error?.message
      error.message = serverMessage || 'Request failed.'
      throw error
    }
  )

  return client
}

export const commerceApiClient = attachInterceptors(
  axios.create({
    baseURL: commerceBase,
    timeout: 20000
  })
)

export const inventoryApiClient = attachInterceptors(
  axios.create({
    baseURL: inventoryBase,
    timeout: 20000
  })
)

// Backwards-compatible alias; avoid using in new code.
export const httpClient = commerceApiClient
