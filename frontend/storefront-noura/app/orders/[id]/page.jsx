'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useEffect, useState } from 'react'
import { getOrder, getOrderTimeline, resolveCustomerToken, quickReorder } from '@/lib/api'
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

export default function OrderDetailPage() {
  const params = useParams()
  const orderId = params?.id
  const [order, setOrder] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [error, setError] = useState('')
  const [reordering, setReordering] = useState(false)
  const [token, setToken] = useState(null)

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) { setError('Please sign in to see order details.'); return }
    if (!orderId) { setError('Invalid order id.'); return }
    setToken(currentToken)

    ;(async () => {
      try {
        const [orderData, timelineData] = await Promise.allSettled([
          getOrder(currentToken, orderId),
          getOrderTimeline(currentToken, orderId)
        ])
        if (orderData.status === 'fulfilled') setOrder(orderData.value)
        else setError(orderData.reason?.message || 'Unable to load order.')
        if (timelineData.status === 'fulfilled') setTimeline(Array.isArray(timelineData.value) ? timelineData.value : [])
      } catch (e) {
        setError(e.message || 'Unable to load order.')
      }
    })()
  }, [orderId])

  const handleReorder = async () => {
    if (!token || !orderId) return
    setReordering(true)
    try {
      await quickReorder(token, orderId)
      window.location.href = '/cart'
    } catch { /* ignore */ }
    setReordering(false)
  }

  if (error) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}>
          <div className="container"><Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Orders', href: '/orders' }, { label: 'Error' }]} /></div>
        </section>
        <section className="featured-section"><div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}><div className="panel" style={{ padding: 40, textAlign: 'center' }}><p style={{ color: 'var(--danger)' }}>{error}</p><div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}><Link href="/auth/login" className="button primary">Sign In</Link><Link href="/orders" className="button ghost">Back to Orders</Link></div></div></div></section>
      </>
    )
  }

  if (!order) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}>
          <div className="container"><Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Orders', href: '/orders' }]} /></div>
        </section>
        <section className="featured-section"><div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}><div className="panel" style={{ padding: 40, textAlign: 'center' }}><p style={{ color: 'var(--muted)' }}>Loading order...</p></div></div></section>
      </>
    )
  }

  const curr = order.currencyCode || 'USD'

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Orders', href: '/orders' }, { label: order.orderNumber || `#${order.id}` }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16, marginBottom: 32 }}>
            <div>
              <h1 style={{ margin: '0 0 4px' }}>{order.orderNumber || `Order #${order.id}`}</h1>
              <p style={{ color: 'var(--muted)', margin: 0 }}>
                Placed {order.placedAt ? new Date(order.placedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Pending'}
              </p>
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <Badge variant={statusColor(order.status)}>{order.status || 'PENDING'}</Badge>
              <button type="button" className="button ghost sm" onClick={handleReorder} disabled={reordering}>{reordering ? 'Adding...' : 'Reorder'}</button>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
            {/* Order Summary */}
            <div className="panel" style={{ padding: 24 }}>
              <h3 style={{ marginTop: 0, marginBottom: 16 }}>Order Summary</h3>
              <dl style={{ display: 'grid', gap: 8, margin: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--muted)' }}>Subtotal</span>
                  <span>{formatCurrency(order.subtotal || 0, curr)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--muted)' }}>Discount</span>
                  <span style={{ color: 'var(--accent-alt)' }}>−{formatCurrency(order.discountTotal || 0, curr)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--muted)' }}>Shipping</span>
                  <span>{formatCurrency(order.shippingTotal || 0, curr)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--muted)' }}>Tax</span>
                  <span>{formatCurrency(order.taxTotal || 0, curr)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--line)', paddingTop: 8, fontWeight: 700, fontSize: '1.1rem' }}>
                  <span>Total</span>
                  <span>{formatCurrency(order.grandTotal || 0, curr)}</span>
                </div>
              </dl>
            </div>

            {/* Payment & Shipping */}
            <div className="panel" style={{ padding: 24 }}>
              <h3 style={{ marginTop: 0, marginBottom: 16 }}>Details</h3>
              {order.paymentMethod && (
                <div style={{ marginBottom: 12 }}>
                  <small style={{ color: 'var(--muted)', textTransform: 'uppercase', fontSize: '0.7rem', letterSpacing: '0.05em' }}>Payment</small>
                  <p style={{ margin: '2px 0 0', fontWeight: 600 }}>{order.paymentMethod.replace(/_/g, ' ')}</p>
                </div>
              )}
              <div>
                <small style={{ color: 'var(--muted)', textTransform: 'uppercase', fontSize: '0.7rem', letterSpacing: '0.05em' }}>Currency</small>
                <p style={{ margin: '2px 0 0', fontWeight: 600 }}>{curr}</p>
              </div>
            </div>
          </div>

          {/* Timeline */}
          {timeline.length > 0 && (
            <div style={{ marginTop: 32 }}>
              <h2 style={{ marginBottom: 16 }}>Order Timeline</h2>
              <div className="timeline">
                {timeline.map((event, i) => (
                  <div key={i} className="timeline-event">
                    <div className="event-dot" />
                    <div>
                      <div className="event-title">{event.title || event.status || event.event || 'Update'}</div>
                      <div className="event-desc">{event.description || event.note || ''}</div>
                      {event.createdAt && <small style={{ color: 'var(--muted-light)' }}>{new Date(event.createdAt).toLocaleString()}</small>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Order Items */}
          <div style={{ marginTop: 32 }}>
            <h2 style={{ marginBottom: 16 }}>Items ({order.items?.length || 0})</h2>
            <div style={{ display: 'grid', gap: 12 }}>
              {(order.items || []).map((item) => (
                <div key={item.id} className="panel" style={{ padding: 16, display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 16, alignItems: 'center' }}>
                  <div>
                    <strong>{item.productName || `Product #${item.productId}`}</strong>
                    {item.sku && <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>SKU: {item.sku}</div>}
                  </div>
                  <span style={{ color: 'var(--muted)' }}>×{item.quantity}</span>
                  <strong>{formatCurrency(item.lineTotal || 0, curr)}</strong>
                </div>
              ))}
            </div>
          </div>

          <div style={{ marginTop: 32 }}>
            <Link href="/orders" className="button ghost">← Back to Orders</Link>
          </div>
        </div>
      </section>
    </>
  )
}
