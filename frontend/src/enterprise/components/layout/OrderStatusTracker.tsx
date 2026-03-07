import { useEffect, useRef, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'

/**
 * Renders the OrderStatusTracker component.
 *
 * @returns The rendered component tree.
 */
export const OrderStatusTracker = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const user = useAppSelector((state) => state.auth.user)
  const ordersState = useAppSelector((state) => state.orders)

  useEffect(() => {
    if (!open || !user) {
      return
    }
    if (user.role === 'admin') {
      void dispatch(fetchAdminOrders())
      return
    }
    void dispatch(fetchMyOrders())
  }, [dispatch, open, user])

  useEffect(() => {
    /**
     * Executes close on outside.
     *
     * @param event The event value.
     * @returns No value.
     */
    const closeOnOutside = (event: MouseEvent): void => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    /**
     * Executes close on escape.
     *
     * @param event The event value.
     * @returns No value.
     */
    const closeOnEscape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    if (open) {
      window.addEventListener('click', closeOnOutside)
      window.addEventListener('keydown', closeOnEscape)
    }
    return () => {
      window.removeEventListener('click', closeOnOutside)
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Track order status"
        className="m3-btn m3-btn-outlined !px-4 !py-2 !text-xs uppercase tracking-wide"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        Track Orders
      </button>

      {open ? (
        <section aria-label="Order status tracker" className="panel-high absolute right-0 z-40 mt-2 w-80 p-3" role="menu">
          <h2 className="mb-2 text-sm font-semibold">Order status tracker</h2>
          {ordersState.status === 'loading' ? <p className="m3-subtitle text-xs">Loading orders...</p> : null}
          {ordersState.error ? <p className="text-xs text-rose-600">{ordersState.error}</p> : null}
          {ordersState.status !== 'loading' && ordersState.items.length === 0 ? (
            <p className="m3-subtitle text-xs">No order history available yet.</p>
          ) : null}
          <ul className="space-y-2">
            {ordersState.items.slice(0, 8).map((order) => (
              <li className="rounded-2xl p-3" key={order.id} style={{ background: 'var(--m3-surface-container-high)' }}>
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs font-semibold">{order.id}</p>
                  <span className="m3-chip text-[10px] uppercase">{order.status}</span>
                </div>
                <p className="m3-subtitle mt-1 text-xs">{new Date(order.createdAt).toLocaleString()}</p>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  )
}
