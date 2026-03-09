import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import { ADMIN_ROLES, hasAnyRole } from '../../shared/auth/roles'

function navClass({ isActive }) {
  return `side-link${isActive ? ' active' : ''}`
}

export function AdminLayout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = hasAnyRole(auth?.roles, ADMIN_ROLES)

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="admin-shell">
      <aside className="sidebar">
        <div className="brand">
          <p className="brand-kicker">Noura</p>
          <h1>Admin Console</h1>
          <p className="brand-copy">Unified back office for commerce operations and warehouse inventory flows.</p>
        </div>

        <nav className="side-nav">
          <p className="side-section">Overview</p>
          <NavLink to="/admin" end className={navClass}>Dashboard</NavLink>

          <p className="side-section">Commerce</p>
          <NavLink to="/admin/commerce/catalog" className={navClass}>Catalog</NavLink>
          <NavLink to="/admin/orders" className={navClass}>Orders</NavLink>
          <NavLink to="/admin/returns" className={navClass}>Returns</NavLink>
          <NavLink to="/admin/stores" className={navClass}>Stores</NavLink>
          <NavLink to="/admin/pricing" className={navClass}>Pricing</NavLink>
          <NavLink to="/admin/users" className={navClass}>Users</NavLink>
          <NavLink to="/admin/notifications" className={navClass}>Notifications</NavLink>

          <p className="side-section">Warehouse</p>
          <NavLink to="/admin/warehouse/catalog" className={navClass}>Inventory catalog</NavLink>
          <NavLink to="/admin/warehouse/locations" className={navClass}>Locations</NavLink>
          <NavLink to="/admin/warehouse/stock" className={navClass}>Stock</NavLink>
          <NavLink to="/admin/warehouse/movements" className={navClass}>Movements</NavLink>
          <NavLink to="/admin/warehouse/batches" className={navClass}>Batches</NavLink>
          <NavLink to="/admin/warehouse/serials" className={navClass}>Serials</NavLink>
          <NavLink to="/admin/warehouse/reports" className={navClass}>Reports</NavLink>
          {isAdmin ? (
            <>
              <NavLink to="/admin/warehouse/webhooks" className={navClass}>Webhooks</NavLink>
              <NavLink to="/admin/warehouse/audit-logs" className={navClass}>Audit logs</NavLink>
            </>
          ) : null}

          <p className="side-section">Tools</p>
          <NavLink to="/admin/tools/control-center" className={navClass}>Control center</NavLink>
        </nav>

        <a className="doc-link" href="http://localhost:8080/swagger-ui" target="_blank" rel="noreferrer">
          Open API docs
        </a>
      </aside>

      <main className="content-shell">
        <header className="topbar">
          <div>
            <strong>{auth?.fullName || auth?.email || 'Admin user'}</strong>
            <p className="topbar-meta">{auth?.email || 'Authenticated session'}</p>
          </div>
          <button className="btn btn-outline" onClick={handleLogout}>
            Sign out
          </button>
        </header>

        <section className="page-content">
          <Outlet />
        </section>
      </main>
    </div>
  )
}
