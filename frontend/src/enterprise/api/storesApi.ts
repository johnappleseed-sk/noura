import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { BackendStore, toStoreLocation } from '@/api/mappers'
import { ApiResponse, PageResponse, StoreLocation } from '@/types'

export const storesApi = {
  getStores: async (): Promise<StoreLocation[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<PageResponse<BackendStore>>>('/stores', {
        params: {
          page: 0,
          size: 100,
          sortBy: 'name',
          direction: 'asc',
        },
      })
      return unwrapApiResponse(response.data).content.map(toStoreLocation)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load stores from backend'))
    }
  },
}
