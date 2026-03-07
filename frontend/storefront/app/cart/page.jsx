'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  clearCart,
  getCart,
  getCustomerAddresses,
  addCartItem,
  updateCartItem,
  removeCartItem,
  resolveCustomerToken,
  checkoutCart
} from '@/lib/api'
import { formatCurrency } from '@/lib/format'

function CartRow({ item, onUpdate, onRemove }) {
  return (
    <div className="product-card">
      <div className="product-meta">
        <strong>{item.productName || `Product #${item.productId}`}</strong>
        <span className="product-category">SKU {item.sku || 'N/A'}</span>
        <p>{formatCurrency(item.lineTotal || 0)}</p>
      </div>
      <div className="hero-actions" style={{ padding: 14 }}>
        <span>Qty: {item.quantity}</span>
        <button className="button ghost" onClick={() => onUpdate(item.id, Math.max(1, item.quantity - 1))}>
          -1
        </button>
        <button className="button ghost" onClick={() => onUpdate(item.id, item.quantity + 1)}>
          +1
        </button>
        <button className="button ghost" onClick={() => onRemove(item.id)}>
          Remove
        </button>
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
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const loadCheckoutData = async (activeToken) => {
    setError('')
    try {
      const [current, customerAddresses] = await Promise.all([
        getCart(activeToken),
        getCustomerAddresses(activeToken)
      ])
      setCart(current)
      setAddresses(Array.isArray(customerAddresses) ? customerAddresses : [])
      const defaultShipping = customerAddresses?.find((address) => address?.defaultShipping)
      if (!selectedAddressId || !customerAddresses?.some((address) => address.id === Number.parseInt(selectedAddressId, 10))) {
        if (defaultShipping?.id) {
          setSelectedAddressId(String(defaultShipping.id))
        } else if (customerAddresses?.length > 0) {
          setSelectedAddressId(String(customerAddresses[0].id))
        } else {
          setSelectedAddressId('')
        }
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

  const addItem = async (event) => {
    event.preventDefault()
    if (!token) return
    setLoading(true)
    try {
      const idNum = Number.parseInt(productId, 10)
      const qty = Number.parseInt(quantity, 10)
      if (!Number.isFinite(idNum) || idNum <= 0 || Number.isNaN(qty) || qty <= 0) {
        throw new Error('Product ID and quantity must be positive.')
      }
      await addCartItem(token, { productId: idNum, quantity: qty })
      setProductId('')
      setQuantity('1')
      await loadCheckoutData(token)
    } catch (e) {
      setError(e.message || 'Unable to add item.')
    } finally {
      setLoading(false)
    }
  }

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

  const handleCheckout = async () => {
    if (!token) return
    setLoading(true)
    const shippingAddressId = selectedAddressId ? Number.parseInt(selectedAddressId, 10) : null
    try {
      await checkoutCart(token, {
        shippingAddressId: Number.isFinite(shippingAddressId) ? shippingAddressId : null,
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

  const selectedAddress = addresses.find(
    (address) => address.id === Number.parseInt(selectedAddressId, 10)
  )

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Cart</span>
          <h1>Customer shopping cart</h1>
        </div>
      </div>

      {error && (
        <div className="notice panel">
          <p>{error}</p>
        </div>
      )}

      <form onSubmit={addItem} className="filter-bar">
          <input
            type="number"
            min="1"
            step="1"
            value={productId}
            required
          onChange={(event) => setProductId(event.target.value)}
          placeholder="Product ID"
        />
        <input
          type="number"
          min="1"
          step="1"
          value={quantity}
          required
          onChange={(event) => setQuantity(event.target.value)}
          placeholder="Quantity"
        />
        <button type="submit" className="button primary" disabled={loading || !token}>
          {loading ? 'Adding...' : 'Add by product id'}
        </button>
          <button type="button" className="button ghost" disabled={loading || !token} onClick={handleClear}>
            Clear cart
          </button>
          {addresses.length > 0 && (
            <div className="panel">
              <div className="section-head">
                <div>
                  <span className="eyebrow">Shipping</span>
                  <h2>Delivery address</h2>
                </div>
              </div>
              <div className="filter-bar">
                <select
                  value={selectedAddressId}
                  onChange={(event) => setSelectedAddressId(event.target.value)}
                >
                  {addresses.map((address) => (
                    <option key={address.id} value={String(address.id)}>
                      {address.label || `${address.recipientName} - ${address.line1}`}
                    </option>
                  ))}
                </select>
              </div>
              <div className="filter-bar">
                <label>Payment method</label>
                <select
                  value={paymentMethod}
                  onChange={(event) => setPaymentMethod(event.target.value)}
                >
                  <option value="CASH_ON_DELIVERY">Cash on delivery</option>
                  <option value="CREDIT_CARD">Credit card</option>
                  <option value="BANK_TRANSFER">Bank transfer</option>
                  <option value="WALLET">Wallet</option>
                  <option value="COD">COD (legacy)</option>
                </select>
              </div>
              {paymentMethod !== 'CASH_ON_DELIVERY' && paymentMethod !== 'COD' ? (
                <>
                  <input
                    type="text"
                    value={paymentProvider}
                    placeholder="Provider"
                    onChange={(event) => setPaymentProvider(event.target.value)}
                  />
                  <input
                    type="text"
                    value={paymentProviderReference}
                    placeholder="Payment reference (optional)"
                    onChange={(event) => setPaymentProviderReference(event.target.value)}
                  />
                </>
              ) : null}
              {selectedAddress ? (
                <p>
                  {[
                    selectedAddress.recipientName,
                    selectedAddress.line1,
                    selectedAddress.line2,
                    selectedAddress.city,
                    selectedAddress.postalCode
                  ]
                    .filter(Boolean)
                    .join(', ')}
                </p>
              ) : null}
              <a href="/account/addresses" className="button ghost">
                Manage addresses
              </a>
            </div>
          )}
        </form>

      {!cart ? (
        <div className="panel notice">Loading cart...</div>
      ) : cart.items.length === 0 ? (
        <div className="panel notice">Your cart is empty.</div>
      ) : (
        <>
          <div className="product-grid">
            {cart.items.map((item) => (
              <CartRow
                key={item.id}
                item={item}
                onUpdate={handleUpdate}
                onRemove={handleRemove}
              />
            ))}
          </div>

          <div className="hero-actions">
            <strong>Subtotal: {formatCurrency(total)}</strong>
            <button
              type="button"
              className="button primary"
              disabled={loading || cart.items.length === 0}
              onClick={handleCheckout}
            >
              Checkout
            </button>
            <a href="/products" className="button ghost">
              Continue shopping
            </a>
          </div>
        </>
      )}
    </section>
  )
}
