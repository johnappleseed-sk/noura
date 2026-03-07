import { AxiosError } from 'axios'
import { ApiResponse } from '@/types'

/**
 * Executes unwrap api response.
 *
 * @param payload The payload value.
 * @returns The result of unwrap api response.
 */
export const unwrapApiResponse = <T>(payload: ApiResponse<T>): T => {
  if (!payload.success) {
    throw new Error(payload.error?.detail ?? payload.message ?? 'Backend request failed')
  }
  return payload.data
}

/**
 * Executes extract api message.
 *
 * @param error The error value.
 * @param fallback The fallback value.
 * @returns The result of extract api message.
 */
export const extractApiMessage = (error: unknown, fallback = 'Backend request failed'): string => {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: { detail?: string }; message?: string } | undefined
    return data?.error?.detail ?? data?.message ?? error.message
  }
  return error instanceof Error ? error.message : fallback
}
