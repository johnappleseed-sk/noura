import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="center-shell">
      <div className="panel narrow">
        <h2>Page not found</h2>
        <p>The page you requested does not exist in the current admin build.</p>
        <Link className="btn btn-primary" to="/admin">
          Go to dashboard
        </Link>
      </div>
    </div>
  )
}
