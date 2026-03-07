import { useEffect, useRef } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  closeNotificationsPanel,
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsReadRemote,
  markNotificationRead,
  markAllNotificationsRead,
  selectNotifications,
  toggleNotificationsPanel,
} from '@/features/notifications/notificationsSlice'

/**
 * Renders the NotificationCenter component.
 *
 * @returns The rendered component tree.
 */
export const NotificationCenter = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const { items, unreadCount, isOpen } = useAppSelector(selectNotifications)
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    void dispatch(fetchUnreadCount())
    const timer = window.setInterval(() => {
      void dispatch(fetchUnreadCount())
    }, 10000)
    return () => {
      window.clearInterval(timer)
    }
  }, [dispatch])

  useEffect(() => {
    /**
     * Executes on window click.
     *
     * @param event The event value.
     * @returns No value.
     */
    const onWindowClick = (event: MouseEvent): void => {
      if (!containerRef.current?.contains(event.target as Node)) {
        dispatch(closeNotificationsPanel())
      }
    }

    /**
     * Executes on escape.
     *
     * @param event The event value.
     * @returns No value.
     */
    const onEscape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        dispatch(closeNotificationsPanel())
      }
    }

    if (isOpen) {
      void dispatch(fetchNotifications())
      window.addEventListener('click', onWindowClick)
      window.addEventListener('keydown', onEscape)
    }
    return () => {
      window.removeEventListener('click', onWindowClick)
      window.removeEventListener('keydown', onEscape)
    }
  }, [dispatch, isOpen])

  useEffect(() => {
    if (!isOpen) {
      return
    }
    const timer = window.setInterval(() => {
      void dispatch(fetchNotifications())
    }, 8000)
    return () => {
      window.clearInterval(timer)
    }
  }, [dispatch, isOpen])

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        aria-label={`Notifications ${unreadCount > 0 ? `(${unreadCount} unread)` : ''}`}
        className="m3-icon-btn relative"
        onClick={() => dispatch(toggleNotificationsPanel())}
        type="button"
      >
        Bell
        {unreadCount > 0 ? (
          <span className="absolute -right-1 -top-1 flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-semibold text-white">
            {unreadCount}
          </span>
        ) : null}
      </button>

      {isOpen ? (
        <section
          aria-label="Notifications panel"
          className="panel-high absolute right-0 z-40 mt-2 w-80 p-3"
          role="menu"
        >
          <header className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Real-time notifications</h2>
            <button
              className="m3-link text-xs"
              onClick={() => {
                dispatch(markAllNotificationsRead())
                void dispatch(markAllNotificationsReadRemote())
              }}
              type="button"
            >
              Mark all read
            </button>
          </header>

          <ul className="max-h-72 space-y-2 overflow-y-auto">
            {items.length === 0 ? (
              <li className="m3-subtitle rounded-2xl p-3 text-xs">No notifications yet.</li>
            ) : (
              items.map((item) => (
                <li
                  className="rounded-2xl p-3 text-xs"
                  key={item.id}
                  onClick={() => {
                    if (!item.read) {
                      void dispatch(markNotificationRead(item.id))
                    }
                  }}
                  role="button"
                  style={{ background: 'var(--m3-surface-container-high)' }}
                >
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-semibold">{item.title}</p>
                    <span className="m3-chip text-[10px] uppercase">{item.category}</span>
                  </div>
                  <p className="m3-subtitle mt-1">{item.description}</p>
                  <p className="mt-1 text-[10px]" style={{ color: 'var(--m3-on-surface-variant)' }}>
                    {new Date(item.createdAt).toLocaleTimeString()}
                  </p>
                </li>
              ))
            )}
          </ul>
        </section>
      ) : null}
    </div>
  )
}
