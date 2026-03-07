import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { BackendProduct, toProduct } from '@/api/mappers'
import { ApiResponse, PageResponse, Product } from '@/types'

export const productsApi = {
  getProducts: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<PageResponse<BackendProduct>>>('/products', {
        params: {
          page: 0,
          size: 100,
          sortBy: 'createdAt',
          direction: 'desc',
        },
      })
      return unwrapApiResponse(response.data).content.map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load products from backend'))
    }
  },

  getProductById: async (productId: string): Promise<Product> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct>>(`/products/${productId}`)
      return toProduct(unwrapApiResponse(response.data), 0)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load product from backend'))
    }
  },

  getRelatedProducts: async (productId: string): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>(`/products/${productId}/related`)
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load related products'))
    }
  },

  getFrequentlyBoughtTogether: async (productId: string): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>(
        `/products/${productId}/frequently-bought-together`,
      )
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load frequently bought together products'))
    }
  },
}
