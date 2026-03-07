'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { getMyOrders, resolveCustomerToken } from '@/lib/api'
import { formatCurrency } from '@/lib/format'

export default function OrdersPage() {
  const router = useRouter()
  const [token, setToken] = useState('')
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) {
      setError('Please sign in to see your orders.')
      return
    }
    setToken(currentToken)
    ;(async () => {
      try {
        const data = await getMyOrders(currentToken)
        setOrders(data || [])
      } catch (e) {
        setError(e.message || 'Unable to load orders.')
      }
    })()
  }, [])

  if (error) {
    return (
      <section className="section">
        <div className="notice panel">
          <p>{error}</p>
          <Link href="/auth/login" className="button primary">
            Sign in
          </Link>
        </div>
      </section>
    )
  }

  if (orders === null) {
    return (
      <section className="section">
        <div className="notice panel">Loading orders...</div>
      </section>
    )
  }

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Orders</span>
          <h1>Your storefront orders</h1>
        </div>
      </div>

      {orders.length === 0 ? (
        <div className="notice panel">
          <p>No orders yet.</p>
          <Link href="/products" className="button ghost">
            Shop now
          </Link>
        </div>
      ) : (
        <div className="product-grid">
          {orders.map((order) => (
            <article key={order.id} className="product-card">
              <div className="product-meta">
                <span className="product-category">{order.status || 'PENDING'}</span>
                <strong>{order.orderNumber || `Order #${order.id}`}</strong>
                <p>{formatCurrency(order.grandTotal || 0)}</p>
                <small>{order.placedAt ? new Date(order.placedAt).toLocaleString() : 'Placement pending'}</small>
                <Link href={`/orders/${order.id}`} className="button ghost">
                  View order
                </Link>
              </div>
            </article>
          ))}
        </div>
      )}
      <button type="button" className="button ghost" onClick={() => router.refresh()}>
        Refresh
      </button>
    </section>
  )
}
