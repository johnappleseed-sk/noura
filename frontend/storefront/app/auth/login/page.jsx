'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { loginCustomer, persistCustomerToken } from '@/lib/api'

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
      if (!result?.accessToken) {
        throw new Error('Login response is missing access token')
      }
      persistCustomerToken(result.accessToken)
      router.push('/cart')
      router.refresh()
    } catch (e) {
      setError(e.message || 'Unable to log in')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="section">
      <div className="panel">
        <div className="section-head">
          <div>
            <span className="eyebrow">Customer sign-in</span>
            <h1>Sign in to your storefront account</h1>
          </div>
        </div>

        <form onSubmit={submit} className="filter-bar">
          <input
            type="email"
            name="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="Email"
          />
          <input
            type="password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password"
            minLength={8}
          />
          <button type="submit" className="button primary" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
          <a className="button ghost" href="/auth/register">
            No account yet?
          </a>
        </form>
      </div>
      {error ? (
        <div className="notice panel">
          <p>{error}</p>
        </div>
      ) : null}
    </section>
  )
}
