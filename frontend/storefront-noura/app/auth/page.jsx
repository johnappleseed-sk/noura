'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import {
  clearCustomerToken,
  getCustomerAddresses,
  getCustomerMe,
  getMyOrders,
  listReturns,
  resolveCustomerToken,
  updateProfile
} from '@/lib/api'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'

function formatDate(value) {
  if (!value) return 'No activity yet'
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export default function AuthLandingPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [profile, setProfile] = useState(null)
  const [orders, setOrders] = useState([])
  const [addresses, setAddresses] = useState([])
  const [returns, setReturns] = useState([])
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '', phone: '' })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const token = resolveCustomerToken()
    if (!token) { setLoading(false); return }

    ;(async () => {
      try {
        const results = await Promise.allSettled([
          getCustomerMe(token),
          getMyOrders(token),
          getCustomerAddresses(token),
          listReturns(token)
        ])
        if (results[0].status === 'fulfilled') {
          setProfile(results[0].value)
          setEditForm({ firstName: results[0].value.firstName || '', lastName: results[0].value.lastName || '', phone: results[0].value.phone || '' })
        }
        if (results[1].status === 'fulfilled') setOrders(Array.isArray(results[1].value) ? results[1].value : [])
        if (results[2].status === 'fulfilled') setAddresses(Array.isArray(results[2].value) ? results[2].value : [])
        if (results[3].status === 'fulfilled') setReturns(Array.isArray(results[3].value) ? results[3].value : [])
      } catch (err) {
        setError(err.message || 'Unable to load your account.')
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  function handleLogout() {
    clearCustomerToken()
    setProfile(null)
    router.refresh()
  }

  const handleSaveProfile = async () => {
    setSaving(true)
    try {
      const token = resolveCustomerToken()
      await updateProfile(token, editForm)
      setProfile((prev) => ({ ...prev, ...editForm }))
      setEditing(false)
    } catch { /* ignore */ }
    setSaving(false)
  }

  if (loading) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}><div className="container"><Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account' }]} /></div></section>
        <section className="featured-section"><div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}><div className="panel" style={{ padding: 40, textAlign: 'center' }}><p style={{ color: 'var(--muted)' }}>Loading your account...</p></div></div></section>
      </>
    )
  }

  if (!profile) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}><div className="container"><Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account' }]} /></div></section>
        <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
          <div className="container" style={{ maxWidth: 520 }}>
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <h1 style={{ marginBottom: 8 }}>Welcome to Noura</h1>
              <p style={{ color: 'var(--muted)', marginBottom: 24 }}>Sign in to your account or create one to start shopping.</p>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
                <Link href="/auth/login" className="button primary lg">Sign In</Link>
                <Link href="/auth/register" className="button ghost lg">Create Account</Link>
              </div>
            </div>
            {error && <div className="panel" style={{ padding: 16, marginTop: 16, borderLeft: '3px solid var(--danger)' }}><p style={{ margin: 0, color: 'var(--danger)' }}>{error}</p></div>}
          </div>
        </section>
      </>
    )
  }

  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.email

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32 }}>
            <div>
              <h1 style={{ margin: '0 0 4px' }}>{fullName}</h1>
              <p style={{ color: 'var(--muted)', margin: 0 }}>{profile.email}</p>
            </div>
            <button type="button" className="button ghost" onClick={handleLogout}>Sign Out</button>
          </div>

          {error && <div className="panel" style={{ padding: 16, marginBottom: 20, borderLeft: '3px solid var(--danger)' }}><p style={{ margin: 0, color: 'var(--danger)' }}>{error}</p></div>}

          {/* Stats */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 16, marginBottom: 32 }}>
            <div className="panel" style={{ padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: '2rem', fontWeight: 700 }}>{orders.length}</div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Orders</div>
            </div>
            <div className="panel" style={{ padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: '2rem', fontWeight: 700 }}>{addresses.length}</div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Addresses</div>
            </div>
            <div className="panel" style={{ padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: '2rem', fontWeight: 700 }}>{returns.length}</div>
              <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Returns</div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
            {/* Profile Info */}
            <div className="panel" style={{ padding: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <h3 style={{ margin: 0 }}>Profile</h3>
                <button type="button" className="button ghost sm" onClick={() => setEditing(!editing)}>{editing ? 'Cancel' : 'Edit'}</button>
              </div>
              {editing ? (
                <div style={{ display: 'grid', gap: 12 }}>
                  <input className="form-input" placeholder="First name" value={editForm.firstName} onChange={(e) => setEditForm((p) => ({ ...p, firstName: e.target.value }))} />
                  <input className="form-input" placeholder="Last name" value={editForm.lastName} onChange={(e) => setEditForm((p) => ({ ...p, lastName: e.target.value }))} />
                  <input className="form-input" placeholder="Phone" value={editForm.phone} onChange={(e) => setEditForm((p) => ({ ...p, phone: e.target.value }))} />
                  <button type="button" className="button primary sm" onClick={handleSaveProfile} disabled={saving}>{saving ? 'Saving...' : 'Save Changes'}</button>
                </div>
              ) : (
                <dl style={{ display: 'grid', gap: 12, margin: 0 }}>
                  <div><dt style={{ color: 'var(--muted)', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Email</dt><dd style={{ margin: '2px 0 0', fontWeight: 600 }}>{profile.email}</dd></div>
                  <div><dt style={{ color: 'var(--muted)', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Phone</dt><dd style={{ margin: '2px 0 0', fontWeight: 600 }}>{profile.phone || 'Not provided'}</dd></div>
                  <div><dt style={{ color: 'var(--muted)', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Last Activity</dt><dd style={{ margin: '2px 0 0', fontWeight: 600 }}>{formatDate(orders[0]?.placedAt)}</dd></div>
                </dl>
              )}
            </div>

            {/* Quick Actions */}
            <div className="panel" style={{ padding: 24 }}>
              <h3 style={{ margin: '0 0 16px' }}>Quick Actions</h3>
              <div style={{ display: 'grid', gap: 8 }}>
                <Link href="/orders" className="button primary" style={{ textAlign: 'center' }}>View Orders</Link>
                <Link href="/returns" className="button ghost" style={{ textAlign: 'center' }}>Manage Returns</Link>
                <Link href="/account/addresses" className="button ghost" style={{ textAlign: 'center' }}>Manage Addresses</Link>
                <Link href="/cart" className="button ghost" style={{ textAlign: 'center' }}>Open Cart</Link>
              </div>
            </div>
          </div>

          {/* Recent Orders */}
          {orders.length > 0 && (
            <div style={{ marginTop: 32 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <h2 style={{ margin: 0 }}>Recent Orders</h2>
                <Link href="/orders" className="button ghost sm">View All</Link>
              </div>
              <div style={{ display: 'grid', gap: 12 }}>
                {orders.slice(0, 3).map((order) => (
                  <Link key={order.id} href={`/orders/${order.id}`} className="panel" style={{ padding: 16, display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 16, alignItems: 'center', textDecoration: 'none' }}>
                    <div>
                      <strong>{order.orderNumber || `#${order.id}`}</strong>
                      <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>{order.placedAt ? new Date(order.placedAt).toLocaleDateString() : 'Pending'}</div>
                    </div>
                    <Badge variant="neutral">{order.status || 'PENDING'}</Badge>
                    <strong>{order.grandTotal != null ? `$${Number(order.grandTotal).toFixed(2)}` : ''}</strong>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </>
  )
}
