'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import {
  getCustomerAddresses,
  addCustomerAddress,
  deleteCustomerAddress,
  resolveCustomerToken
} from '@/lib/api'

function formatAddressLine(address) {
  return [
    address.recipientName,
    address.phone,
    address.line1,
    address.line2,
    address.district,
    address.city,
    address.stateProvince,
    address.postalCode,
    address.countryCode
  ]
    .filter(Boolean)
    .join(', ')
}

export default function AccountAddressesPage() {
  const [token, setToken] = useState(null)
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState({
    label: '',
    recipientName: '',
    phone: '',
    line1: '',
    line2: '',
    district: '',
    city: '',
    stateProvince: '',
    postalCode: '',
    countryCode: 'US',
    defaultShipping: true,
    defaultBilling: false
  })

  const loadAddresses = async (activeToken) => {
    setError('')
    try {
      const list = await getCustomerAddresses(activeToken)
      setAddresses(Array.isArray(list) ? list : [])
    } catch (err) {
      setError(err.message || 'Unable to load addresses.')
    }
  }

  useEffect(() => {
    const currentToken = resolveCustomerToken()
    if (!currentToken) {
      setError('Please sign in to manage addresses.')
      return
    }
    setToken(currentToken)
    loadAddresses(currentToken)
  }, [])

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setMessage('')

    try {
      if (!form.recipientName.trim()) {
        throw new Error('Recipient name is required.')
      }
      if (!form.line1.trim()) {
        throw new Error('Address line 1 is required.')
      }
      if (!form.city.trim()) {
        throw new Error('City is required.')
      }
      if (!form.countryCode.trim()) {
        throw new Error('Country code is required.')
      }
      await addCustomerAddress(token, {
        label: form.label.trim() || null,
        recipientName: form.recipientName.trim(),
        phone: form.phone.trim() || null,
        line1: form.line1.trim(),
        line2: form.line2.trim() || null,
        district: form.district.trim() || null,
        city: form.city.trim(),
        stateProvince: form.stateProvince.trim() || null,
        postalCode: form.postalCode.trim() || null,
        countryCode: form.countryCode.trim(),
        defaultShipping: form.defaultShipping,
        defaultBilling: form.defaultBilling
      })
      setMessage('Address saved.')
      setForm((previous) => ({
        ...previous,
        label: '',
        recipientName: '',
        phone: '',
        line1: '',
        line2: '',
        district: '',
        city: '',
        stateProvince: '',
        postalCode: '',
        countryCode: 'US',
        defaultShipping: true,
        defaultBilling: false
      }))
      await loadAddresses(token)
    } catch (err) {
      setError(err.message || 'Unable to add address.')
    } finally {
      setLoading(false)
    }
  }

  const onFieldChange = (field, value) => {
    setForm((previous) => ({ ...previous, [field]: value }))
  }

  const removeAddress = async (addressId) => {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await deleteCustomerAddress(token, addressId)
      await loadAddresses(token)
      setMessage('Address removed.')
    } catch (err) {
      setError(err.message || 'Unable to remove address.')
    } finally {
      setLoading(false)
    }
  }

  if (error && !token) {
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

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Account</span>
          <h1>Shipping addresses</h1>
        </div>
      </div>

      {error ? (
        <div className="notice panel">
          <p>{error}</p>
        </div>
      ) : null}

      {message ? (
        <div className="panel notice">
          <p>{message}</p>
        </div>
      ) : null}

      <form onSubmit={submit} className="filter-bar">
        <input
          type="text"
          placeholder="Label (optional)"
          value={form.label}
          onChange={(event) => onFieldChange('label', event.target.value)}
        />
        <input
          type="text"
          placeholder="Recipient name *"
          required
          value={form.recipientName}
          onChange={(event) => onFieldChange('recipientName', event.target.value)}
        />
        <input
          type="text"
          placeholder="Phone"
          value={form.phone}
          onChange={(event) => onFieldChange('phone', event.target.value)}
        />
        <input
          type="text"
          placeholder="Line 1 *"
          required
          value={form.line1}
          onChange={(event) => onFieldChange('line1', event.target.value)}
        />
        <input
          type="text"
          placeholder="Line 2"
          value={form.line2}
          onChange={(event) => onFieldChange('line2', event.target.value)}
        />
        <input
          type="text"
          placeholder="District"
          value={form.district}
          onChange={(event) => onFieldChange('district', event.target.value)}
        />
        <input
          type="text"
          placeholder="City *"
          required
          value={form.city}
          onChange={(event) => onFieldChange('city', event.target.value)}
        />
        <input
          type="text"
          placeholder="State/Province"
          value={form.stateProvince}
          onChange={(event) => onFieldChange('stateProvince', event.target.value)}
        />
        <input
          type="text"
          placeholder="Postal code"
          value={form.postalCode}
          onChange={(event) => onFieldChange('postalCode', event.target.value)}
        />
        <input
          type="text"
          placeholder="Country code (2 chars) *"
          required
          maxLength={2}
          value={form.countryCode}
          onChange={(event) => onFieldChange('countryCode', event.target.value)}
        />
        <label className="button ghost">
          <input
            type="checkbox"
            checked={form.defaultShipping}
            onChange={(event) => onFieldChange('defaultShipping', event.target.checked)}
          />
          Default shipping
        </label>
        <label className="button ghost">
          <input
            type="checkbox"
            checked={form.defaultBilling}
            onChange={(event) => onFieldChange('defaultBilling', event.target.checked)}
          />
          Default billing
        </label>
        <button type="submit" className="button primary" disabled={loading}>
          {loading ? 'Saving...' : 'Add address'}
        </button>
      </form>

      <div className="section-head">
        <h2>Saved addresses</h2>
      </div>

      {addresses.length === 0 ? (
        <div className="panel notice">No addresses yet.</div>
      ) : (
        <div className="product-grid">
          {addresses.map((address) => (
            <article key={address.id} className="product-card">
              <div className="product-meta">
                <strong>{address.label || 'Address'}</strong>
                <p>{formatAddressLine(address)}</p>
                <small>
                  {address.defaultShipping ? 'Default shipping' : ''}{address.defaultShipping && address.defaultBilling ? ' • ' : ''}
                  {address.defaultBilling ? 'Default billing' : ''}
                </small>
              </div>
              <div className="hero-actions">
                <button
                  type="button"
                  className="button ghost"
                  onClick={() => removeAddress(address.id)}
                  disabled={loading}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
