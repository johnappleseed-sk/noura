export type Role = 'ADMIN' | 'CUSTOMER' | 'B2B'

export interface ApiEnvelope<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
  path: string
}

export interface PagePayload<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface AuthTokensResponse {
  userId: string
  email: string
  fullName: string
  roles: Role[]
  accessToken: string
  refreshToken: string
}

export interface DashboardSummary {
  revenue: number
  ordersCount: number
  usersCount: number
  storesCount: number
  topProducts: string[]
  storePerformance: string[]
}

export interface ProductVariant {
  id: string
  color: string
  size: string
  sku: string
}

export interface ProductMedia {
  id: string
  mediaType: string
  url: string
  sortOrder: number
}

export interface ProductStoreInventory {
  storeId: string
  storeName: string
  stock: number
  storePrice: number
}

export interface Product {
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
  shortDescription: string | null
  longDescription: string | null
  seoTitle: string | null
  seoDescription: string | null
  seoSlug: string | null
  variants: ProductVariant[]
  media: ProductMedia[]
  storeInventory: ProductStoreInventory[]
}

export interface OrderItem {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export type OrderStatus =
  | 'CREATED'
  | 'REVIEWED'
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'PACKED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export type RefundStatus = 'NONE' | 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'COMPLETED'

export interface Order {
  id: string
  userId: string
  storeId: string | null
  subtotal: number
  discountAmount: number
  shippingAmount: number
  totalAmount: number
  fulfillmentMethod: 'PICKUP' | 'DELIVERY'
  status: OrderStatus
  refundStatus: RefundStatus
  couponCode: string | null
  createdAt: string
  items: OrderItem[]
}

export interface Store {
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
  services: ('PICKUP' | 'DELIVERY' | 'CURBSIDE' | 'B2B_DESK')[]
  shippingFee: number
  freeShippingThreshold: number
  distanceKm: number
  openNow: boolean
}

export interface UserProfile {
  id: string
  fullName: string
  email: string
  phone: string | null
  roles: Role[]
  preferredStoreId: string | null
}

export interface Approval {
  id: string
  requesterId: string
  orderId: string | null
  amount: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewerNotes: string | null
}

export interface NotificationPayload {
  targetUserId?: string
  category: 'ORDER' | 'SYSTEM' | 'STORE' | 'AI' | 'SECURITY'
  title: string
  body: string
}

export interface AuthUser {
  userId: string
  email: string
  fullName: string
  roles: Role[]
}
