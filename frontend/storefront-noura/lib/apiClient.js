const rawBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  process.env.API_BASE_URL ||
  'http://localhost:8080'

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

const apiHost = resolveApiHost(rawBaseUrl) || 'http://localhost:8080'

const commerceBase = normalizeBaseUrl(
  process.env.NEXT_PUBLIC_COMMERCE_API_BASE_URL ||
    process.env.COMMERCE_API_BASE_URL ||
    apiHost,
  '/api/v1'
)

function buildHeaders(token, initHeaders = {}) {
  const headers = {
    Accept: 'application/json',
    ...initHeaders
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

async function unwrapEnvelope(response) {
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : { message: await response.text() }

  if (!response.ok) {
    throw new Error(
      payload?.error?.detail ||
        payload?.message ||
        payload?.code ||
        `Request failed with status ${response.status}`
    )
  }

  if (!payload?.success) {
    throw new Error(payload?.error?.detail || payload?.message || 'API request failed')
  }

  return payload.data
}

function createClient(baseUrl) {
  return {
    baseUrl,
    async request(path, options = {}) {
      const response = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers: buildHeaders(null, options.headers || {}),
        next: options.next ?? { revalidate: 60 }
      })

      return unwrapEnvelope(response)
    },
    async requestWithAuth(path, token, options = {}) {
      const response = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers: buildHeaders(token, {
          ...(options.headers || {}),
          ...(options.body ? { 'Content-Type': 'application/json' } : {})
        })
      })

      return unwrapEnvelope(response)
    }
  }
}

export const commerceApiClient = createClient(commerceBase)
