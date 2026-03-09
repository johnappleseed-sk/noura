'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { loginCustomer, persistCustomerToken } from '@/lib/api'
import { Breadcrumbs } from '@/components/navigation'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')

    try {
      const result = await loginCustomer({ email: email.trim(), password })
      if (!result?.accessToken) throw new Error('Login response is missing access token')
      persistCustomerToken(result.accessToken)
      router.push('/auth')
      router.refresh()
    } catch (e) {
      setError(e.message || 'Unable to log in')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Sign In' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 48, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 520, width: '100%' }}>
          <div className="panel" style={{ padding: 32 }}>
            <h1 style={{ margin: '0 0 4px', textAlign: 'center' }}>Sign In</h1>
            <p style={{ color: 'var(--muted)', textAlign: 'center', marginBottom: 24 }}>Welcome back to Noura</p>

            {error && (
              <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', background: '#fef2f2', borderLeft: '3px solid var(--danger)' }}>
                <p style={{ margin: 0, color: 'var(--danger)', fontSize: '0.9rem' }}>{error}</p>
              </div>
            )}

            <form onSubmit={submit} style={{ display: 'grid', gap: 16 }}>
              <div>
                <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Email</label>
                <input type="email" name="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} className="form-input" placeholder="you@example.com" />
              </div>
              <div>
                <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>Password</label>
                <input type="password" name="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} className="form-input" placeholder="Min 8 characters" minLength={8} />
              </div>
              <button type="submit" className="button primary lg" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Signing in...' : 'Sign In'}
              </button>
            </form>

            <div style={{ textAlign: 'center', marginTop: 20, fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--muted)' }}>Don&apos;t have an account? </span>
              <Link href="/auth/register" style={{ color: 'var(--accent-blue)', fontWeight: 600 }}>Create one</Link>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
