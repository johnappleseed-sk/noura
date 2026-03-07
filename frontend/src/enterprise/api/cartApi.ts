import { AxiosError } from 'axios'
import { axiosClient } from '@/api/axiosClient'

interface BackendEnvelope<T> {
  success: boolean
  message: string
  data: T
  error?: { code?: string; detail?: string }
}

/**
 * Executes unwrap.
 *
 * @param payload The payload value.
 * @returns The result of unwrap.
 */
const unwrap = <T>(payload: BackendEnvelope<T>): T => {
  if (!payload.success) {
    throw new Error(payload.error?.detail ?? payload.message ?? 'Cart API request failed')
  }
  return payload.data
}

/**
 * Executes extract api message.
 *
 * @param error The error value.
 * @returns The result of extract api message.
 */
const extractApiMessage = (error: unknown): string => {
  if (error instanceof AxiosError) {
    return (
      (error.response?.data as { error?: { detail?: string }; message?: string } | undefined)?.error?.detail ??
      (error.response?.data as { message?: string } | undefined)?.message ??
      error.message
    )
  }
  return 'Cart API request failed'
}

export const cartApi = {
  clearCart: async (): Promise<void> => {
    try {
      const response = await axiosClient.delete<BackendEnvelope<unknown>>('/cart/items')
      unwrap(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error))
    }
  },
}

