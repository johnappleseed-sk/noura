import { env } from '@/config/env'
import { NotificationMessage } from '@/types'

type NotificationHandler = (message: NotificationMessage) => void

export class RealtimeNotificationsClient {
  private socket: WebSocket | null = null

  /**
   * Executes start.
   *
   * @param onMessage The on message value.
   * @returns No value.
   */
  public start(onMessage: NotificationHandler): void {
    if (!env.enableRealtimeNotifications) {
      return
    }

    if (env.websocketUrl) {
      this.socket = new WebSocket(env.websocketUrl)
      this.socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data as string) as Partial<NotificationMessage>
          onMessage({
            id: payload.id ?? `ws-${Date.now()}`,
            title: payload.title ?? 'Notification',
            description: payload.description ?? 'Real-time update received.',
            category: payload.category ?? 'system',
            createdAt: payload.createdAt ?? new Date().toISOString(),
            read: false,
          })
        } catch {
          onMessage({
            id: `ws-fallback-${Date.now()}`,
            title: 'Notification',
            description: 'A real-time event was received.',
            category: 'system',
            createdAt: new Date().toISOString(),
            read: false,
          })
        }
      }
      return
    }
  }

  /**
   * Executes stop.
   *
   * @returns No value.
   */
  public stop(): void {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
  }
}
