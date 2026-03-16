import { Link } from 'react-router-dom'

export function UnauthorizedPage() {
  return (
    <div className="center-shell">
      <div className="panel narrow">
        <h2>Unauthorized</h2>
        <p>Your current account does not have access to the admin dashboard.</p>
        <Link className="btn btn-primary" to="/login">
          Back to sign in
        </Link>
      </div>
    </div>
  )
}
