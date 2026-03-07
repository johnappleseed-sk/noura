import { Order, OrderStatus, Product, StoreHoursEntry, StoreLocation, StoreService, UserRole } from '@/types'

interface BackendProductVariant {
  id: string
  color: string | null
  size: string | null
  sku: string | null
}

interface BackendProductMedia {
  id: string
  mediaType: string
  url: string
  sortOrder: number
}

interface BackendProductStoreInventory {
  storeId: string
  storeName?: string
  stock: number
  storePrice?: number
}

export interface BackendProduct {
  id: string
  name: string
  category: string
  brand: string
  price: number
  flashSale: boolean
  trending: boolean
  bestSeller: boolean
  averageRating: number
  reviewCount: number
  popularityScore: number
  shortDescription?: string
  longDescription?: string
  seoSlug?: string
  variants: BackendProductVariant[]
  media: BackendProductMedia[]
  storeInventory: BackendProductStoreInventory[]
}

export interface BackendStore {
  id: string
  name: string
  addressLine1: string
  city: string
  state: string
  zipCode: string
  country: string
  region: string
  latitude: number
  longitude: number
  openTime: string
  closeTime: string
  active: boolean
  services: Array<'PICKUP' | 'DELIVERY' | 'CURBSIDE' | 'B2B_DESK'>
  shippingFee: number
  freeShippingThreshold: number
}

interface BackendOrderItem {
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface BackendOrder {
  id: string
  storeId?: string | null
  totalAmount: number
  status: string
  createdAt: string
  items: BackendOrderItem[]
}

const toNumber = (value: unknown): number => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

const normalizeTime = (value: string): string => {
  if (!value) {
    return '00:00'
  }
  return value.length >= 5 ? value.slice(0, 5) : value
}

const regionMap: Record<string, StoreLocation['region']> = {
  us: 'us',
  eu: 'eu',
  apac: 'apac',
  global: 'global',
}

const storeServiceMap: Record<BackendStore['services'][number], StoreService> = {
  PICKUP: 'pickup',
  DELIVERY: 'delivery',
  CURBSIDE: 'curbside',
  B2B_DESK: 'b2b-desk',
}

const orderStatusMap: Record<string, OrderStatus> = {
  CREATED: 'created',
  REVIEWED: 'reviewed',
  PAYMENT_PENDING: 'payment_pending',
  PAID: 'paid',
  PACKED: 'packed',
  SHIPPED: 'shipped',
  DELIVERED: 'delivered',
  CANCELLED: 'cancelled',
  REFUNDED: 'refunded',
}

const roleMap: Record<string, UserRole> = {
  ADMIN: 'admin',
  B2B: 'b2b',
  CUSTOMER: 'customer',
}

const defaultStoreHours = (open: string, close: string): StoreHoursEntry[] => [
  { day: 'Mon', open, close },
  { day: 'Tue', open, close },
  { day: 'Wed', open, close },
  { day: 'Thu', open, close },
  { day: 'Fri', open, close },
  { day: 'Sat', open, close },
  { day: 'Sun', open, close },
]

/**
 * Maps source data to Product.
 *
 * @param item The source object to transform.
 * @param index The index value.
 * @returns The mapped DTO representation.
 */
export const toProduct = (item: BackendProduct, index: number): Product => {
  const images = [...item.media]
    .sort((left, right) => left.sortOrder - right.sortOrder)
    .map((media) => media.url)

  const storeInventoryRecord = (item.storeInventory ?? []).reduce<Record<string, number>>((acc, entry) => {
    acc[entry.storeId] = toNumber(entry.stock)
    return acc
  }, {})

  const stock = Object.values(storeInventoryRecord).reduce((sum, current) => sum + current, 0)
  const tags = Array.from(
    new Set(
      [
        item.category,
        item.brand,
        item.seoSlug,
        item.flashSale ? 'flash-sale' : null,
        item.trending ? 'trending' : null,
        item.bestSeller ? 'best-seller' : null,
      ]
        .filter((value): value is string => Boolean(value))
        .map((value) => value.toLowerCase()),
    ),
  )
  const variantSummary = (item.variants ?? [])
    .map((variant) => [variant.color, variant.size].filter(Boolean).join(' / '))
    .filter(Boolean)

  const features = Array.from(
    new Set(
      [
        ...variantSummary,
        stock > 0 ? `In stock (${stock} units)` : 'Out of stock',
        item.storeInventory.length > 0 ? `Available in ${item.storeInventory.length} stores` : null,
      ].filter((value): value is string => Boolean(value)),
    ),
  )

  return {
    id: item.id,
    name: item.name,
    description: item.longDescription ?? item.shortDescription ?? 'No description available.',
    category: item.category,
    brand: item.brand,
    price: toNumber(item.price),
    rating: toNumber(item.averageRating),
    reviewCount: Math.max(0, Math.round(toNumber(item.reviewCount))),
    stock,
    popularity: Math.max(0, Math.round(toNumber(item.popularityScore))),
    createdAt: new Date(Date.now() - index * 60_000).toISOString(),
    images: images.length > 0 ? images : ['/vite.svg'],
    tags,
    features: features.length > 0 ? features : ['Catalog item'],
    reviews: [],
    storeInventory: storeInventoryRecord,
    pickupAvailableStoreIds: Object.entries(storeInventoryRecord)
      .filter(([, value]) => value > 0)
      .map(([storeId]) => storeId),
  }
}

/**
 * Maps source data to StoreLocation.
 *
 * @param item The source object to transform.
 * @returns The mapped DTO representation.
 */
export const toStoreLocation = (item: BackendStore): StoreLocation => {
  const open = normalizeTime(item.openTime)
  const close = normalizeTime(item.closeTime)
  const mappedRegion = regionMap[item.region?.toLowerCase() ?? ''] ?? 'global'

  return {
    id: item.id,
    name: item.name,
    addressLine1: item.addressLine1,
    city: item.city,
    state: item.state,
    zipCode: item.zipCode,
    country: item.country,
    phone: 'N/A',
    latitude: toNumber(item.latitude),
    longitude: toNumber(item.longitude),
    region: mappedRegion,
    hoursSummary: `Daily ${open}-${close}`,
    hours: defaultStoreHours(open, close),
    services: (item.services ?? []).map((service) => storeServiceMap[service]).filter(Boolean),
    freeShippingThreshold: toNumber(item.freeShippingThreshold),
    shippingFee: toNumber(item.shippingFee),
    status: item.active ? 'active' : 'inactive',
  }
}

/**
 * Maps source data to Order.
 *
 * @param item The source object to transform.
 * @param storeNameById The store name by id value.
 * @returns The mapped DTO representation.
 */
export const toOrder = (
  item: BackendOrder,
  storeNameById: Record<string, string> = {},
): Order => ({
  id: item.id,
  createdAt: item.createdAt,
  status: orderStatusMap[item.status] ?? 'created',
  total: toNumber(item.totalAmount),
  storeId: item.storeId ?? null,
  storeName: item.storeId ? storeNameById[item.storeId] : undefined,
  items: (item.items ?? []).map((line) => ({
    productId: line.productId,
    name: line.productName,
    quantity: Math.max(1, Math.round(toNumber(line.quantity))),
    price: toNumber(line.unitPrice),
    storeId: item.storeId ?? null,
  })),
})

/**
 * Executes resolve user role.
 *
 * @param roles The roles value.
 * @returns The result of resolve user role.
 */
export const resolveUserRole = (roles: string[]): UserRole => {
  if (roles.includes('ADMIN')) {
    return 'admin'
  }
  if (roles.includes('B2B')) {
    return 'b2b'
  }
  return 'customer'
}

/**
 * Maps source data to role list.
 *
 * @param roles The roles value.
 * @returns The mapped DTO representation.
 */
export const toUserRoles = (roles: string[]): UserRole[] =>
  roles.map((role) => roleMap[role]).filter((role): role is UserRole => Boolean(role))
