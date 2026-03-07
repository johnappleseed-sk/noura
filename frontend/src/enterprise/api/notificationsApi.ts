import { AxiosError } from 'axios'
import { axiosClient } from '@/api/axiosClient'
import { NotificationCategory, NotificationMessage } from '@/types'

interface BackendEnvelope<T> {
  success: boolean
  message: string
  data: T
  error?: { code?: string; detail?: string }
}

interface BackendNotification {
  id: string
  targetUserId?: string | null
  category: 'ORDER' | 'SYSTEM' | 'STORE' | 'AI' | 'SECURITY'
  title: string
  body: string
  read: boolean
  createdAt: string
}

/**
 * Executes normalize category.
 *
 * @param value The value value.
 * @returns The result of normalize category.
 */
const normalizeCategory = (value: BackendNotification['category']): NotificationCategory => {
  switch (value) {
    case 'ORDER':
      return 'order'
    case 'STORE':
      return 'store'
    case 'AI':
      return 'ai'
    case 'SECURITY':
      return 'system'
    default:
      return 'system'
  }
}

/**
 * Maps source data to NotificationMessage.
 *
 * @param item The source object to transform.
 * @returns The mapped DTO representation.
 */
const toNotificationMessage = (item: BackendNotification): NotificationMessage => ({
  id: item.id,
  title: item.title,
  description: item.body,
  category: normalizeCategory(item.category),
  createdAt: item.createdAt,
  read: item.read,
})

/**
 * Executes unwrap.
 *
 * @param payload The payload value.
 * @returns The result of unwrap.
 */
const unwrap = <T>(payload: BackendEnvelope<T>): T => {
  if (!payload.success) {
    throw new Error(payload.error?.detail ?? payload.message ?? 'Notification API request failed')
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
  return 'Notification API request failed'
}

export const notificationsApi = {
  myNotifications: async (): Promise<NotificationMessage[]> => {
    try {
      const response = await axiosClient.get<BackendEnvelope<BackendNotification[]>>('/notifications/me')
      return unwrap(response.data).map(toNotificationMessage)
    } catch (error) {
      throw new Error(extractApiMessage(error))
    }
  },
  unreadCount: async (): Promise<number> => {
    try {
      const response = await axiosClient.get<BackendEnvelope<number>>('/notifications/me/unread-count')
      return unwrap(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error))
    }
  },
  markAsRead: async (notificationId: string): Promise<NotificationMessage> => {
    try {
      const response = await axiosClient.patch<BackendEnvelope<BackendNotification>>(
        `/notifications/${notificationId}/read`,
      )
      return toNotificationMessage(unwrap(response.data))
    } catch (error) {
      throw new Error(extractApiMessage(error))
    }
  },
  markAllAsRead: async (): Promise<number> => {
    try {
      const response = await axiosClient.patch<BackendEnvelope<number>>('/notifications/me/read-all')
      return unwrap(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error))
    }
  },
}

