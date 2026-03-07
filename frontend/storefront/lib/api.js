const API_BASE_URL =
  process.env.API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  'http://localhost:8080'

const CUSTOMER_TOKEN_KEY = 'noura_customer_access_token'

function resolveTokenFromWindow() {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage.getItem(CUSTOMER_TOKEN_KEY)
}

export function resolveCustomerToken() {
  return resolveTokenFromWindow()
}

export function persistCustomerToken(token) {
  if (typeof window !== 'undefined' && token) {
    window.localStorage.setItem(CUSTOMER_TOKEN_KEY, token)
  }
}

export function clearCustomerToken() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(CUSTOMER_TOKEN_KEY)
  }
}

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

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json'
    },
    next: {
      revalidate: 60
    }
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  const envelope = await response.json()
  if (!envelope?.success) {
    throw new Error(envelope?.message || 'API request failed')
  }

  return envelope.data
}

async function requestWithAuth(path, token, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: buildHeaders(token, {
      ...(options.headers || {}),
      ...(options.body ? { 'Content-Type': 'application/json' } : {})
    })
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  const envelope = await response.json()
  if (!envelope?.success) {
    throw new Error(envelope?.message || 'API request failed')
  }

  return envelope.data
}

export async function getCategories() {
  return request('/api/storefront/v1/catalog/categories')
}

export async function getProducts({ q, categoryId, page = 0, size = 12, sort = 'featured' } = {}) {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  if (categoryId) params.set('categoryId', categoryId)
  params.set('page', String(page))
  params.set('size', String(size))
  params.set('sort', sort)
  return request(`/api/storefront/v1/catalog/products?${params.toString()}`)
}

export async function getProduct(productId) {
  return request(`/api/storefront/v1/catalog/products/${productId}`)
}

export async function getAvailability(productId) {
  return request(`/api/storefront/v1/catalog/products/${productId}/availability`)
}

export async function registerCustomer(payload) {
  return requestWithAuth('/api/storefront/v1/customers/register', null, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function loginCustomer(payload) {
  return requestWithAuth('/api/storefront/v1/customers/login', null, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function getCustomerMe(token) {
  return requestWithAuth('/api/storefront/v1/customers/me', token)
}

export async function getCustomerAddresses(token) {
  return requestWithAuth('/api/storefront/v1/customers/me/addresses', token)
}

export async function addCustomerAddress(token, payload) {
  return requestWithAuth('/api/storefront/v1/customers/me/addresses', token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteCustomerAddress(token, addressId) {
  return requestWithAuth(`/api/storefront/v1/customers/me/addresses/${addressId}`, token, {
    method: 'DELETE'
  })
}

export async function getCart(token) {
  return requestWithAuth('/api/storefront/v1/cart', token)
}

export async function addCartItem(token, payload) {
  return requestWithAuth('/api/storefront/v1/cart/items', token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function updateCartItem(token, itemId, payload) {
  return requestWithAuth(`/api/storefront/v1/cart/items/${itemId}`, token, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

export async function removeCartItem(token, itemId) {
  return requestWithAuth(`/api/storefront/v1/cart/items/${itemId}`, token, {
    method: 'DELETE'
  })
}

export async function clearCart(token) {
  return requestWithAuth('/api/storefront/v1/cart', token, {
    method: 'DELETE'
  })
}

export async function checkoutCart(token, body) {
  return requestWithAuth('/api/storefront/v1/orders/checkout', token, {
    method: 'POST',
    body: JSON.stringify(body || {})
  })
}

export async function getOrderPayments(token, orderId) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}/payments`, token)
}

export async function createOrderPayment(token, orderId, body) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}/payments`, token, {
    method: 'POST',
    body: JSON.stringify(body || {})
  })
}

export async function captureOrderPayment(token, orderId, paymentId) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}/payments/${paymentId}/capture`, token, {
    method: 'POST'
  })
}

export async function getMyOrders(token) {
  return requestWithAuth('/api/storefront/v1/orders/me', token)
}

export async function getOrder(token, orderId) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}`, token)
}

export async function cancelOrder(token, orderId) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}/cancel`, token, {
    method: 'POST'
  })
}

export async function getOrderFulfillment(token, orderId) {
  return requestWithAuth(`/api/storefront/v1/orders/${orderId}/fulfillment`, token)
}
