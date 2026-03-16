'use client'

import { useState } from 'react'

/**
 * ProfileEditor — Editable profile form.
 */
export function ProfileEditor({ profile = {}, onSave, loading, className = '' }) {
  const [form, setForm] = useState({
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    email: profile.email || '',
    phone: profile.phone || '',
  })

  const set = (k, v) => setForm(prev => ({ ...prev, [k]: v }))

  return (
    <form
      className={`profile-editor ${className}`}
      onSubmit={(e) => { e.preventDefault(); onSave?.(form) }}
    >
      <div className="form-row">
        <div className="form-group">
          <label className="input-label">First Name</label>
          <input className="form-input" value={form.firstName} onChange={e => set('firstName', e.target.value)} />
        </div>
        <div className="form-group">
          <label className="input-label">Last Name</label>
          <input className="form-input" value={form.lastName} onChange={e => set('lastName', e.target.value)} />
        </div>
      </div>
      <div className="form-group">
        <label className="input-label">Email</label>
        <input className="form-input" type="email" value={form.email} onChange={e => set('email', e.target.value)} />
      </div>
      <div className="form-group">
        <label className="input-label">Phone</label>
        <input className="form-input" type="tel" value={form.phone} onChange={e => set('phone', e.target.value)} />
      </div>
      <button type="submit" className="button primary" disabled={loading}>
        {loading ? 'Saving…' : 'Save Changes'}
      </button>
    </form>
  )
}

/**
 * AddressCard — Single address card with edit/delete.
 */
export function AddressCard({ address, isDefault, onEdit, onDelete, onSetDefault, className = '' }) {
  return (
    <div className={`address-card ${isDefault ? 'default' : ''} ${className}`}>
      {isDefault && <span className="badge success" style={{ marginBottom: 6 }}>Default</span>}
      <strong>{address.label || address.fullName || 'Address'}</strong>
      <p style={{ margin: '4px 0 0', fontSize: '0.875rem', lineHeight: 1.5, color: 'var(--muted)' }}>
        {address.line1}<br />
        {address.line2 && <>{address.line2}<br /></>}
        {address.city}, {address.state} {address.postalCode}<br />
        {address.country}
      </p>
      <div className="address-card-actions" style={{ marginTop: 10, display: 'flex', gap: 8 }}>
        {onEdit && <button type="button" className="button ghost sm" onClick={onEdit}>Edit</button>}
        {!isDefault && onSetDefault && (
          <button type="button" className="button ghost sm" onClick={onSetDefault}>Set Default</button>
        )}
        {onDelete && <button type="button" className="button ghost sm" onClick={onDelete} style={{ color: 'var(--danger)' }}>Delete</button>}
      </div>
    </div>
  )
}

/**
 * AddressBook — List of addresses with add new.
 */
export function AddressBook({ addresses = [], onEdit, onDelete, onSetDefault, onAdd, className = '' }) {
  return (
    <div className={className}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0 }}>Addresses</h3>
        {onAdd && <button type="button" className="button ghost sm" onClick={onAdd}>+ Add Address</button>}
      </div>
      <div className="address-grid" style={{ display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
        {addresses.map((addr, i) => (
          <AddressCard
            key={addr.id || i}
            address={addr}
            isDefault={addr.isDefault}
            onEdit={() => onEdit?.(addr)}
            onDelete={() => onDelete?.(addr)}
            onSetDefault={() => onSetDefault?.(addr)}
          />
        ))}
      </div>
    </div>
  )
}

/**
 * AddressForm — Address input form for checkout or account.
 */
export function AddressForm({ address = {}, onSubmit, onCancel, loading, className = '' }) {
  const [form, setForm] = useState({
    fullName: address.fullName || '',
    line1: address.line1 || '',
    line2: address.line2 || '',
    city: address.city || '',
    state: address.state || '',
    postalCode: address.postalCode || '',
    country: address.country || '',
    phone: address.phone || '',
  })

  const set = (k, v) => setForm(prev => ({ ...prev, [k]: v }))

  return (
    <form
      className={`address-form ${className}`}
      onSubmit={(e) => { e.preventDefault(); onSubmit?.(form) }}
    >
      <div className="form-group">
        <label className="input-label">Full Name</label>
        <input className="form-input" value={form.fullName} onChange={e => set('fullName', e.target.value)} required />
      </div>
      <div className="form-group">
        <label className="input-label">Address Line 1</label>
        <input className="form-input" value={form.line1} onChange={e => set('line1', e.target.value)} required />
      </div>
      <div className="form-group">
        <label className="input-label">Address Line 2</label>
        <input className="form-input" value={form.line2} onChange={e => set('line2', e.target.value)} />
      </div>
      <div className="form-row">
        <div className="form-group">
          <label className="input-label">City</label>
          <input className="form-input" value={form.city} onChange={e => set('city', e.target.value)} required />
        </div>
        <div className="form-group">
          <label className="input-label">State / Province</label>
          <input className="form-input" value={form.state} onChange={e => set('state', e.target.value)} required />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label className="input-label">Postal Code</label>
          <input className="form-input" value={form.postalCode} onChange={e => set('postalCode', e.target.value)} required />
        </div>
        <div className="form-group">
          <label className="input-label">Country</label>
          <input className="form-input" value={form.country} onChange={e => set('country', e.target.value)} required />
        </div>
      </div>
      <div className="form-group">
        <label className="input-label">Phone</label>
        <input className="form-input" type="tel" value={form.phone} onChange={e => set('phone', e.target.value)} />
      </div>
      <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
        <button type="submit" className="button primary" disabled={loading}>
          {loading ? 'Saving…' : 'Save Address'}
        </button>
        {onCancel && <button type="button" className="button ghost" onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  )
}

/**
 * OrderHistoryTable — Orders table with basic columns.
 */
export function OrderHistoryTable({ orders = [], onViewOrder, className = '' }) {
  return (
    <div className={`order-table-wrap ${className}`}>
      <table className="order-table">
        <thead>
          <tr>
            <th>Order</th>
            <th>Date</th>
            <th>Status</th>
            <th>Total</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {orders.length === 0 ? (
            <tr><td colSpan={5} style={{ textAlign: 'center', padding: 32, color: 'var(--muted)' }}>No orders yet</td></tr>
          ) : (
            orders.map(order => (
              <tr key={order.id}>
                <td><strong>#{order.orderNumber || order.id}</strong></td>
                <td>{order.date || order.createdAt}</td>
                <td><span className={`badge ${order.status === 'DELIVERED' ? 'success' : order.status === 'CANCELLED' ? 'danger' : ''}`}>{order.status}</span></td>
                <td>${order.total?.toFixed(2)}</td>
                <td><button type="button" className="button ghost sm" onClick={() => onViewOrder?.(order)}>View</button></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

/**
 * CouponsWallet — List of available/expired coupons.
 */
export function CouponsWallet({ coupons = [], onApply, className = '' }) {
  return (
    <div className={className}>
      <h3 style={{ margin: '0 0 16px' }}>My Coupons</h3>
      {coupons.length === 0 ? (
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>No coupons available</p>
      ) : (
        <div style={{ display: 'grid', gap: 12, gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))' }}>
          {coupons.map((c, i) => (
            <div key={c.id || i} className={`coupon-card ${c.expired ? 'expired' : ''}`}>
              <div className="coupon-card-value">{c.value}</div>
              <div className="coupon-card-code">{c.code}</div>
              <div className="coupon-card-desc">{c.description}</div>
              {c.expiresAt && <div className="coupon-card-exp">Exp: {c.expiresAt}</div>}
              {!c.expired && onApply && (
                <button type="button" className="button primary sm" onClick={() => onApply(c)} style={{ marginTop: 8 }}>
                  Apply
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * SecuritySettings — Change password form.
 */
export function SecuritySettings({ onChangePassword, loading, className = '' }) {
  const [form, setForm] = useState({ current: '', newPass: '', confirm: '' })
  const [error, setError] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    if (form.newPass !== form.confirm) {
      setError('Passwords do not match')
      return
    }
    if (form.newPass.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    setError('')
    onChangePassword?.(form)
  }

  return (
    <form className={className} onSubmit={handleSubmit}>
      <h3 style={{ margin: '0 0 16px' }}>Change Password</h3>
      <div className="form-group">
        <label className="input-label">Current Password</label>
        <input className="form-input" type="password" value={form.current} onChange={e => setForm(p => ({ ...p, current: e.target.value }))} required autoComplete="current-password" />
      </div>
      <div className="form-group">
        <label className="input-label">New Password</label>
        <input className="form-input" type="password" value={form.newPass} onChange={e => setForm(p => ({ ...p, newPass: e.target.value }))} required autoComplete="new-password" />
      </div>
      <div className="form-group">
        <label className="input-label">Confirm New Password</label>
        <input className="form-input" type="password" value={form.confirm} onChange={e => setForm(p => ({ ...p, confirm: e.target.value }))} required autoComplete="new-password" />
      </div>
      {error && <div className="input-error" style={{ marginBottom: 8 }}>{error}</div>}
      <button type="submit" className="button primary" disabled={loading}>
        {loading ? 'Updating…' : 'Update Password'}
      </button>
    </form>
  )
}
