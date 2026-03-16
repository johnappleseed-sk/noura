import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../useAuth'
import { useToastFeedback } from '../../../shared/ui/useToastFeedback'
import { apiHost } from '../../../shared/api/httpClient'

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

export function LoginPage() {
  const { isAuthenticated, loginPassword } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useToastFeedback({ errorMessage: error })

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
      setError(getErrorMessage(err, 'Unable to sign in.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-brand-panel">
        <div className="brand-content">
          <h1 className="brand-logo">Noura</h1>
          <p className="brand-tagline">Enterprise Control Center</p>

          <div className="brand-meta">
            <p>Secure administrative platform</p>
            <p>Connected to backend:</p>
            <code>{apiHost}</code>
          </div>
        </div>
      </div>

      <div className="auth-form-panel">
        <div className="login-card">
          <header className="login-header">
            <h2>Admin Sign In</h2>
            <p className="login-subtitle">
              Access the Noura administration dashboard
            </p>
          </header>

          <form className="login-form" onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="email">Email</label>

              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                value={email}
                placeholder="admin@company.com"
                disabled={loading}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>

              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                value={password}
                placeholder="Enter password"
                disabled={loading}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button
              className="btn-primary"
              type="submit"
              disabled={loading}
            >
              {loading ? (
                <span className="loading">
                  <span className="spinner"></span>
                  Signing in...
                </span>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          {error ? <p className="subtle-meta">{error}</p> : null}
        </div>
      </div>
    </div>
  )
}
