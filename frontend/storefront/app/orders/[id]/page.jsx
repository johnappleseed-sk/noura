'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { cancelOrder, captureOrderPayment, getOrder, getOrderPayments, getOrderFulfillment, resolveCustomerToken } from '@/lib/api'
import { formatCurrency } from '@/lib/format'

export default function OrderDetailPage() {
  const params = useParams()
  const router = useRouter()
  const orderId = params?.id
  const [token, setToken] = useState(null)
  const [order, setOrder] = useState(null)
  const [payments, setPayments] = useState([])
  const [error, setError] = useState('')
  const [processingPaymentId, setProcessingPaymentId] = useState(null)
  const [cancellingOrder, setCancellingOrder] = useState(false)
  const [fulfillment, setFulfillment] = useState(null)

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) {
      setError('Please sign in to see order details.')
      return
    }
    setToken(currentToken)
    if (!orderId) {
      setError('Invalid order id.')
      return
    }
    ;(async () => {
      try {
        const normalizedId = Number.parseInt(orderId, 10)
        if (!Number.isFinite(normalizedId) || normalizedId <= 0) {
          throw new Error('Invalid order id.')
        }
        const [orderData, paymentData, fulfillmentData] = await Promise.all([
          getOrder(currentToken, normalizedId),
          getOrderPayments(currentToken, normalizedId),
          getOrderFulfillment(currentToken, normalizedId)
        ])
        setOrder(orderData)
        setPayments(Array.isArray(paymentData) ? paymentData : [])
        setFulfillment(fulfillmentData || null)
      } catch (e) {
        setError(e.message || 'Unable to load order.')
      }
    })()
  }, [orderId])

  const refreshPayments = async () => {
    if (!token || !order) return
    try {
      const data = await getOrderPayments(token, order.id)
      setPayments(Array.isArray(data) ? data : [])
      const refreshedOrder = await getOrder(token, order.id)
      setOrder(refreshedOrder)
      const refreshedFulfillment = await getOrderFulfillment(token, order.id)
      setFulfillment(refreshedFulfillment || null)
    } catch (e) {
      setError(e.message || 'Unable to refresh payment status.')
    }
  }

  const handleCapture = async (paymentId) => {
    if (!token || !order) return
    setProcessingPaymentId(paymentId)
    try {
      await captureOrderPayment(token, order.id, paymentId)
      await refreshPayments()
    } catch (e) {
      setError(e.message || 'Unable to capture payment.')
    } finally {
      setProcessingPaymentId(null)
    }
  }

  const handleCancelOrder = async () => {
    if (!token || !order) return
    setCancellingOrder(true)
    try {
      const updated = await cancelOrder(token, order.id)
      setOrder(updated)
      await refreshPayments()
    } catch (e) {
      setError(e.message || 'Unable to cancel order.')
    } finally {
      setCancellingOrder(false)
    }
  }

  if (error) {
    return (
      <section className="section">
        <div className="notice panel">
          <p>{error}</p>
          <Link href="/auth/login" className="button primary">
            Sign in
          </Link>
          <button className="button ghost" type="button" onClick={() => router.push('/orders')}>
            Back to orders
          </button>
        </div>
      </section>
    )
  }

  if (!token || order === null) {
    return (
      <section className="section">
        <div className="notice panel">Loading order...</div>
      </section>
    )
  }

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Order</span>
          <h1>{order.orderNumber || `Order #${order.id}`}</h1>
        </div>
        <Link href="/orders" className="button ghost">
          Back to orders
        </Link>
      </div>

        <article className="product-card product-detail">
        <div className="product-meta">
          <p>State: {order.status}</p>
          <p>Placed: {order.placedAt ? new Date(order.placedAt).toLocaleString() : 'Pending'}</p>
          <p>Currency: {order.currencyCode || 'USD'}</p>
          <p>Subtotal: {formatCurrency(order.subtotal || 0, order.currencyCode || 'USD')}</p>
          <p>Discount: {formatCurrency(order.discountTotal || 0, order.currencyCode || 'USD')}</p>
          <p>Shipping: {formatCurrency(order.shippingTotal || 0, order.currencyCode || 'USD')}</p>
          <p>Tax: {formatCurrency(order.taxTotal || 0, order.currencyCode || 'USD')}</p>
          <strong>Total: {formatCurrency(order.grandTotal || 0, order.currencyCode || 'USD')}</strong>
          {order.latestPayment ? (
            <div className="product-meta">
              <h3>Payment</h3>
              <p>Method: {order.latestPayment.paymentMethod}</p>
              <p>Status: {order.latestPayment.status}</p>
              <p>Provider: {order.latestPayment.provider || 'N/A'}</p>
              <p>Reference: {order.latestPayment.providerReference || 'N/A'}</p>
              <p>
                Amount: {formatCurrency(order.latestPayment.amount || 0, order.currencyCode || 'USD')}
              </p>
            </div>
          ) : null}
          <div className="hero-actions">
            <button type="button" className="button ghost" onClick={refreshPayments}>
              Refresh payments
            </button>
            {['DRAFT', 'PENDING_PAYMENT', 'PAID', 'PROCESSING'].includes(order.status) ? (
              <button
                type="button"
                className="button ghost"
                disabled={cancellingOrder}
                onClick={handleCancelOrder}
              >
                {cancellingOrder ? 'Cancelling...' : 'Cancel order'}
              </button>
            ) : null}
          </div>
          {order.shippingAddress ? (
            <div className="product-meta">
              <h3>Shipping address</h3>
              <p>{order.shippingAddress.recipientName || 'Recipient not set'}</p>
              <p>{order.shippingAddress.phone || ''}</p>
              <p>{[order.shippingAddress.line1, order.shippingAddress.line2].filter(Boolean).join(', ')}</p>
              <p>{[order.shippingAddress.district, order.shippingAddress.city, order.shippingAddress.stateProvince]
                .filter(Boolean)
                .join(', ')}</p>
              <p>{[order.shippingAddress.postalCode, order.shippingAddress.countryCode].filter(Boolean).join(' ')}</p>
            </div>
          ) : null}
          {fulfillment ? (
            <div className="product-meta">
              <h3>Fulfillment</h3>
              <p>Status: {fulfillment.status}</p>
              <p>Carrier: {fulfillment.carrier || 'N/A'}</p>
              <p>Tracking: {fulfillment.trackingNumber || 'N/A'}</p>
              <p>Tracking URL: {fulfillment.trackingUrl || 'N/A'}</p>
              <p>
                Estimated delivery:{' '}
                {fulfillment.estimatedDeliveryAt ? new Date(fulfillment.estimatedDeliveryAt).toLocaleString() : 'N/A'}
              </p>
              <p>
                Shipped at:{' '}
                {fulfillment.shippedAt ? new Date(fulfillment.shippedAt).toLocaleString() : 'N/A'}
              </p>
              <p>
                Delivered at:{' '}
                {fulfillment.deliveredAt ? new Date(fulfillment.deliveredAt).toLocaleString() : 'N/A'}
              </p>
              <p>Notes: {fulfillment.notes || 'N/A'}</p>
            </div>
          ) : null}
        </div>
      </article>

      <div className="section-head">
        <h2>Items</h2>
      </div>
      <div className="product-grid">
        {(order.items || []).map((item) => (
          <article key={item.id} className="product-card">
            <div className="product-meta">
              <strong>{item.productName || `Product #${item.productId}`}</strong>
              <p>SKU {item.sku || 'N/A'}</p>
              <small>Unit {item.unitLabel || 'piece'}</small>
              <small>Qty: {item.quantity}</small>
              <small>Unit Price: {formatCurrency(item.unitPrice || 0, order.currencyCode || 'USD')}</small>
              <strong>{formatCurrency(item.lineTotal || 0, order.currencyCode || 'USD')}</strong>
            </div>
          </article>
        ))}
      </div>

      <div className="section-head">
        <h2>Payment history</h2>
      </div>
      {payments.length === 0 ? (
        <div className="panel notice">No payment records yet.</div>
      ) : (
        <div className="product-grid">
          {payments.map((payment) => (
            <article key={payment.id} className="product-card">
              <div className="product-meta">
                <strong>{payment.paymentMethod || 'UNKNOWN'}</strong>
                <p>Status: {payment.status}</p>
                <p>Provider: {payment.provider || 'N/A'}</p>
                <p>Reference: {payment.providerReference || 'N/A'}</p>
                <p>Failure reason: {payment.failureReason || '—'}</p>
                <p>
                  Amount: {formatCurrency(payment.amount || 0, payment.currencyCode || order.currencyCode || 'USD')}
                </p>
                <div className="hero-actions">
                  {(payment.status === 'PENDING' || payment.status === 'AUTHORIZED') ? (
                    <button
                      type="button"
                      className="button primary"
                      disabled={processingPaymentId === payment.id}
                      onClick={() => handleCapture(payment.id)}
                    >
                      {processingPaymentId === payment.id ? 'Capturing...' : 'Capture'}
                    </button>
                  ) : null}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
