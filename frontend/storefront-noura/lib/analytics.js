'use client'

const baseUrl = (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const endpoint = `${baseUrl}/api/v1/analytics/events`
const storageKey = 'noura.analytics.session'
const customerTokenKey = 'noura_customer_access_token'

function getSessionId() {
  if (typeof window === 'undefined') return null
  try {
    const existing = window.localStorage.getItem(storageKey)
    if (existing) return existing
    const created = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`
    window.localStorage.setItem(storageKey, created)
    return created
  } catch {
    return null
  }
}

function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const decoded = typeof window !== 'undefined' ? window.atob(normalized) : null
    return decoded ? JSON.parse(decoded) : null
  } catch {
    return null
  }
}

function getCustomerRef() {
  if (typeof window === 'undefined') return null

  try {
    const token = window.localStorage.getItem(customerTokenKey)
    const payload = decodeJwtPayload(token)
    return payload?.sub || payload?.email || payload?.username || null
  } catch {
    return null
  }
}

export async function trackAnalyticsEvent(payload = {}) {
  try {
    await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: getSessionId(),
        customerRef: getCustomerRef(),
        pagePath: typeof window !== 'undefined' ? window.location.pathname : null,
        source: 'storefront-web',
        occurredAt: new Date().toISOString(),
        ...payload
      }),
      keepalive: true
    })
  } catch {
    // Analytics tracking must never interrupt commerce flows.
  }
}
