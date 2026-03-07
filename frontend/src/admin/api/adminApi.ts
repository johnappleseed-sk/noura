import { apiClient, unwrap } from '@/api/client';
import type { PageResponse } from '@/types/api';
import type {
  Approval,
  ApprovalStatus,
  DashboardSummary,
  NotificationItem,
  Order,
  OrderTimelineEvent,
  OrderStatus,
  Product,
  ProductRequest,
  RefundStatus,
  Store,
  StoreRequest,
  UserProfile,
} from '@/types/models';

interface PaginationQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

/**
 * Executes pagination.
 *
 * @param query The search query text.
 * @returns The result of pagination.
 */
const pagination = (query?: PaginationQuery): PaginationQuery => ({
  page: query?.page ?? 0,
  size: query?.size ?? 20,
  sortBy: query?.sortBy ?? 'createdAt',
  direction: query?.direction ?? 'desc',
});

export const adminApi = {
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const response = await apiClient.get('/admin/dashboard/summary');
    return unwrap<DashboardSummary>(response);
  },

  getProducts: async (query?: PaginationQuery): Promise<PageResponse<Product>> => {
    const response = await apiClient.get('/products', {
      params: pagination({ ...query, sortBy: query?.sortBy ?? 'createdAt' }),
    });
    return unwrap<PageResponse<Product>>(response);
  },

  createProduct: async (payload: ProductRequest): Promise<Product> => {
    const response = await apiClient.post('/products', payload);
    return unwrap<Product>(response);
  },

  updateProduct: async (productId: string, payload: ProductRequest): Promise<Product> => {
    const response = await apiClient.put(`/products/${productId}`, payload);
    return unwrap<Product>(response);
  },

  deleteProduct: async (productId: string): Promise<void> => {
    const response = await apiClient.delete(`/products/${productId}`);
    unwrap<void>(response);
  },

  getOrders: async (query?: PaginationQuery): Promise<PageResponse<Order>> => {
    const response = await apiClient.get('/orders', {
      params: pagination({ ...query, sortBy: query?.sortBy ?? 'createdAt' }),
    });
    return unwrap<PageResponse<Order>>(response);
  },

  updateOrderStatus: async (
    orderId: string,
    status: OrderStatus,
    refundStatus: RefundStatus,
  ): Promise<Order> => {
    const response = await apiClient.patch(`/orders/${orderId}/status`, { status, refundStatus });
    return unwrap<Order>(response);
  },

  getOrderTimeline: async (orderId: string): Promise<OrderTimelineEvent[]> => {
    const response = await apiClient.get(`/orders/${orderId}/timeline`);
    return unwrap<OrderTimelineEvent[]>(response);
  },

  getStores: async (query?: PaginationQuery): Promise<PageResponse<Store>> => {
    const response = await apiClient.get('/stores', {
      params: pagination({ ...query, sortBy: query?.sortBy ?? 'name', direction: query?.direction ?? 'asc' }),
    });
    return unwrap<PageResponse<Store>>(response);
  },

  createStore: async (payload: StoreRequest): Promise<Store> => {
    const response = await apiClient.post('/stores', payload);
    return unwrap<Store>(response);
  },

  updateStore: async (storeId: string, payload: StoreRequest): Promise<Store> => {
    const response = await apiClient.put(`/stores/${storeId}`, payload);
    return unwrap<Store>(response);
  },

  deleteStore: async (storeId: string): Promise<void> => {
    const response = await apiClient.delete(`/stores/${storeId}`);
    unwrap<void>(response);
  },

  getUsers: async (query?: PaginationQuery): Promise<PageResponse<UserProfile>> => {
    const response = await apiClient.get('/admin/users', {
      params: pagination(query),
    });
    return unwrap<PageResponse<UserProfile>>(response);
  },

  updateUser: async (userId: string, payload: { roles?: UserProfile['roles']; enabled?: boolean }): Promise<UserProfile> => {
    const response = await apiClient.patch(`/admin/users/${userId}`, payload);
    return unwrap<UserProfile>(response);
  },

  getApprovals: async (): Promise<Approval[]> => {
    const response = await apiClient.get('/admin/b2b/approvals');
    return unwrap<Approval[]>(response);
  },

  updateApproval: async (approvalId: string, status: ApprovalStatus, reviewerNotes: string): Promise<Approval> => {
    const response = await apiClient.patch(`/admin/b2b/approvals/${approvalId}`, { status, reviewerNotes });
    return unwrap<Approval>(response);
  },

  sendBroadcast: async (payload: { category: string; title: string; body: string }): Promise<void> => {
    const response = await apiClient.post('/notifications/broadcast', payload);
    unwrap<void>(response);
  },

  sendUserNotification: async (
    userId: string,
    payload: { category: string; title: string; body: string },
  ): Promise<void> => {
    const response = await apiClient.post(`/notifications/user/${userId}`, payload);
    unwrap<void>(response);
  },

  getMyNotifications: async (): Promise<NotificationItem[]> => {
    const response = await apiClient.get('/notifications/me');
    return unwrap<NotificationItem[]>(response);
  },

  getUnreadNotificationsCount: async (): Promise<number> => {
    const response = await apiClient.get('/notifications/me/unread-count');
    return unwrap<number>(response);
  },

  markNotificationRead: async (notificationId: string): Promise<NotificationItem> => {
    const response = await apiClient.patch(`/notifications/${notificationId}/read`);
    return unwrap<NotificationItem>(response);
  },

  markAllNotificationsRead: async (): Promise<number> => {
    const response = await apiClient.patch('/notifications/me/read-all');
    return unwrap<number>(response);
  },
};
