import { Link } from "react-router-dom"
import '../styles/pages/NotFoundPage.css'

export function NotFoundPage() {
  return (
    <div className="center-shell notfound-shell">
      <div className="panel narrow notfound-panel">

        <div className="notfound-code">404</div>

        <h2>Page Not Found</h2>

        <p className="notfound-text">
          The page you are looking for does not exist or may have been removed
          from this admin environment.
        </p>

        <div className="notfound-actions">
          <Link className="btn btn-primary" to="/admin">
            Go to Dashboard
          </Link>

          <Link className="btn btn-outline" to="/admin/products">
            Browse Products
          </Link>

          <Link className="btn btn-ghost" to="/">
            Back to Home
          </Link>
        </div>

      </div>
    </div>
  )
}

export default NotFoundPage
