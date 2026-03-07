import { apiClient } from '@/admin/api/client'
import {
  ApiEnvelope,
  Approval,
  DashboardSummary,
  NotificationPayload,
  Order,
  OrderStatus,
  PagePayload,
  Product,
  RefundStatus,
  Store,
  UserProfile,
} from '@/admin/types'

export interface ProductUpsertRequest {
  name: string
  category: string
  brand: string
  price: number
  flashSale: boolean
  trending: boolean
  bestSeller: boolean
  shortDescription: string
  longDescription: string
  seoTitle: string
  seoDescription: string
  seoSlug: string
  variants: { color: string; size: string; sku: string }[]
  media: { mediaType: string; url: string; sortOrder: number }[]
  inventory: { storeId: string; stock: number; storePrice: number }[]
}

export interface StoreUpsertRequest {
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

const unwrap = <T>(payload: ApiEnvelope<T>): T => payload.data

const unwrapPage = <T>(payload: ApiEnvelope<PagePayload<T>>): PagePayload<T> => payload.data

export const adminApi = {
  /**
   * Retrieves get dashboard summary.
   *
   * @returns A promise that resolves with the operation result.
   */
  async getDashboardSummary(): Promise<DashboardSummary> {
    const response = await apiClient.get<ApiEnvelope<DashboardSummary>>('/admin/dashboard/summary')
    return unwrap(response.data)
  },

  /**
   * Retrieves get products.
   *
   * @param params The params value.
   * @returns A promise that resolves with the operation result.
   */
  async getProducts(params: Record<string, string | number | boolean | undefined>): Promise<PagePayload<Product>> {
    const response = await apiClient.get<ApiEnvelope<PagePayload<Product>>>('/products', { params })
    return unwrapPage(response.data)
  },

  /**
   * Creates create product.
   *
   * @param payload The payload value.
   * @returns A promise that resolves with the operation result.
   */
  async createProduct(payload: ProductUpsertRequest): Promise<Product> {
    const response = await apiClient.post<ApiEnvelope<Product>>('/products', payload)
    return unwrap(response.data)
  },

  /**
   * Updates update product.
   *
   * @param productId The product id used to locate the target record.
   * @param payload The payload value.
   * @returns A promise that resolves with the operation result.
   */
  async updateProduct(productId: string, payload: ProductUpsertRequest): Promise<Product> {
    const response = await apiClient.put<ApiEnvelope<Product>>(`/products/${productId}`, payload)
    return unwrap(response.data)
  },

  /**
   * Removes delete product.
   *
   * @param productId The product id used to locate the target record.
   * @returns No value.
   */
  async deleteProduct(productId: string): Promise<void> {
    await apiClient.delete(`/products/${productId}`)
  },

  /**
   * Retrieves get orders.
   *
   * @param params The params value.
   * @returns A promise that resolves with the operation result.
   */
  async getOrders(params: Record<string, string | number | boolean | undefined>): Promise<PagePayload<Order>> {
    const response = await apiClient.get<ApiEnvelope<PagePayload<Order>>>('/orders', { params })
    return unwrapPage(response.data)
  },

  /**
   * Updates update order.
   *
   * @param orderId The order id used to locate the target record.
   * @param status The status value.
   * @param refundStatus The refund status value.
   * @returns A promise that resolves with the operation result.
   */
  async updateOrder(orderId: string, status: OrderStatus, refundStatus: RefundStatus): Promise<Order> {
    const response = await apiClient.patch<ApiEnvelope<Order>>(`/orders/${orderId}/status`, { status, refundStatus })
    return unwrap(response.data)
  },

  /**
   * Retrieves get stores.
   *
   * @param params The params value.
   * @returns A promise that resolves with the operation result.
   */
  async getStores(params: Record<string, string | number | boolean | undefined>): Promise<PagePayload<Store>> {
    const response = await apiClient.get<ApiEnvelope<PagePayload<Store>>>('/stores', { params })
    return unwrapPage(response.data)
  },

  /**
   * Creates create store.
   *
   * @param payload The payload value.
   * @returns A promise that resolves with the operation result.
   */
  async createStore(payload: StoreUpsertRequest): Promise<Store> {
    const response = await apiClient.post<ApiEnvelope<Store>>('/stores', payload)
    return unwrap(response.data)
  },

  /**
   * Updates update store.
   *
   * @param storeId The store id used to locate the target record.
   * @param payload The payload value.
   * @returns A promise that resolves with the operation result.
   */
  async updateStore(storeId: string, payload: StoreUpsertRequest): Promise<Store> {
    const response = await apiClient.put<ApiEnvelope<Store>>(`/stores/${storeId}`, payload)
    return unwrap(response.data)
  },

  /**
   * Removes delete store.
   *
   * @param storeId The store id used to locate the target record.
   * @returns No value.
   */
  async deleteStore(storeId: string): Promise<void> {
    await apiClient.delete(`/stores/${storeId}`)
  },

  /**
   * Retrieves get users.
   *
   * @param params The params value.
   * @returns A promise that resolves with the operation result.
   */
  async getUsers(params: Record<string, string | number | boolean | undefined>): Promise<PagePayload<UserProfile>> {
    const response = await apiClient.get<ApiEnvelope<PagePayload<UserProfile>>>('/admin/users', { params })
    return unwrapPage(response.data)
  },

  /**
   * Retrieves get approvals.
   *
   * @returns A promise that resolves with the operation result.
   */
  async getApprovals(): Promise<Approval[]> {
    const response = await apiClient.get<ApiEnvelope<Approval[]>>('/admin/b2b/approvals')
    return unwrap(response.data)
  },

  /**
   * Updates update approval.
   *
   * @param approvalId The approval id used to locate the target record.
   * @param status The status value.
   * @param reviewerNotes The reviewer notes value.
   * @returns A promise that resolves with the operation result.
   */
  async updateApproval(approvalId: string, status: Approval['status'], reviewerNotes: string): Promise<Approval> {
    const response = await apiClient.patch<ApiEnvelope<Approval>>(`/admin/b2b/approvals/${approvalId}`, {
      status,
      reviewerNotes,
    })
    return unwrap(response.data)
  },

  /**
   * Executes send broadcast.
   *
   * @param payload The payload value.
   * @returns No value.
   */
  async sendBroadcast(payload: NotificationPayload): Promise<void> {
    await apiClient.post('/notifications/broadcast', payload)
  },

  /**
   * Executes send user notification.
   *
   * @param userId The user id used to locate the target record.
   * @param payload The payload value.
   * @returns No value.
   */
  async sendUserNotification(userId: string, payload: NotificationPayload): Promise<void> {
    await apiClient.post(`/notifications/user/${userId}`, payload)
  },
}
