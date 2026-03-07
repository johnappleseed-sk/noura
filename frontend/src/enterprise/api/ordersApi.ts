import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { BackendOrder, BackendStore, toOrder } from '@/api/mappers'
import { ApiResponse, Order, PageResponse } from '@/types'

const toStoreNameById = (stores: BackendStore[]): Record<string, string> =>
  stores.reduce<Record<string, string>>((acc, store) => {
    acc[store.id] = store.name
    return acc
  }, {})

const byCreatedAtDesc = (left: Order, right: Order): number =>
  new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()

export const ordersApi = {
  getMyOrders: async (): Promise<Order[]> => {
    try {
      const [ordersResponse, storesResponse] = await Promise.all([
        axiosClient.get<ApiResponse<BackendOrder[]>>('/account/orders'),
        axiosClient
          .get<ApiResponse<PageResponse<BackendStore>>>('/stores', {
            params: { page: 0, size: 100, sortBy: 'name', direction: 'asc' },
          })
          .catch(() => null),
      ])

      const orders = unwrapApiResponse(ordersResponse.data)
      const storeNameById = storesResponse
        ? toStoreNameById(unwrapApiResponse(storesResponse.data).content)
        : {}

      return orders.map((order) => toOrder(order, storeNameById)).sort(byCreatedAtDesc)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load orders from backend'))
    }
  },

  getAdminOrders: async (): Promise<Order[]> => {
    try {
      const [ordersResponse, storesResponse] = await Promise.all([
        axiosClient.get<ApiResponse<PageResponse<BackendOrder>>>('/orders', {
          params: { page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' },
        }),
        axiosClient
          .get<ApiResponse<PageResponse<BackendStore>>>('/stores', {
            params: { page: 0, size: 100, sortBy: 'name', direction: 'asc' },
          })
          .catch(() => null),
      ])

      const ordersPage = unwrapApiResponse(ordersResponse.data)
      const storeNameById = storesResponse
        ? toStoreNameById(unwrapApiResponse(storesResponse.data).content)
        : {}

      return ordersPage.content.map((order) => toOrder(order, storeNameById)).sort(byCreatedAtDesc)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load admin orders from backend'))
    }
  },
}
