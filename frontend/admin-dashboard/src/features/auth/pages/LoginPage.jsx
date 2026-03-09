import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../useAuth'

export function LoginPage() {
  const { isAuthenticated, loginPassword } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('admin@noura.local')
  const [password, setPassword] = useState('Admin123!')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (isAuthenticated) {
    return <Navigate to="/admin" replace />
  }

  const destination = location.state?.from?.pathname || '/admin'

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await loginPassword({ email, password })
      navigate(destination, { replace: true })
    } catch (err) {
      setError(err.message || 'Unable to sign in.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <p className="brand-kicker">Noura</p>
        <h1>Admin sign in</h1>
        <p className="login-copy">Connect to the Noura monolith on <code>http://localhost:8080</code> using an <strong>ADMIN</strong> account.</p>

        <form className="stack-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="admin@noura.local"
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Password"
              required
            />
          </label>

          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        {error ? <p className="status-error">{error}</p> : null}

        <div className="auth-aside">
          <p className="subtle-meta">Seeded local admin</p>
          <p className="subtle-meta mono">admin@noura.local / Admin123!</p>
          <p className="subtle-meta">Need a non-admin user? Create one via the Control Center after signing in.</p>
        </div>
      </div>
    </div>
  )
}
