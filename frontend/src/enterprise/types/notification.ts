export type NotificationCategory = 'order' | 'system' | 'promotion' | 'ai' | 'store'

export interface NotificationMessage {
  id: string
  title: string
  description: string
  category: NotificationCategory
  createdAt: string
  read: boolean
}
