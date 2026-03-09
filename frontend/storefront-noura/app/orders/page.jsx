'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { getMyOrders, resolveCustomerToken, quickReorder } from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'

const statusColor = (s) => {
  if (!s) return 'neutral'
  const l = s.toUpperCase()
  if (l.includes('DELIVER') || l.includes('COMPLET')) return 'success'
  if (l.includes('CANCEL') || l.includes('FAIL')) return 'danger'
  if (l.includes('SHIP') || l.includes('PROCESS')) return 'info'
  return 'warning'
}

export default function OrdersPage() {
  const [token, setToken] = useState('')
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')
  const [reordering, setReordering] = useState(null)

  const load = async (t) => {
    try {
      const data = await getMyOrders(t)
      setOrders(Array.isArray(data) ? data : data?.items || data?.content || [])
    } catch (e) {
      setError(e.message || 'Unable to load orders.')
    }
  }

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) { setError('Please sign in to see your orders.'); return }
    setToken(currentToken)
    load(currentToken)
  }, [])

  const handleReorder = async (orderId) => {
    if (!token) return
    setReordering(orderId)
    try {
      await quickReorder(token, orderId)
      window.location.href = '/cart'
    } catch { /* ignore */ }
    setReordering(null)
  }

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Orders' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}>
          <h1 style={{ marginBottom: 24 }}>My Orders</h1>

          {error && (
            <div className="panel" style={{ padding: 24, textAlign: 'center', marginBottom: 20 }}>
              <p style={{ color: 'var(--danger)' }}>{error}</p>
              <Link href="/auth/login" className="button primary">Sign In</Link>
            </div>
          )}

          {!error && orders === null && (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)' }}>Loading orders...</p>
            </div>
          )}

          {orders && orders.length === 0 && (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <h2 style={{ marginBottom: 8 }}>No orders yet</h2>
              <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Start shopping to see your orders here.</p>
              <Link href="/products" className="button primary">Browse Products</Link>
            </div>
          )}

          {orders && orders.length > 0 && (
            <div style={{ display: 'grid', gap: 16 }}>
              {orders.map((order) => (
                <div key={order.id} className="panel" style={{ padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12, marginBottom: 12 }}>
                    <div>
                      <strong style={{ fontSize: '1.05rem' }}>{order.orderNumber || `Order #${order.id}`}</strong>
                      <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>
                        {order.placedAt ? new Date(order.placedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Pending'}
                      </div>
                    </div>
                    <Badge variant={statusColor(order.status)}>{order.status || 'PENDING'}</Badge>
                  </div>

                  <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', marginBottom: 16, fontSize: '0.9rem' }}>
                    <div><span style={{ color: 'var(--muted)' }}>Items: </span><strong>{order.items?.length || '—'}</strong></div>
                    <div><span style={{ color: 'var(--muted)' }}>Total: </span><strong>{formatCurrency(order.grandTotal || 0)}</strong></div>
                    {order.paymentMethod && <div><span style={{ color: 'var(--muted)' }}>Payment: </span>{order.paymentMethod}</div>}
                  </div>

                  <div style={{ display: 'flex', gap: 8 }}>
                    <Link href={`/orders/${order.id}`} className="button primary sm">View Details</Link>
                    <button type="button" className="button ghost sm" disabled={reordering === order.id} onClick={() => handleReorder(order.id)}>
                      {reordering === order.id ? 'Adding...' : 'Reorder'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  )
}
