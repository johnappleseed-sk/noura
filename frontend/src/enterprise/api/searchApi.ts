import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { ApiResponse } from '@/types'

interface BackendSearchSuggestion {
  value: string
  scope: string
}

interface BackendTrendTag {
  value: string
  score: number
}

export const searchApi = {
  predictive: async (query: string, scope = 'all'): Promise<string[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendSearchSuggestion[]>>('/search/predictive', {
        params: { q: query, scope },
      })
      return unwrapApiResponse(response.data).map((entry) => entry.value)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load predictive suggestions'))
    }
  },

  trendTags: async (): Promise<string[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<BackendTrendTag[]>>('/search/trend-tags')
      return unwrapApiResponse(response.data).map((entry) => entry.value)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load trend tags'))
    }
  },
}
