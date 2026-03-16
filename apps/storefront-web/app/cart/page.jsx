'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import {
  applyCoupon,
  checkoutCart,
  clearCart,
  getCart,
  getCustomerAddresses,
  removeCartItem,
  resolveCustomerToken,
  updateCartItem,
  validateServiceArea
} from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import { Breadcrumbs } from '@/components/navigation'
import Badge from '@/components/ui/Badge'
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

function validationPresentation(status) {
  switch (status) {
    case 'VALID':
      return { label: 'Deliverable', variant: 'success' }
    case 'OUT_OF_SERVICE_AREA':
      return { label: 'Outside service area', variant: 'danger' }
    case 'OUT_OF_STORE_RADIUS':
      return { label: 'Outside store radius', variant: 'warning' }
    case 'STORE_CLOSED':
      return { label: 'Store closed', variant: 'warning' }
    case 'STORE_UNAVAILABLE':
      return { label: 'Store unavailable', variant: 'danger' }
    default:
      return { label: 'Not verified', variant: 'neutral' }
  }
}

function liveEligibilityMessage(eligibility) {
  if (!eligibility) {
    return null
  }
  if (eligibility.isServiceAvailable) {
    return {
      tone: 'success',
      text: eligibility.distanceMeters != null
        ? `Delivery available. Matched store is ${eligibility.distanceMeters < 1000 ? `${eligibility.distanceMeters} m` : `${(eligibility.distanceMeters / 1000).toFixed(1)} km`} away.`
        : 'Delivery available for the selected address.'
    }
  }

  switch (eligibility.eligibilityReason) {
    case 'SERVICE_AREA_MISS':
      return { tone: 'danger', text: 'The selected address is outside all active delivery service areas.' }
    case 'OUT_OF_RANGE':
    case 'OUT_OF_STORE_RADIUS':
      return { tone: 'warning', text: 'The selected address is outside the delivery radius of nearby stores.' }
    case 'STORE_CLOSED':
      return { tone: 'warning', text: 'The matched store is currently closed. Delivery cannot be confirmed right now.' }
    case 'NO_STORE_AVAILABLE':
      return { tone: 'danger', text: 'No active delivery-capable store can serve this address at the moment.' }
    default:
      return { tone: 'danger', text: `Delivery validation failed: ${eligibility.eligibilityReason || 'unknown reason'}.` }
  }
}

function toneStyles(tone) {
  switch (tone) {
    case 'success':
      return { background: '#f0fdf4', borderLeft: '3px solid var(--success)', color: 'var(--success)' }
    case 'warning':
      return { background: '#fff7ed', borderLeft: '3px solid #c1672c', color: '#9a3412' }
    case 'danger':
      return { background: '#fef2f2', borderLeft: '3px solid var(--danger)', color: 'var(--danger)' }
    default:
      return { background: '#f8fafc', borderLeft: '3px solid var(--line)', color: 'var(--muted)' }
  }
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
  const [deliveryEligibility, setDeliveryEligibility] = useState(null)
  const [eligibilityLoading, setEligibilityLoading] = useState(false)

  const loadCheckoutData = async (activeToken) => {
    setError('')
    try {
      const [current, customerAddresses] = await Promise.all([
        getCart(activeToken),
        getCustomerAddresses(activeToken)
      ])
      const normalizedAddresses = Array.isArray(customerAddresses) ? customerAddresses : []
      setCart(current)
      setAddresses(normalizedAddresses)

      if (current?.addressId && normalizedAddresses.some((address) => String(address.id) === String(current.addressId))) {
        setSelectedAddressId(String(current.addressId))
        return
      }

      const defaultShipping = normalizedAddresses.find((address) => address?.defaultShipping)
      if (defaultShipping?.id) {
        setSelectedAddressId(String(defaultShipping.id))
      } else if (normalizedAddresses[0]?.id) {
        setSelectedAddressId(String(normalizedAddresses[0].id))
      } else {
        setSelectedAddressId('')
      }
    } catch (requestError) {
      setError(requestError.message || 'Unable to load cart.')
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

  const selectedAddress = useMemo(
    () => addresses.find((address) => String(address.id) === String(selectedAddressId)) || null,
    [addresses, selectedAddressId]
  )

  useEffect(() => {
    let active = true

    async function refreshEligibility() {
      if (selectedAddress?.latitude == null || selectedAddress?.longitude == null) {
        setDeliveryEligibility(null)
        return
      }

      setEligibilityLoading(true)
      try {
        const result = await validateServiceArea({
          latitude: selectedAddress.latitude,
          longitude: selectedAddress.longitude,
          serviceType: 'DELIVERY'
        })
        if (active) {
          setDeliveryEligibility(result)
        }
      } catch (requestError) {
        if (active) {
          setDeliveryEligibility(null)
          setError(requestError.message || 'Unable to validate delivery coverage.')
        }
      } finally {
        if (active) {
          setEligibilityLoading(false)
        }
      }
    }

    refreshEligibility()
    return () => {
      active = false
    }
  }, [selectedAddress])

  const total = useMemo(() => cart?.subtotal || 0, [cart])
  const itemCount = useMemo(() => cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0, [cart])
  const selectedAddressMeta = validationPresentation(selectedAddress?.validationStatus)
  const liveMessage = liveEligibilityMessage(deliveryEligibility)

  const handleUpdate = async (itemId, nextQty) => {
    if (!token) return
    setLoading(true)
    try {
      await updateCartItem(token, itemId, { quantity: nextQty })
      await loadCheckoutData(token)
    } catch (requestError) {
      setError(requestError.message || 'Unable to update item.')
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
    } catch (requestError) {
      setError(requestError.message || 'Unable to remove item.')
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
    } catch (requestError) {
      setCouponMsg(requestError.message || 'Invalid coupon code.')
    }
  }

  const handleCheckout = async () => {
    if (!token) return
    if (!selectedAddressId) {
      setError('Select a delivery address before checkout.')
      return
    }
    if (selectedAddress?.latitude == null || selectedAddress?.longitude == null) {
      setError('The selected address does not include coordinates. Update the address before checkout.')
      return
    }
    if (deliveryEligibility && !deliveryEligibility.isServiceAvailable) {
      setError(liveEligibilityMessage(deliveryEligibility)?.text || 'Delivery is currently unavailable for the selected address.')
      return
    }

    setLoading(true)
    setError('')
    try {
      await trackAnalyticsEvent({
        eventType: 'CHECKOUT_STARTED',
        pagePath: '/cart'
      })
      await checkoutCart(token, {
        fulfillmentMethod: 'DELIVERY',
        addressId: selectedAddressId,
        shippingAddress: selectedAddress,
        shippingAddressSnapshot: selectedAddress?.formattedAddress || null,
        paymentMethod,
        paymentProvider: paymentProvider.trim() || null,
        paymentProviderReference: paymentProviderReference.trim() || null
      })
      await loadCheckoutData(token)
      router.push('/orders')
    } catch (requestError) {
      setError(requestError.message || 'Checkout failed.')
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
    } catch (requestError) {
      setError(requestError.message || 'Unable to clear cart.')
    } finally {
      setLoading(false)
    }
  }

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
              <div className="cart-items-section">
                {cart.items.map((item) => (
                  <CartItem key={item.id} item={item} onUpdate={handleUpdate} onRemove={handleRemove} disabled={loading} />
                ))}
                <div style={{ paddingTop: 16 }}>
                  <Link href="/products" className="button ghost">← Continue Shopping</Link>
                </div>
              </div>

              <div className="cart-summary">
                <div className="panel" style={{ padding: 24, position: 'sticky', top: 80 }}>
                  <h3 style={{ marginTop: 0, marginBottom: 20 }}>Order Summary</h3>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ color: 'var(--muted)' }}>Subtotal ({itemCount} items)</span>
                    <strong>{formatCurrency(total)}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ color: 'var(--muted)' }}>Shipping</span>
                    <span style={{ color: 'var(--accent-alt)' }}>Calculated at checkout</span>
                  </div>

                  <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, marginTop: 16, marginBottom: 16 }}>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <input type="text" className="form-input" placeholder="Coupon code" value={couponCode} onChange={(event) => setCouponCode(event.target.value)} style={{ flex: 1 }} />
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
                      <div style={{ marginBottom: 16 }}>
                        <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Delivery Address</label>
                        {addresses.length === 0 ? (
                          <div>
                            <p style={{ fontSize: '0.85rem', color: 'var(--muted)', margin: '0 0 8px' }}>No addresses found.</p>
                            <Link href="/account/addresses" className="button ghost sm">Add Address</Link>
                          </div>
                        ) : (
                          <>
                            <select className="form-input" value={selectedAddressId} onChange={(event) => setSelectedAddressId(event.target.value)}>
                              {addresses.map((address) => (
                                <option key={address.id} value={String(address.id)}>
                                  {address.label || `${address.recipientName} - ${address.line1}`}
                                </option>
                              ))}
                            </select>
                            {selectedAddress && (
                              <div style={{ marginTop: 10, display: 'grid', gap: 8 }}>
                                <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                                  <Badge variant={selectedAddressMeta.variant}>{selectedAddressMeta.label}</Badge>
                                  {eligibilityLoading && <Badge variant="neutral">Checking coverage...</Badge>}
                                  {selectedAddress.defaultShipping && <Badge variant="info">Default</Badge>}
                                </div>
                                <small style={{ display: 'block', color: 'var(--muted)' }}>
                                  {selectedAddress.formattedAddress || [selectedAddress.recipientName, selectedAddress.line1, selectedAddress.city, selectedAddress.postalCode].filter(Boolean).join(', ')}
                                </small>
                                {liveMessage && (
                                  <div style={{ padding: 10, borderRadius: 'var(--radius-sm)', ...toneStyles(liveMessage.tone) }}>
                                    <small>{liveMessage.text}</small>
                                  </div>
                                )}
                              </div>
                            )}
                          </>
                        )}
                      </div>

                      <div style={{ marginBottom: 16 }}>
                        <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Payment Method</label>
                        <select className="form-input" value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}>
                          <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
                          <option value="CREDIT_CARD">Credit Card</option>
                          <option value="BANK_TRANSFER">Bank Transfer</option>
                          <option value="WALLET">Wallet</option>
                        </select>
                      </div>

                      {paymentMethod !== 'CASH_ON_DELIVERY' && (
                        <div style={{ marginBottom: 16 }}>
                          <input type="text" className="form-input" placeholder="Payment Provider" value={paymentProvider} onChange={(event) => setPaymentProvider(event.target.value)} style={{ marginBottom: 8 }} />
                          <input type="text" className="form-input" placeholder="Reference (optional)" value={paymentProviderReference} onChange={(event) => setPaymentProviderReference(event.target.value)} />
                        </div>
                      )}

                      <button
                        type="button"
                        className="button primary lg"
                        style={{ width: '100%' }}
                        onClick={handleCheckout}
                        disabled={
                          loading ||
                          !selectedAddressId ||
                          eligibilityLoading ||
                          selectedAddress?.latitude == null ||
                          selectedAddress?.longitude == null ||
                          Boolean(deliveryEligibility && !deliveryEligibility.isServiceAvailable)
                        }
                      >
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
