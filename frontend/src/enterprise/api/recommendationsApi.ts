import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { BackendProduct, toProduct } from '@/api/mappers'
import { ApiResponse, Product } from '@/types'

export const recommendationsApi = {
  personalized: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/personalized')
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load personalized recommendations'))
    }
  },

  crossSell: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/cross-sell')
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load cross-sell recommendations'))
    }
  },

  bestSellers: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/best-sellers')
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load best sellers'))
    }
  },

  trending: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/trending')
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load trending products'))
    }
  },

  deals: async (): Promise<Product[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/deals')
      return unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load deals'))
    }
  },

  aiRanked: async (): Promise<{ products: Product[]; reason: string; engine: string; generatedAt: string }> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendProduct[]>>('/recommendations/personalized')
      const products = unwrapApiResponse(response.data).map((item, index) => toProduct(item, index))
      return {
        products,
        reason: 'Personalized ranking from backend recommendation service.',
        engine: 'RecommendationService',
        generatedAt: new Date().toISOString(),
      }
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load AI recommendation feed'))
    }
  },
}
