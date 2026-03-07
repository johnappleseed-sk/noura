'use client'

import { useState } from 'react'
import Link from 'next/link'
import { registerCustomer } from '@/lib/api'

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
      setSuccess('Account created. Continue to sign in.')
    } catch (e) {
      setError(e.message || 'Unable to create account')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="section">
      <div className="panel">
        <div className="section-head">
          <div>
            <span className="eyebrow">Create account</span>
            <h1>Register for the storefront</h1>
          </div>
        </div>

        <form onSubmit={submit} className="filter-bar">
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="Email"
          />
          <input
            type="password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password (min 8)"
            minLength={8}
          />
          <input
            type="text"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            placeholder="First name"
          />
          <input
            type="text"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            placeholder="Last name"
          />
          <input
            type="text"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="Phone"
          />
          <button type="submit" className="button primary" disabled={loading}>
            {loading ? 'Creating...' : 'Create account'}
          </button>
          <Link href="/auth/login" className="button ghost">
            Back to login
          </Link>
        </form>
      </div>

      {error ? (
        <div className="notice panel">
          <p>{error}</p>
        </div>
      ) : null}

      {success ? (
        <div className="notice panel">
          <p>
            {success} <a href="/auth/login">Sign in</a>.
          </p>
        </div>
      ) : null}
    </section>
  )
}
