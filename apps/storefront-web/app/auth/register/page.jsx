'use client'

import { useState } from 'react'
import Link from 'next/link'
import { registerCustomer } from '@/lib/api'
import { Breadcrumbs } from '@/components/navigation'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      await registerCustomer({
        email: email.trim(),
        password,
        firstName: firstName.trim() || null,
        lastName: lastName.trim() || null,
        phone: phone.trim() || null
      })
      setSuccess('Account created successfully!')
    } catch (e) {
      setError(e.message || 'Unable to create account')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Create Account' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 48, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 520, width: '100%' }}>
          <div className="panel" style={{ padding: 32 }}>
            <h1 style={{ margin: '0 0 4px', textAlign: 'center' }}>Create Account</h1>
            <p style={{ color: 'var(--muted)', textAlign: 'center', marginBottom: 24 }}>Join Noura for a premium shopping experience</p>

            {error && (
              <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', background: '#fef2f2', borderLeft: '3px solid var(--danger)' }}>
                <p style={{ margin: 0, color: 'var(--danger)', fontSize: '0.9rem' }}>{error}</p>
              </div>
            )}

            {success && (
              <div style={{ padding: 16, marginBottom: 16, borderRadius: 'var(--radius-sm)', background: '#f0fdf4', borderLeft: '3px solid var(--success)', textAlign: 'center' }}>
                <p style={{ margin: '0 0 8px', color: 'var(--success)', fontWeight: 600 }}>{success}</p>
                <Link href="/auth/login" className="button primary sm">Sign In Now</Link>
              </div>
            )}

            <form onSubmit={submit} style={{ display: 'grid', gap: 16 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>First Name</label>
                  <input type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)} className="form-input" placeholder="John" />
                </div>
                <div>
                  <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Last Name</label>
                  <input type="text" value={lastName} onChange={(e) => setLastName(e.target.value)} className="form-input" placeholder="Doe" />
                </div>
              </div>
              <div>
                <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Email *</label>
                <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className="form-input" placeholder="you@example.com" />
              </div>
              <div>
                <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Password *</label>
                <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} className="form-input" placeholder="Min 8 characters" minLength={8} />
              </div>
              <div>
                <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Phone</label>
                <input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} className="form-input" placeholder="+1 (555) 000-0000" />
              </div>
              <button type="submit" className="button primary lg" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Creating...' : 'Create Account'}
              </button>
            </form>

            <div style={{ textAlign: 'center', marginTop: 20, fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--muted)' }}>Already have an account? </span>
              <Link href="/auth/login" style={{ color: 'var(--accent-blue)', fontWeight: 600 }}>Sign In</Link>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
