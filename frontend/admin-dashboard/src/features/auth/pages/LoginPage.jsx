import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../useAuth";

export function LoginPage() {
  const { isAuthenticated, loginPassword } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("admin@noura.local");
  const [password, setPassword] = useState("Admin123!");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  if (isAuthenticated) {
    return <Navigate to="/admin" replace />;
  }

  const destination = location.state?.from?.pathname || "/admin";

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await loginPassword({ email, password });
      navigate(destination, { replace: true });
    } catch (err) {
      setError(err.message || "Unable to sign in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-wrapper">
      
      {/* Left Branding Panel */}
      <div className="auth-brand-panel">
        <div className="brand-content">
          <h1 className="brand-logo">Noura</h1>
          <p className="brand-tagline">Enterprise Control Center</p>

          <div className="brand-meta">
            <p>Secure administrative platform</p>
            <p>Connected to backend:</p>
            <code>http://localhost:8080</code>
          </div>
        </div>
      </div>

      {/* Right Login Panel */}
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
                placeholder="admin@noura.local"
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
                "Sign In"
              )}
            </button>

          </form>

          {error && (
            <div className="auth-error" role="alert">
              {error}
            </div>
          )}

          <footer className="login-footer">
            <p className="seed-label">Seeded Admin</p>
            <p className="mono">admin@noura.local / Admin123!</p>
          </footer>

        </div>
      </div>
    </div>
  );
}