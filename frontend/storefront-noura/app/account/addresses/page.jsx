'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import {
  getCustomerAddresses,
  addCustomerAddress,
  deleteCustomerAddress,
  resolveCustomerToken
} from '@/lib/api'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'

export default function AccountAddressesPage() {
  const [token, setToken] = useState(null)
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    label: '', recipientName: '', phone: '', line1: '', line2: '',
    district: '', city: '', stateProvince: '', postalCode: '', countryCode: 'US',
    defaultShipping: true, defaultBilling: false
  })

  const resetForm = () => setForm({ label: '', recipientName: '', phone: '', line1: '', line2: '', district: '', city: '', stateProvince: '', postalCode: '', countryCode: 'US', defaultShipping: true, defaultBilling: false })

  const loadAddresses = async (t) => {
    try {
      const list = await getCustomerAddresses(t)
      setAddresses(Array.isArray(list) ? list : [])
    } catch (err) { setError(err.message || 'Unable to load addresses.') }
  }

  useEffect(() => {
    const t = resolveCustomerToken()
    if (!t) { setError('Please sign in to manage addresses.'); return }
    setToken(t)
    loadAddresses(t)
  }, [])

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true); setError(''); setMessage('')
    try {
      await addCustomerAddress(token, {
        label: form.label.trim() || null, recipientName: form.recipientName.trim(),
        phone: form.phone.trim() || null, line1: form.line1.trim(), line2: form.line2.trim() || null,
        district: form.district.trim() || null, city: form.city.trim(), stateProvince: form.stateProvince.trim() || null,
        postalCode: form.postalCode.trim() || null, countryCode: form.countryCode.trim(),
        defaultShipping: form.defaultShipping, defaultBilling: form.defaultBilling
      })
      setMessage('Address saved.')
      resetForm()
      setShowForm(false)
      await loadAddresses(token)
    } catch (err) { setError(err.message || 'Unable to add address.') }
    finally { setLoading(false) }
  }

  const onField = (f, v) => setForm((p) => ({ ...p, [f]: v }))

  const removeAddress = async (id) => {
    setLoading(true); setError(''); setMessage('')
    try { await deleteCustomerAddress(token, id); await loadAddresses(token); setMessage('Address removed.') }
    catch (err) { setError(err.message || 'Unable to remove address.') }
    finally { setLoading(false) }
  }

  if (!token && error) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}><div className="container"><Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Addresses' }]} /></div></section>
        <section className="featured-section"><div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}><div className="panel" style={{ padding: 40, textAlign: 'center' }}><p style={{ color: 'var(--danger)' }}>{error}</p><Link href="/auth/login" className="button primary">Sign In</Link></div></div></section>
      </>
    )
  }

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Addresses' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
            <h1 style={{ margin: 0 }}>Shipping Addresses</h1>
            <button type="button" className="button primary sm" onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Cancel' : '+ Add Address'}
            </button>
          </div>

          {error && <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', background: '#fef2f2', borderLeft: '3px solid var(--danger)' }}><p style={{ margin: 0, color: 'var(--danger)', fontSize: '0.9rem' }}>{error}</p></div>}
          {message && <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', background: '#f0fdf4', borderLeft: '3px solid var(--success)' }}><p style={{ margin: 0, color: 'var(--success)', fontSize: '0.9rem' }}>{message}</p></div>}

          {/* Add Form */}
          {showForm && (
            <div className="panel" style={{ padding: 24, marginBottom: 24 }}>
              <h3 style={{ margin: '0 0 16px' }}>New Address</h3>
              <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <input className="form-input" placeholder="Label (e.g. Home)" value={form.label} onChange={(e) => onField('label', e.target.value)} />
                  <input className="form-input" placeholder="Recipient Name *" required value={form.recipientName} onChange={(e) => onField('recipientName', e.target.value)} />
                </div>
                <input className="form-input" placeholder="Phone" value={form.phone} onChange={(e) => onField('phone', e.target.value)} />
                <input className="form-input" placeholder="Address Line 1 *" required value={form.line1} onChange={(e) => onField('line1', e.target.value)} />
                <input className="form-input" placeholder="Address Line 2" value={form.line2} onChange={(e) => onField('line2', e.target.value)} />
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                  <input className="form-input" placeholder="City *" required value={form.city} onChange={(e) => onField('city', e.target.value)} />
                  <input className="form-input" placeholder="State/Province *" required value={form.stateProvince} onChange={(e) => onField('stateProvince', e.target.value)} />
                  <input className="form-input" placeholder="Postal Code *" required value={form.postalCode} onChange={(e) => onField('postalCode', e.target.value)} />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <input className="form-input" placeholder="District" value={form.district} onChange={(e) => onField('district', e.target.value)} />
                  <input className="form-input" placeholder="Country (2 chars) *" required maxLength={2} value={form.countryCode} onChange={(e) => onField('countryCode', e.target.value)} />
                </div>
                <div style={{ display: 'flex', gap: 16 }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                    <input type="checkbox" checked={form.defaultShipping} onChange={(e) => onField('defaultShipping', e.target.checked)} /> Default Shipping
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                    <input type="checkbox" checked={form.defaultBilling} onChange={(e) => onField('defaultBilling', e.target.checked)} /> Default Billing
                  </label>
                </div>
                <button type="submit" className="button primary" disabled={loading}>{loading ? 'Saving...' : 'Save Address'}</button>
              </form>
            </div>
          )}

          {/* Addresses List */}
          {addresses.length === 0 ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)' }}>No addresses saved yet.</p>
            </div>
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {addresses.map((addr) => (
                <div key={addr.id} className="panel" style={{ padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
                        <strong>{addr.label || 'Address'}</strong>
                        {addr.defaultShipping && <Badge variant="success">Default Shipping</Badge>}
                        {addr.defaultBilling && <Badge variant="info">Default Billing</Badge>}
                      </div>
                      <p style={{ margin: 0, color: 'var(--muted)' }}>
                        {[addr.recipientName, addr.line1, addr.line2, addr.district, addr.city, addr.stateProvince, addr.postalCode, addr.countryCode].filter(Boolean).join(', ')}
                      </p>
                      {addr.phone && <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>Phone: {addr.phone}</p>}
                    </div>
                    <button type="button" className="button ghost sm" onClick={() => removeAddress(addr.id)} disabled={loading}>Delete</button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div style={{ marginTop: 24 }}>
            <Link href="/auth" className="button ghost">← Back to Account</Link>
          </div>
        </div>
      </section>
    </>
  )
}
