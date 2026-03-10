'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import {
  clearCart,
  getCart,
  getCustomerAddresses,
  addCartItem,
  updateCartItem,
  removeCartItem,
  resolveCustomerToken,
  checkoutCart,
  applyCoupon
} from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import { Breadcrumbs } from '@/components/navigation'
import { trackAnalyticsEvent } from '@/lib/analytics'

function CartItem({ item, onUpdate, onRemove, disabled }) {
  return (
    <div className="cart-item">
      <div className="cart-item-image" style={item.imageUrl ? { backgroundImage: `url(${item.imageUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}>
        {!item.imageUrl && <span style={{ color: 'var(--muted)', fontSize: '0.8rem' }}>Product</span>}
      </div>
      <div className="cart-item-info">
        <strong>{item.productName || `Product #${item.productId.slice(0, 8)}`}</strong>
        <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>SKU: {item.sku || 'N/A'}</span>
        <span style={{ fontWeight: 600 }}>{formatCurrency(item.lineTotal || 0)}</span>
      </div>
      <div className="cart-item-actions">
        <div className="qty-control">
          <button type="button" disabled={disabled || item.quantity <= 1} onClick={() => onUpdate(item.id, item.quantity - 1)}>−</button>
          <span>{item.quantity}</span>
          <button type="button" disabled={disabled} onClick={() => onUpdate(item.id, item.quantity + 1)}>+</button>
        </div>
        <button type="button" className="button ghost sm" disabled={disabled} onClick={() => onRemove(item.id)}>Remove</button>
      </div>
    </div>
  )
}

export default function CartPage() {
  const router = useRouter()
  const [token, setToken] = useState(null)
  const [cart, setCart] = useState(null)
  const [addresses, setAddresses] = useState([])
  const [selectedAddressId, setSelectedAddressId] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH_ON_DELIVERY')
  const [paymentProvider, setPaymentProvider] = useState('')
  const [paymentProviderReference, setPaymentProviderReference] = useState('')
  const [couponCode, setCouponCode] = useState('')
  const [couponMsg, setCouponMsg] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showCheckout, setShowCheckout] = useState(false)

  const loadCheckoutData = async (activeToken) => {
    setError('')
    try {
      const [current, customerAddresses] = await Promise.all([
        getCart(activeToken),
        getCustomerAddresses(activeToken)
      ])
      setCart(current)
      setAddresses(Array.isArray(customerAddresses) ? customerAddresses : [])
      const defaultShipping = customerAddresses?.find((a) => a?.defaultShipping)
      if (!selectedAddressId || !customerAddresses?.some((a) => String(a.id) === String(selectedAddressId))) {
        if (defaultShipping?.id) setSelectedAddressId(String(defaultShipping.id))
        else if (customerAddresses?.length > 0) setSelectedAddressId(String(customerAddresses[0].id))
        else setSelectedAddressId('')
      }
    } catch (e) {
      setError(e.message || 'Unable to load cart.')
    }
  }

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) {
      setError('Please sign in to view your cart.')
      return
    }
    setToken(currentToken)
    loadCheckoutData(currentToken)
  }, [])

  const total = useMemo(() => cart?.subtotal || 0, [cart])
  const itemCount = useMemo(() => cart?.items?.reduce((s, i) => s + i.quantity, 0) || 0, [cart])

  const handleUpdate = async (itemId, nextQty) => {
    if (!token) return
    setLoading(true)
    try {
      await updateCartItem(token, itemId, { quantity: nextQty })
      await loadCheckoutData(token)
    } catch (e) {
      setError(e.message || 'Unable to update item.')
    } finally {
      setLoading(false)
    }
  }

  const handleRemove = async (itemId) => {
    if (!token) return
    setLoading(true)
    try {
      await removeCartItem(token, itemId)
      await loadCheckoutData(token)
    } catch (e) {
      setError(e.message || 'Unable to remove item.')
    } finally {
      setLoading(false)
    }
  }

  const handleApplyCoupon = async () => {
    if (!token || !couponCode.trim()) return
    setCouponMsg('')
    try {
      await applyCoupon(token, couponCode.trim())
      setCouponMsg('Coupon applied!')
      await loadCheckoutData(token)
    } catch (e) {
      setCouponMsg(e.message || 'Invalid coupon code.')
    }
  }

  const handleCheckout = async () => {
    if (!token) return
    setLoading(true)
    try {
      await trackAnalyticsEvent({
        eventType: 'CHECKOUT_STARTED',
        pagePath: '/cart'
      })
      const shippingAddress = addresses.find((a) => String(a.id) === String(selectedAddressId)) || null
      await checkoutCart(token, {
        shippingAddress,
        paymentMethod,
        paymentProvider: paymentProvider.trim() || null,
        paymentProviderReference: paymentProviderReference.trim() || null
      })
      await loadCheckoutData(token)
      router.push('/orders')
    } catch (e) {
      setError(e.message || 'Checkout failed.')
    } finally {
      setLoading(false)
    }
  }

  const handleClear = async () => {
    if (!token) return
    setLoading(true)
    try {
      await clearCart(token)
      await loadCheckoutData(token)
    } catch (e) {
      setError(e.message || 'Unable to clear cart.')
    } finally {
      setLoading(false)
    }
  }

  const selectedAddress = addresses.find((a) => String(a.id) === String(selectedAddressId)) || null

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Cart' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
            <h1 style={{ margin: 0 }}>Shopping Cart {cart && <span style={{ fontWeight: 400, color: 'var(--muted)', fontSize: '1rem' }}>({itemCount} items)</span>}</h1>
            {cart && cart.items.length > 0 && (
              <button type="button" className="button ghost sm" onClick={handleClear} disabled={loading}>Clear Cart</button>
            )}
          </div>

          {error && (
            <div className="panel" style={{ padding: 16, marginBottom: 20, borderLeft: '3px solid var(--danger)', background: '#fef2f2' }}>
              <p style={{ margin: 0, color: 'var(--danger)' }}>{error}</p>
            </div>
          )}

          {!token ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <h2>Sign in to view your cart</h2>
              <p style={{ color: 'var(--muted)', marginBottom: 20 }}>You need an account to add items and checkout.</p>
              <Link href="/auth/login" className="button primary">Sign In</Link>
            </div>
          ) : !cart ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)' }}>Loading your cart...</p>
            </div>
          ) : cart.items.length === 0 ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <h2 style={{ marginBottom: 8 }}>Your cart is empty</h2>
              <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Looks like you haven&apos;t added anything yet.</p>
              <Link href="/products" className="button primary">Browse Products</Link>
            </div>
          ) : (
            <div className="cart-layout">
              {/* Cart Items */}
              <div className="cart-items-section">
                {cart.items.map((item) => (
                  <CartItem key={item.id} item={item} onUpdate={handleUpdate} onRemove={handleRemove} disabled={loading} />
                ))}
                <div style={{ paddingTop: 16 }}>
                  <Link href="/products" className="button ghost">← Continue Shopping</Link>
                </div>
              </div>

              {/* Cart Summary Sidebar */}
              <div className="cart-summary">
                <div className="panel" style={{ padding: 24, position: 'sticky', top: 80 }}>
                  <h3 style={{ marginTop: 0, marginBottom: 20 }}>Order Summary</h3>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ color: 'var(--muted)' }}>Subtotal ({itemCount} items)</span>
                    <strong>{formatCurrency(total)}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ color: 'var(--muted)' }}>Shipping</span>
                    <span style={{ color: 'var(--accent-alt)' }}>Free</span>
                  </div>

                  {/* Coupon */}
                  <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, marginTop: 16, marginBottom: 16 }}>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <input type="text" className="form-input" placeholder="Coupon code" value={couponCode} onChange={(e) => setCouponCode(e.target.value)} style={{ flex: 1 }} />
                      <button type="button" className="button ghost sm" onClick={handleApplyCoupon}>Apply</button>
                    </div>
                    {couponMsg && <small style={{ color: couponMsg.includes('applied') ? 'var(--success)' : 'var(--danger)', marginTop: 4, display: 'block' }}>{couponMsg}</small>}
                  </div>

                  <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, display: 'flex', justifyContent: 'space-between', fontSize: '1.15rem' }}>
                    <strong>Total</strong>
                    <strong>{formatCurrency(total)}</strong>
                  </div>

                  {!showCheckout ? (
                    <button type="button" className="button primary lg" style={{ width: '100%', marginTop: 20 }} onClick={() => setShowCheckout(true)} disabled={loading}>
                      Proceed to Checkout
                    </button>
                  ) : (
                    <div style={{ marginTop: 20 }}>
                      {/* Shipping Address */}
                      <div style={{ marginBottom: 16 }}>
                        <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Shipping Address</label>
                        {addresses.length === 0 ? (
                          <div>
                            <p style={{ fontSize: '0.85rem', color: 'var(--muted)', margin: '0 0 8px' }}>No addresses found.</p>
                            <Link href="/account/addresses" className="button ghost sm">Add Address</Link>
                          </div>
                        ) : (
                          <>
                            <select className="form-input" value={selectedAddressId} onChange={(e) => setSelectedAddressId(e.target.value)}>
                              {addresses.map((a) => (
                                <option key={a.id} value={String(a.id)}>
                                  {a.label || `${a.recipientName} - ${a.line1}`}
                                </option>
                              ))}
                            </select>
                            {selectedAddress && (
                              <small style={{ display: 'block', marginTop: 4, color: 'var(--muted)' }}>
                                {[selectedAddress.recipientName, selectedAddress.line1, selectedAddress.city, selectedAddress.postalCode].filter(Boolean).join(', ')}
                              </small>
                            )}
                          </>
                        )}
                      </div>

                      {/* Payment */}
                      <div style={{ marginBottom: 16 }}>
                        <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Payment Method</label>
                        <select className="form-input" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                          <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
                          <option value="CREDIT_CARD">Credit Card</option>
                          <option value="BANK_TRANSFER">Bank Transfer</option>
                          <option value="WALLET">Wallet</option>
                        </select>
                      </div>

                      {paymentMethod !== 'CASH_ON_DELIVERY' && (
                        <div style={{ marginBottom: 16 }}>
                          <input type="text" className="form-input" placeholder="Payment Provider" value={paymentProvider} onChange={(e) => setPaymentProvider(e.target.value)} style={{ marginBottom: 8 }} />
                          <input type="text" className="form-input" placeholder="Reference (optional)" value={paymentProviderReference} onChange={(e) => setPaymentProviderReference(e.target.value)} />
                        </div>
                      )}

                      <button type="button" className="button primary lg" style={{ width: '100%' }} onClick={handleCheckout} disabled={loading || !selectedAddressId}>
                        {loading ? 'Processing...' : 'Place Order'}
                      </button>

                      <button type="button" className="button ghost sm" style={{ width: '100%', marginTop: 8 }} onClick={() => setShowCheckout(false)}>
                        ← Back to Cart
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </section>
    </>
  )
}
