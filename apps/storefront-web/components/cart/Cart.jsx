'use client'

import { useState, useRef, useEffect } from 'react'
import Link from 'next/link'

/**
 * MiniCart — Dropdown mini cart preview.
 */
export function MiniCart({ items = [], total, open, onClose, onViewCart, onCheckout, className = '' }) {
  const ref = useRef(null)

  useEffect(() => {
    if (!open) return
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) onClose?.()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open, onClose])

  if (!open) return null

  return (
    <div ref={ref} className={`mini-cart ${className}`}>
      <div className="mini-cart-header">
        <strong style={{ fontSize: '0.9rem' }}>Cart ({items.length})</strong>
        <button type="button" className="modal-close" onClick={onClose} aria-label="Close">✕</button>
      </div>
      {items.length === 0 ? (
        <div style={{ padding: '32px 20px', textAlign: 'center', color: 'var(--muted)', fontSize: '0.875rem' }}>
          Your cart is empty
        </div>
      ) : (
        <>
          <div className="mini-cart-items">
            {items.map((item, i) => (
              <div key={i} className="mini-cart-item">
                <div className="mini-cart-thumb">
                  {item.imageUrl ? <img src={item.imageUrl} alt={item.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : '📦'}
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.name}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>Qty: {item.quantity}</div>
                </div>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', whiteSpace: 'nowrap' }}>
                  ${(item.price * item.quantity).toFixed(2)}
                </span>
              </div>
            ))}
          </div>
          <div className="mini-cart-footer">
            <div className="summary-row total">
              <span>Total</span>
              <span>${total?.toFixed(2)}</span>
            </div>
            <Link href="/cart" className="button primary" onClick={onClose} style={{ textAlign: 'center' }}>View Cart</Link>
            <Link href="/cart" className="button accent" onClick={onClose} style={{ textAlign: 'center' }}>Checkout</Link>
          </div>
        </>
      )}
    </div>
  )
}

/**
 * CartItem — Full cart item row with quantity controls and remove.
 */
export function CartItem({ item, onUpdateQty, onRemove, className = '' }) {
  return (
    <div className={`cart-item ${className}`}>
      <div className="cart-item-image">
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : '📦'}
      </div>
      <div className="cart-item-info">
        <strong style={{ fontSize: '0.95rem' }}>{item.name}</strong>
        {item.variant && <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>{item.variant}</span>}
        <span style={{ fontWeight: 700 }}>${(item.price * item.quantity).toFixed(2)}</span>
      </div>
      <div className="cart-item-actions">
        <div className="qty-control">
          <button type="button" onClick={() => onUpdateQty?.(item, Math.max(1, item.quantity - 1))}>−</button>
          <span>{item.quantity}</span>
          <button type="button" onClick={() => onUpdateQty?.(item, item.quantity + 1)}>+</button>
        </div>
        <button
          type="button"
          className="button ghost sm"
          onClick={() => onRemove?.(item)}
          aria-label="Remove item"
        >✕</button>
      </div>
    </div>
  )
}

/**
 * CouponInput — Coupon code entry with apply/remove.
 */
export function CouponInput({ onApply, appliedCoupon, onRemove, className = '' }) {
  const [code, setCode] = useState('')
  const [error, setError] = useState('')

  const handleApply = () => {
    if (!code.trim()) {
      setError('Enter a coupon code')
      return
    }
    setError('')
    onApply?.(code.trim())
    setCode('')
  }

  if (appliedCoupon) {
    return (
      <div className={`coupon-applied ${className}`}>
        <span>🎫 {appliedCoupon.code} — {appliedCoupon.description || 'Applied'}</span>
        <button type="button" className="coupon-remove" onClick={onRemove}>✕</button>
      </div>
    )
  }

  return (
    <div className={className}>
      <div className="coupon-input-group">
        <input
          type="text"
          className="form-input"
          placeholder="Coupon code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleApply()}
        />
        <button type="button" className="button ghost" onClick={handleApply}>Apply</button>
      </div>
      {error && <span className="input-error" style={{ marginTop: 4 }}>{error}</span>}
    </div>
  )
}

/**
 * ShippingEstimate — Shipping method selector.
 */
export function ShippingEstimate({ options = [], value, onChange, className = '' }) {
  return (
    <div className={`shipping-options ${className}`}>
      {options.map(opt => (
        <label
          key={opt.id}
          className={`shipping-option ${value === opt.id ? 'selected' : ''}`}
        >
          <input
            type="radio"
            name="shipping"
            className="radio-input"
            checked={value === opt.id}
            onChange={() => onChange?.(opt.id)}
          />
          <span className="radio-dot" />
          <div className="shipping-option-info">
            <div className="shipping-option-name">{opt.name}</div>
            <div className="shipping-option-est">{opt.estimate}</div>
          </div>
          <span className="shipping-option-price">
            {opt.price === 0 ? 'FREE' : `$${opt.price?.toFixed(2)}`}
          </span>
        </label>
      ))}
    </div>
  )
}

/**
 * CartSummary — Order summary sidebar.
 */
export function CartSummary({ subtotal, shipping, tax, discount, total, children, className = '' }) {
  return (
    <div className={`cart-summary ${className}`}>
      <h3 style={{ margin: 0, fontSize: '1.1rem' }}>Order Summary</h3>
      <div className="summary-row">
        <span>Subtotal</span>
        <span>${subtotal?.toFixed(2)}</span>
      </div>
      {shipping != null && (
        <div className="summary-row">
          <span>Shipping</span>
          <span>{shipping === 0 ? 'FREE' : `$${shipping.toFixed(2)}`}</span>
        </div>
      )}
      {discount > 0 && (
        <div className="summary-row" style={{ color: 'var(--success)' }}>
          <span>Discount</span>
          <span>−${discount.toFixed(2)}</span>
        </div>
      )}
      {tax != null && (
        <div className="summary-row">
          <span>Tax</span>
          <span>${tax.toFixed(2)}</span>
        </div>
      )}
      <div className="summary-row total">
        <span>Total</span>
        <span>${total?.toFixed(2)}</span>
      </div>
      {children}
    </div>
  )
}

/**
 * OrderConfirmation — Thank you page content.
 */
export function OrderConfirmation({ orderNumber, email, estimatedDelivery, children, className = '' }) {
  return (
    <div className={`order-confirmation ${className}`}>
      <div className="order-confirmation-icon">✅</div>
      <h2>Thank you for your order!</h2>
      <p className="order-number">
        Order <strong>#{orderNumber}</strong>
      </p>
      {email && (
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          Confirmation sent to <strong>{email}</strong>
        </p>
      )}
      {estimatedDelivery && (
        <p style={{ fontSize: '0.9rem' }}>
          Estimated delivery: <strong>{estimatedDelivery}</strong>
        </p>
      )}
      {children}
    </div>
  )
}
