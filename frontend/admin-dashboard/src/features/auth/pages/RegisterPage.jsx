import { Navigate, Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../useAuth'

export function RegisterPage() {
  const navigate = useNavigate()
  const { isAuthenticated, register } = useAuth()
  const [form, setForm] = useState({
    username: '',
    email: '',
    fullName: '',
    password: ''
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (isAuthenticated) {
    return <Navigate to="/admin" replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await register(form)
      navigate('/admin', { replace: true })
    } catch (err) {
      setError(err.message || 'Unable to register.')
    } finally {
      setLoading(false)
    }
  }

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <p className="brand-kicker">Noura</p>
        <h1>Create inventory account</h1>
        <p className="login-copy">Self-registration creates a viewer account. Admin role elevation still happens through the inventory module.</p>

        <form className="stack-form" onSubmit={handleSubmit}>
          <label>
            Username
            <input
              value={form.username}
              onChange={(event) => updateField('username', event.target.value)}
              placeholder="jane.ops"
              required
            />
          </label>

          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              placeholder="jane.ops@noura.local"
              required
            />
          </label>

          <label>
            Full name
            <input
              value={form.fullName}
              onChange={(event) => updateField('fullName', event.target.value)}
              placeholder="Jane Operations"
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={(event) => updateField('password', event.target.value)}
              placeholder="Choose a password"
              required
            />
          </label>

          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Creating account...' : 'Register'}
          </button>
        </form>

        {error ? <p className="status-error">{error}</p> : null}
        <p className="subtle-meta">Already have a token-enabled user? <Link className="inline-link" to="/login">Sign in</Link>.</p>
      </div>
    </div>
  )
}
