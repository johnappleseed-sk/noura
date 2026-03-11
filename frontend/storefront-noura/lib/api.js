import { commerceApiClient } from './apiClient'

const request = commerceApiClient.request
const requestWithAuth = commerceApiClient.requestWithAuth
const ACTIVE_API_PREFIX = ''
const CUSTOMER_TOKEN_KEY = 'noura_customer_access_token'
const UNSUPPORTED_PROFILE_MESSAGE =
  'This backend profile does not expose storefront returns, fulfillment tracking, or post-checkout payment APIs.'

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

function splitFullName(fullName) {
  const safeName = typeof fullName === 'string' ? fullName.trim() : ''
  if (!safeName) {
    return {
      firstName: '',
      lastName: ''
    }
  }

  const parts = safeName.split(/\s+/)
  return {
    firstName: parts[0] || '',
    lastName: parts.slice(1).join(' ')
  }
}

function buildFullName(payload) {
  const firstName = payload?.firstName?.trim() || ''
  const lastName = payload?.lastName?.trim() || ''
  const fullName = payload?.fullName?.trim() || [firstName, lastName].filter(Boolean).join(' ').trim()

  if (fullName) {
    return fullName
  }

  return payload?.email?.split('@')[0] || 'Customer'
}

function normalizeCategoryTree(nodes, bucket = []) {
  for (const node of Array.isArray(nodes) ? nodes : []) {
    bucket.push({
      id: node.id,
      name: node.name,
      description: node.description || '',
      imageUrl: null,
      sortOrder: bucket.length,
      productCount: 0
    })

    if (Array.isArray(node.children) && node.children.length > 0) {
      normalizeCategoryTree(node.children, bucket)
    }
  }

  return bucket
}

function pickPrimaryVariant(product) {
  if (!Array.isArray(product?.variants) || product.variants.length === 0) {
    return null
  }

  return product.variants.find((variant) => variant?.active) || product.variants[0]
}

function pickPrimaryMedia(product) {
  if (!Array.isArray(product?.media) || product.media.length === 0) {
    return null
  }

  return product.media.find((asset) => asset?.primary) || product.media[0]
}

function sumInventoryStock(product) {
  return (product?.storeInventory || []).reduce((total, item) => total + Number(item?.stock || 0), 0)
}

function normalizeProductCard(product) {
  const primaryVariant = pickPrimaryVariant(product)
  const primaryMedia = pickPrimaryMedia(product)
  const stockQty = typeof product?.stockQty === 'number' ? product.stockQty : sumInventoryStock(product)

  return {
    id: product.id,
    sku: primaryVariant?.sku || null,
    name: product.name,
    price: product.price || 0,
    compareAtPrice: product.compareAtPrice || null,
    imageUrl: product.imageUrl || primaryMedia?.url || null,
    categoryId: product.categoryId || null,
    categoryName: product.categoryName || product.category || null,
    stockQty,
    lowStock: Boolean(product.lowStock ?? (stockQty > 0 && stockQty <= 5)),
    allowNegativeStock: Boolean(product.allowNegativeStock ?? product.allowBackorder),
    isNew: Boolean(product.isNew),
    isTrending: Boolean(product.isTrending),
    isBestseller: Boolean(product.isBestseller),
    merchandisingScore: product.merchandisingScore || 0
  }
}

function normalizeRecommendationCard(product) {
  return {
    id: product?.id,
    sku: product?.sku || null,
    name: product?.name || '',
    price: product?.price || 0,
    imageUrl: product?.imageUrl || null,
    categoryId: product?.categoryId || null,
    categoryName: product?.categoryName || null,
    stockQty: product?.stockQty || 0,
    lowStock: Boolean(product?.lowStock),
    allowNegativeStock: Boolean(product?.allowNegativeStock),
    reason: product?.reason || null,
    score: product?.score || 0
  }
}

function normalizeProductDetail(product) {
  const primaryVariant = pickPrimaryVariant(product)
  const primaryMedia = pickPrimaryMedia(product)
  const stockQty = sumInventoryStock(product)

  return {
    id: product.id,
    sku: primaryVariant?.sku || null,
    name: product.name,
    price: product.price || 0,
    imageUrl: primaryMedia?.url || null,
    categoryName: product.category || null,
    barcode: null,
    baseUnitName: 'piece',
    weightValue: null,
    weightUnit: null,
    lengthValue: null,
    lengthUnit: null,
    widthValue: null,
    widthUnit: null,
    heightValue: null,
    heightUnit: null,
    lowStockThreshold: null,
    boxSpecifications: product.shortDescription || null,
    units: (product.variants || []).map((variant) => ({
      id: variant.id,
      name: [variant.color, variant.size].filter(Boolean).join(' / ') || variant.sku || 'Variant',
      abbreviation: variant.sku || null,
      conversionToBase: 1
    })),
    active: Boolean(product.active),
    allowNegativeStock: Boolean(product.allowBackorder),
    lowStock: stockQty > 0 && stockQty <= 5,
    stockQty
  }
}

function normalizeAvailability(inventory) {
  const stockQty = (Array.isArray(inventory) ? inventory : []).reduce(
    (total, item) => total + Number(item?.stock || 0),
    0
  )

  return {
    active: true,
    allowNegativeStock: false,
    lowStock: stockQty > 0 && stockQty <= 5,
    stockQty
  }
}

function normalizeProfile(profile) {
  const name = splitFullName(profile?.fullName)

  return {
    id: profile?.id,
    email: profile?.email || '',
    firstName: name.firstName,
    lastName: name.lastName,
    phone: profile?.phone || '',
    status: profile?.enabled === false ? 'DISABLED' : 'ACTIVE',
    emailVerified: null,
    marketingOptIn: null
  }
}

function normalizeAddress(address) {
  return {
    id: address?.id,
    label: address?.label || '',
    recipientName: address?.fullName || '',
    fullName: address?.fullName || '',
    phone: address?.phone || '',
    line1: address?.line1 || '',
    line2: address?.line2 || '',
    district: address?.district || '',
    city: address?.city || '',
    stateProvince: address?.state || '',
    postalCode: address?.zipCode || '',
    countryCode: address?.country || '',
    latitude: address?.latitude ?? null,
    longitude: address?.longitude ?? null,
    accuracyMeters: address?.accuracyMeters ?? null,
    placeId: address?.placeId || null,
    formattedAddress: address?.formattedAddress || '',
    deliveryInstructions: address?.deliveryInstructions || '',
    validationStatus: address?.validationStatus || 'UNVERIFIED',
    defaultShipping: Boolean(address?.defaultAddress),
    defaultBilling: Boolean(address?.defaultAddress)
  }
}

function normalizeCart(cart) {
  const items = Array.isArray(cart?.items)
    ? cart.items.map((item) => ({
        id: item.id,
        productId: item.productId,
        sku: null,
        productName: item.productName,
        unitLabel: 'item',
        quantity: item.quantity || 0,
        unitPrice: item.unitPrice || 0,
        lineTotal: item.lineTotal || 0
      }))
    : []

  return {
    id: cart?.cartId,
    storeId: cart?.storeId || null,
    addressId: cart?.addressId || null,
    status: 'ACTIVE',
    currencyCode: 'USD',
    items,
    subtotal: cart?.totals?.subtotal || 0,
    totalItems: items.reduce((total, item) => total + Number(item.quantity || 0), 0),
    updatedAt: null
  }
}

function normalizeGeocodeResult(result) {
  if (!result) {
    return null
  }

  return {
    latitude: result?.latitude ?? null,
    longitude: result?.longitude ?? null,
    formattedAddress: result?.formattedAddress || '',
    country: result?.country || '',
    region: result?.region || '',
    city: result?.city || '',
    district: result?.district || '',
    postalCode: result?.postalCode || '',
    placeId: result?.placeId || ''
  }
}

function normalizeServiceEligibility(eligibility) {
  if (!eligibility) {
    return null
  }

  const isServiceAvailable = Boolean(eligibility?.serviceAvailable)
  return {
    isServiceAvailable,
    serviceAvailable: isServiceAvailable,
    serviceType: eligibility?.serviceType || null,
    matchedServiceAreaId: eligibility?.matchedServiceAreaId || null,
    matchedStoreId: eligibility?.matchedStoreId || null,
    distanceMeters: eligibility?.distanceMeters ?? null,
    insideServiceArea: Boolean(eligibility?.insideServiceArea),
    storeOpenNow: Boolean(eligibility?.storeOpenNow),
    eligibilityReason: eligibility?.eligibilityReason || null
  }
}

function normalizeLocationResolve(result) {
  return {
    locationId: result?.locationId || null,
    geocode: normalizeGeocodeResult(result?.geocode),
    eligibility: normalizeServiceEligibility(result?.eligibility)
  }
}

function normalizeNearbyStore(store) {
  return {
    id: store?.storeId,
    name: store?.name || '',
    addressLine1: store?.addressLine1 || '',
    city: store?.city || '',
    state: store?.state || '',
    zipCode: store?.zipCode || '',
    country: store?.country || '',
    region: store?.region || '',
    latitude: store?.latitude ?? null,
    longitude: store?.longitude ?? null,
    serviceRadiusMeters: store?.serviceRadiusMeters ?? null,
    openTime: store?.openTime || null,
    closeTime: store?.closeTime || null,
    active: Boolean(store?.active),
    services: Array.isArray(store?.services) ? store.services : [],
    distanceMeters: store?.distanceMeters ?? null,
    openNow: Boolean(store?.openNow)
  }
}

function buildAddressPayload(payload) {
  return {
    label: payload?.label || null,
    fullName: payload?.recipientName || payload?.fullName || '',
    phone: payload?.phone || null,
    line1: payload?.line1 || '',
    line2: payload?.line2 || null,
    district: payload?.district || null,
    city: payload?.city || '',
    state: payload?.stateProvince || payload?.state || '',
    zipCode: payload?.postalCode || payload?.zipCode || '',
    country: payload?.countryCode || payload?.country || '',
    latitude: payload?.latitude ?? null,
    longitude: payload?.longitude ?? null,
    accuracyMeters: payload?.accuracyMeters ?? null,
    placeId: payload?.placeId || null,
    formattedAddress: payload?.formattedAddress || null,
    deliveryInstructions: payload?.deliveryInstructions || null,
    defaultAddress: Boolean(payload?.defaultShipping || payload?.defaultBilling)
  }
}

function normalizeOrder(order) {
  return {
    id: order?.id,
    orderNumber: order?.id ? `ORD-${String(order.id).split('-')[0].toUpperCase()}` : null,
    status: order?.status || 'PENDING',
    currencyCode: 'USD',
    subtotal: order?.subtotal || 0,
    discountTotal: order?.discountAmount || 0,
    shippingTotal: order?.shippingAmount || 0,
    taxTotal: 0,
    grandTotal: order?.totalAmount || 0,
    placedAt: order?.createdAt || null,
    shippingAddress: null,
    latestPayment: null,
    items: (order?.items || []).map((item, index) => ({
      id: `${order?.id || 'order'}-${index}`,
      productId: item?.productId,
      sku: null,
      productName: item?.productName,
      quantity: item?.quantity || 0,
      unitLabel: 'item',
      unitPrice: item?.unitPrice || 0,
      lineTotal: item?.lineTotal || 0
    }))
  }
}

function normalizeCatalogSort(sort) {
  switch (sort) {
    case 'name':
    case 'priceAsc':
    case 'priceDesc':
    case 'popularity':
    case 'new':
    case 'trending':
    case 'bestselling':
      return sort
    default:
      return 'featured'
  }
}

function buildShippingAddressSnapshot(address) {
  if (!address) {
    return 'Store pickup requested.'
  }

  if (address?.formattedAddress) {
    return address.formattedAddress
  }

  return [
    address.recipientName,
    address.line1,
    address.line2,
    address.district,
    address.city,
    address.stateProvince,
    address.postalCode,
    address.countryCode
  ]
    .filter(Boolean)
    .join(', ')
}

function unsupportedFeatureError(feature) {
  return new Error(`${feature} are not available on the active backend profile. ${UNSUPPORTED_PROFILE_MESSAGE}`)
}

function safeParseJson(value) {
  if (!value) return {}
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

function buildCarouselHref(linkType, linkValue) {
  if (!linkValue) return null

  switch ((linkType || 'INTERNAL').toUpperCase()) {
    case 'EXTERNAL':
      return linkValue
    case 'CATEGORY':
      return `/products?categoryId=${encodeURIComponent(linkValue)}`
    case 'PRODUCT':
      return `/products/${encodeURIComponent(linkValue)}`
    case 'COLLECTION':
      return `/products?collection=${encodeURIComponent(linkValue)}`
    case 'CUSTOM':
    case 'INTERNAL':
    default:
      return linkValue.startsWith('/') ? linkValue : `/${linkValue.replace(/^\/+/, '')}`
  }
}

function normalizeCarouselAction(linkType, linkValue, text, openInNewTab) {
  if (!text || !linkValue) {
    return null
  }

  const href = buildCarouselHref(linkType, linkValue)
  if (!href) {
    return null
  }

  return {
    text,
    href,
    external: String(linkType || '').toUpperCase() === 'EXTERNAL',
    openInNewTab: Boolean(openInNewTab)
  }
}

function normalizeHeroSlide(slide) {
  const theme = safeParseJson(slide?.themeMetadataJson)

  return {
    id: slide?.id,
    title: slide?.title || '',
    subtitle: theme?.eyebrow || slide?.audienceSegment || '',
    description: slide?.description || '',
    imageUrl: slide?.imageDesktop || slide?.imageMobile || null,
    imageMobileUrl: slide?.imageMobile || slide?.imageDesktop || null,
    altText: slide?.altText || slide?.title || 'Hero slide',
    cta: normalizeCarouselAction(slide?.linkType, slide?.linkValue, slide?.buttonText, slide?.openInNewTab),
    secondaryCta: normalizeCarouselAction(
      slide?.secondaryLinkType,
      slide?.secondaryLinkValue,
      slide?.secondaryButtonText,
      slide?.secondaryOpenInNewTab
    ),
    overlay: slide?.backgroundStyle || theme?.overlay || 'gradient',
    textAlign: theme?.contentPosition || theme?.textAlign || 'left',
    textColor: theme?.textColor || 'light',
    analyticsKey: slide?.analyticsKey || null,
    experimentKey: slide?.experimentKey || null
  }
}

export async function getHeroSlides({ storeId, channelId, locale, audienceSegment, previewToken } = {}) {
  const params = new URLSearchParams()
  if (storeId) params.set('storeId', storeId)
  if (channelId) params.set('channelId', channelId)
  if (locale) params.set('locale', locale)
  if (audienceSegment) params.set('audienceSegment', audienceSegment)
  if (previewToken) params.set('previewToken', previewToken)

  const suffix = params.toString() ? `?${params.toString()}` : ''
  const data = await request(`${ACTIVE_API_PREFIX}/carousels/hero${suffix}`)
  return (data || []).map(normalizeHeroSlide)
}

export async function getCategories() {
  const data = await request(`${ACTIVE_API_PREFIX}/categories/tree`)
  return normalizeCategoryTree(data)
}

export async function getProducts({ q, categoryId, page = 0, size = 12, sort = 'featured' } = {}) {
  const params = new URLSearchParams()
  const safeSort = normalizeCatalogSort(sort)

  if (q) params.set('query', q)
  if (categoryId) params.set('categoryId', categoryId)
  params.set('page', String(page))
  params.set('size', String(size))
  params.set('sort', safeSort)

  const data = await request(`${ACTIVE_API_PREFIX}/merchandising/products?${params.toString()}`)

  return {
    items: (data?.content || []).map(normalizeProductCard),
    page: data?.page || 0,
    hasNext: Boolean(data && data.last === false),
    hasPrevious: Boolean(data && data.first === false)
  }
}

export async function getProduct(productId) {
  const data = await request(`${ACTIVE_API_PREFIX}/products/${productId}`)
  return normalizeProductDetail(data)
}

export async function getAvailability(productId) {
  const data = await request(`${ACTIVE_API_PREFIX}/products/${productId}/inventory`)
  return normalizeAvailability(data)
}

export async function registerCustomer(payload) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/auth/register`, null, {
    method: 'POST',
    body: JSON.stringify({
      fullName: buildFullName(payload),
      email: payload?.email?.trim() || '',
      password: payload?.password || ''
    })
  })
}

export async function loginCustomer(payload) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/auth/login`, null, {
    method: 'POST',
    body: JSON.stringify({
      email: payload?.email?.trim() || '',
      password: payload?.password || ''
    })
  })
}

export async function getCustomerMe(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/profile`, token)
  return normalizeProfile(data)
}

export async function getCustomerAddresses(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses`, token)
  return (data || []).map(normalizeAddress)
}

export async function getCustomerAddress(token, addressId) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses/${addressId}`, token)
  return normalizeAddress(data)
}

export async function addCustomerAddress(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses`, token, {
    method: 'POST',
    body: JSON.stringify(buildAddressPayload(payload))
  })

  return normalizeAddress(data)
}

export async function updateCustomerAddress(token, addressId, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses/${addressId}`, token, {
    method: 'PUT',
    body: JSON.stringify(buildAddressPayload(payload))
  })

  return normalizeAddress(data)
}

export async function deleteCustomerAddress(token, addressId) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses/${addressId}`, token, {
    method: 'DELETE'
  })
}

export async function setDefaultCustomerAddress(token, addressId) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/addresses/${addressId}/set-default`, token, {
    method: 'POST'
  })

  return normalizeAddress(data)
}

export async function resolveLocation(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/location/resolve`, token, {
    method: 'POST',
    body: JSON.stringify({
      latitude: payload?.latitude,
      longitude: payload?.longitude,
      accuracyMeters: payload?.accuracyMeters ?? null,
      source: payload?.source || null,
      consentGiven: Boolean(payload?.consentGiven),
      purpose: payload?.purpose || null,
      persist: Boolean(payload?.persist),
      serviceType: payload?.serviceType || 'DELIVERY'
    })
  })

  return normalizeLocationResolve(data)
}

export async function reverseGeocode(payload) {
  const data = await request(`${ACTIVE_API_PREFIX}/location/reverse-geocode`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      latitude: payload?.latitude,
      longitude: payload?.longitude,
      locale: payload?.locale || null
    })
  })

  return normalizeGeocodeResult(data)
}

export async function forwardGeocode(payload) {
  const data = await request(`${ACTIVE_API_PREFIX}/location/forward-geocode`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      query: payload?.query || '',
      limit: payload?.limit ?? 5,
      countryCodes: payload?.countryCodes || null,
      locale: payload?.locale || null
    })
  })

  return (data || []).map(normalizeGeocodeResult)
}

export async function validateServiceArea(payload) {
  const data = await request(`${ACTIVE_API_PREFIX}/location/validate-service-area`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      latitude: payload?.latitude,
      longitude: payload?.longitude,
      serviceType: payload?.serviceType || 'DELIVERY',
      at: payload?.at || null,
      maxDistanceMeters: payload?.maxDistanceMeters ?? null
    })
  })

  return normalizeServiceEligibility(data)
}

export async function getNearbyStores({
  latitude,
  longitude,
  serviceType = 'DELIVERY',
  openNow,
  limit = 5,
  maxDistanceMeters
}) {
  const params = new URLSearchParams()
  params.set('lat', String(latitude))
  params.set('lng', String(longitude))
  if (serviceType) params.set('serviceType', serviceType)
  if (typeof openNow === 'boolean') params.set('openNow', String(openNow))
  if (limit) params.set('limit', String(limit))
  if (maxDistanceMeters) params.set('maxDistanceMeters', String(maxDistanceMeters))

  const data = await request(`${ACTIVE_API_PREFIX}/location/nearby-stores?${params.toString()}`)
  return (data || []).map(normalizeNearbyStore)
}

export async function getCart(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart`, token)
  return normalizeCart(data)
}

export async function addCartItem(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart/items`, token, {
    method: 'POST',
    body: JSON.stringify({
      productId: payload?.productId,
      variantId: null,
      quantity: payload?.quantity || 1,
      storeId: null,
      analyticsListName: payload?.analyticsListName || null,
      analyticsSlot: payload?.analyticsSlot ?? null,
      analyticsPagePath: payload?.analyticsPagePath || null
    })
  })

  return normalizeCart(data)
}

export async function updateCartItem(token, itemId, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart/items/${itemId}`, token, {
    method: 'PUT',
    body: JSON.stringify({
      quantity: payload?.quantity || 1
    })
  })

  return normalizeCart(data)
}

export async function removeCartItem(token, itemId) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart/items/${itemId}`, token, {
    method: 'DELETE'
  })

  return normalizeCart(data)
}

export async function clearCart(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart/items`, token, {
    method: 'DELETE'
  })

  return normalizeCart(data)
}

export async function checkoutCart(token, body) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/checkout`, token, {
    method: 'POST',
    body: JSON.stringify({
      fulfillmentMethod: body?.fulfillmentMethod || (body?.addressId || body?.shippingAddress ? 'DELIVERY' : 'PICKUP'),
      storeId: body?.storeId || null,
      addressId: body?.addressId || body?.shippingAddress?.id || null,
      shippingAddressSnapshot:
        body?.shippingAddressSnapshot ||
        buildShippingAddressSnapshot(body?.shippingAddress),
      paymentReference:
        [body?.paymentMethod, body?.paymentProvider, body?.paymentProviderReference]
          .filter(Boolean)
          .join(' | ') || 'Storefront checkout',
      couponCode: body?.couponCode || null,
      b2bInvoice: false,
      idempotencyKey: null
    })
  })

  return normalizeOrder(data)
}

export async function getOrderPayments() {
  return []
}

export async function createOrderPayment() {
  throw unsupportedFeatureError('Post-checkout payment creation')
}

export async function captureOrderPayment() {
  throw unsupportedFeatureError('Post-checkout payment capture')
}

export async function getMyOrders(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/orders`, token)
  return (data || []).map(normalizeOrder)
}

export async function getOrder(token, orderId) {
  const orders = await getMyOrders(token)
  const order = orders.find((item) => String(item.id) === String(orderId))

  if (!order) {
    throw new Error('Order not found.')
  }

  return order
}

export async function cancelOrder() {
  throw unsupportedFeatureError('Order cancellation')
}

export async function getOrderFulfillment() {
  return null
}

export async function listReturns() {
  return []
}

export async function getReturnRequest() {
  throw unsupportedFeatureError('Returns')
}

export async function createReturnRequest() {
  throw unsupportedFeatureError('Returns')
}

export async function cancelReturnRequest() {
  throw unsupportedFeatureError('Returns')
}

// ── Search & Discovery ──────────────────────────────────────────────

export async function predictiveSearch(query, scope = 'all') {
  const params = new URLSearchParams()
  if (query) params.set('q', query)
  params.set('scope', scope)
  const data = await request(`${ACTIVE_API_PREFIX}/search/predictive?${params.toString()}`)
  return data || []
}

export async function getTrendTags() {
  const data = await request(`${ACTIVE_API_PREFIX}/search/trend-tags`)
  return data || []
}

// ── Recommendations ─────────────────────────────────────────────────

export async function getBestSellers() {
  const data = await request(`${ACTIVE_API_PREFIX}/recommendations/best-sellers`)
  return (data || []).map(normalizeRecommendationCard)
}

export async function getTrendingProducts() {
  const data = await request(`${ACTIVE_API_PREFIX}/recommendations/trending`)
  return (data || []).map(normalizeRecommendationCard)
}

export async function getDeals() {
  const data = await request(`${ACTIVE_API_PREFIX}/recommendations/deals`)
  return (data || []).map(normalizeRecommendationCard)
}

export async function getPersonalizedRecommendations(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/recommendations/personalized`, token)
  return (data || []).map(normalizeRecommendationCard)
}

export async function getCrossSellProducts(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/recommendations/cross-sell`, token)
  return (data || []).map(normalizeRecommendationCard)
}

// ── Product Reviews ─────────────────────────────────────────────────

export async function getProductReviews(productId) {
  const data = await request(`${ACTIVE_API_PREFIX}/products/${productId}/reviews`)
  return data || []
}

export async function addProductReview(token, productId, payload) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/products/${productId}/reviews`, token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

// ── Related Products ────────────────────────────────────────────────

export async function getRelatedProducts(productId) {
  const data = await request(`${ACTIVE_API_PREFIX}/products/${productId}/related`)
  return (data || []).map(normalizeRecommendationCard)
}

export async function getFrequentlyBoughtTogether(productId) {
  const data = await request(`${ACTIVE_API_PREFIX}/products/${productId}/frequently-bought-together`)
  return (data || []).map(normalizeRecommendationCard)
}

// ── Promotions ──────────────────────────────────────────────────────

export async function getActivePromotions() {
  const data = await request(`${ACTIVE_API_PREFIX}/promotions/active`)
  return data || []
}

// ── Coupon ──────────────────────────────────────────────────────────

export async function applyCoupon(token, couponCode) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/cart/coupon`, token, {
    method: 'POST',
    body: JSON.stringify({ couponCode })
  })
  return normalizeCart(data)
}

// ── Multi-step Checkout ─────────────────────────────────────────────

export async function getCheckoutPreview(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/checkout/steps/review`, token)
  return data
}

export async function submitCheckoutShipping(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/checkout/steps/shipping`, token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  return data
}

export async function submitCheckoutPayment(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/checkout/steps/payment`, token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  return data
}

export async function confirmCheckout(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/checkout/steps/confirm`, token, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return normalizeOrder(data)
}

// ── Order Timeline ──────────────────────────────────────────────────

export async function getOrderTimeline(token, orderId) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/orders/${orderId}/timeline`, token)
  return data || []
}

// ── Stores ──────────────────────────────────────────────────────────

export async function getStores({ page = 0, size = 20 } = {}) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  const data = await request(`${ACTIVE_API_PREFIX}/stores?${params.toString()}`)
  return {
    items: data?.content || [],
    page: data?.page || 0,
    hasNext: Boolean(data && data.last === false),
    hasPrevious: Boolean(data && data.first === false)
  }
}

export async function getNearestStores(lat, lng, limit = 5) {
  const params = new URLSearchParams()
  params.set('lat', String(lat))
  params.set('lng', String(lng))
  params.set('limit', String(limit))
  const data = await request(`${ACTIVE_API_PREFIX}/stores/nearest?${params.toString()}`)
  return data || []
}

// ── Notifications ───────────────────────────────────────────────────

export async function getMyNotifications(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/notifications/me`, token)
  return data || []
}

export async function getUnreadCount(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/notifications/me/unread-count`, token)
  return data || 0
}

export async function markNotificationRead(token, notificationId) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/notifications/${notificationId}/read`, token, {
    method: 'PATCH'
  })
}

export async function markAllNotificationsRead(token) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/notifications/me/read-all`, token, {
    method: 'PATCH'
  })
}

// ── Payment Methods ─────────────────────────────────────────────────

export async function getPaymentMethods(token) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/payment-methods`, token)
  return data || []
}

export async function addPaymentMethod(token, payload) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/account/payment-methods`, token, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deletePaymentMethod(token, paymentMethodId) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/account/payment-methods/${paymentMethodId}`, token, {
    method: 'DELETE'
  })
}

// ── Profile ─────────────────────────────────────────────────────────

export async function updateProfile(token, payload) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/profile`, token, {
    method: 'PUT',
    body: JSON.stringify({
      fullName: buildFullName(payload),
      phone: payload?.phone || ''
    })
  })
  return normalizeProfile(data)
}

// ── Quick Reorder ───────────────────────────────────────────────────

export async function quickReorder(token, orderId) {
  const data = await requestWithAuth(`${ACTIVE_API_PREFIX}/account/orders/${orderId}/quick-reorder`, token, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return data
}

// ── Password Reset ──────────────────────────────────────────────────

export async function requestPasswordReset(email) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/auth/password-reset/request`, null, {
    method: 'POST',
    body: JSON.stringify({ email })
  })
}

export async function confirmPasswordReset(token, newPassword) {
  return requestWithAuth(`${ACTIVE_API_PREFIX}/auth/password-reset/confirm`, null, {
    method: 'POST',
    body: JSON.stringify({ token, newPassword })
  })
}
