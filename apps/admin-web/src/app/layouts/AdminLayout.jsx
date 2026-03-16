import { useEffect, useMemo, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { NAV_SECTIONS, ADMIN_ONLY_ITEMS } from '../navigation'
import { ADMIN_ROLES, hasAnyRole, hasCapability } from '../../shared/auth/roles'
import { useAuth } from '../../features/auth/useAuth'
import { Icon } from '../../shared/ui/Icon'
import { apiHost } from '../../shared/api/httpClient'
import './AdminLayout.css'

function initials(value) {
  const raw = String(value || '').trim()
  if (!raw) return 'NA'
  const parts = raw.split(/\s+/).filter(Boolean)
  const head = parts[0]?.[0] || ''
  const tail = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return `${head}${tail}`.toUpperCase() || 'NA'
}

function useOutsideClick(ref, onOutside) {
  useEffect(() => {
    function onPointerDown(event) {
      if (!ref.current) return
      if (ref.current.contains(event.target)) return
      onOutside?.()
    }

    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [ref, onOutside])
}

export function AdminLayout() {
  const { auth, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try {
      return window.localStorage.getItem('admin.sidebar.collapsed') === 'true'
    } catch (_) {
      return false
    }
  })
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const profileRef = useRef(null)

  useOutsideClick(profileRef, () => setProfileMenuOpen(false))

  useEffect(() => {
    setMobileSidebarOpen(false)
    setProfileMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    try {
      window.localStorage.setItem('admin.sidebar.collapsed', String(sidebarCollapsed))
    } catch (_) {
      // ignore
    }
  }, [sidebarCollapsed])

  const sessionRoleLabel = useMemo(() => {
    const role = (auth?.roles || [])[0] || 'USER'
    return String(role).replaceAll('_', ' ')
  }, [auth?.roles])

  const visibleSections = useMemo(() => {
    return NAV_SECTIONS.map((section) => {
      const items = (section.items || []).filter((item) => hasCapability(auth, item.capability))
      return { ...section, items }
    }).filter((section) => section.items.length)
  }, [auth])

  const adminItems = useMemo(() => {
    const allowed = hasAnyRole(auth?.roles || [], ADMIN_ROLES)
    if (!allowed) return []
    return ADMIN_ONLY_ITEMS.filter((item) => hasCapability(auth, item.capability))
  }, [auth])

  const shellClassName = `admin-shell${sidebarCollapsed ? ' sidebar-collapsed' : ''}`
  const sidebarClassName = `sidebar${sidebarCollapsed ? ' collapsed' : ''}${mobileSidebarOpen ? ' open' : ''}`

  function toggleSidebar() {
    setSidebarCollapsed((value) => !value)
  }

  function openMobileSidebar() {
    setMobileSidebarOpen(true)
  }

  function closeMobileSidebar() {
    setMobileSidebarOpen(false)
  }

  function onLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const docsUrl = `${apiHost.replace(/\/+$/, '')}/swagger-ui`
  const userName = auth?.fullName || auth?.email || 'Admin'

  return (
    <div className={shellClassName}>
      {mobileSidebarOpen ? (
        <button className="sidebar-backdrop" type="button" onClick={closeMobileSidebar} aria-label="Close navigation" />
      ) : null}

      <aside className={sidebarClassName} aria-label="Admin navigation">
        <div className="sidebar-top">
          <a className="brand" href="/admin" onClick={(e) => { e.preventDefault(); navigate('/admin') }}>
            <div className="logo-mark" aria-hidden>
              N
            </div>
            <div className="brand-text">
              <div className="brand-kicker">Noura</div>
              <h1>Admin</h1>
              <div className="brand-badges">
                <span className="badge">Enterprise</span>
                <span className="badge subtle">{String(import.meta.env.MODE || 'prod')}</span>
              </div>
            </div>
          </a>

          <button className="icon-btn sidebar-toggle" type="button" onClick={toggleSidebar} aria-label="Toggle sidebar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path d="M15 6 9 12l6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>

        <nav className="side-nav">
          {visibleSections.map((section) => (
            <div className="nav-group" key={section.label}>
              <div className="side-section-row">
                <div className="side-section-meta">
                  <div className="side-section">{section.label}</div>
                  <span className="side-section-badge">Core</span>
                </div>
                <span className="side-section-count" title="Visible pages">
                  {section.items.length}
                </span>
              </div>

              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={Boolean(item.end)}
                  className={({ isActive }) => `side-link${isActive ? ' active' : ''}`}
                >
                  <Icon name={item.icon} size={20} className="side-icon" />
                  <span className="side-link-label">{item.label}</span>
                </NavLink>
              ))}
            </div>
          ))}

          {adminItems.length ? (
            <div className="nav-group">
              <div className="side-section-row">
                <div className="side-section-meta">
                  <div className="side-section">Admin</div>
                  <span className="side-section-badge">Restricted</span>
                </div>
                <span className="side-section-count">{adminItems.length}</span>
              </div>
              {adminItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `side-link${isActive ? ' active' : ''}`}
                >
                  <Icon name={item.icon} size={20} className="side-icon" />
                  <span className="side-link-label">{item.label}</span>
                </NavLink>
              ))}
            </div>
          ) : null}
        </nav>

        <div className="sidebar-footer">
          <a className="doc-link" href={docsUrl} target="_blank" rel="noreferrer">
            <Icon name="help" size={20} className="doc-link-icon" />
            <span className="doc-link-label">API docs</span>
          </a>
        </div>
      </aside>

      <div className="content-shell">
        <header className="topbar">
          <div className="header-left">
            <div className="header-greeting">
              <p className="subtle-meta">Operations console</p>
              <div className="header-user-name">{userName}</div>
            </div>
          </div>

          <div className="header-right" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <button className="icon-btn mobile-only" type="button" onClick={openMobileSidebar} aria-label="Open navigation">
              <Icon name="menu" size={20} />
            </button>

            <button className="search-trigger" type="button" onClick={() => navigate('/admin')} aria-label="Search (not implemented)">
              <Icon name="search" size={18} className="search-icon" />
              <span className="search-placeholder">Search pages, entities, or IDs</span>
              <span className="kbd">Ctrl K</span>
            </button>

            <div className="profile" ref={profileRef}>
              <button
                className="profile-trigger"
                type="button"
                onClick={() => setProfileMenuOpen((value) => !value)}
                aria-haspopup="menu"
                aria-expanded={profileMenuOpen}
              >
                <div className="avatar" aria-hidden>
                  {initials(userName)}
                </div>
                <div className="profile-meta">
                  <div className="profile-name">{userName}</div>
                  <div className="profile-role">{sessionRoleLabel}</div>
                </div>
                <Icon name="chevronDown" size={18} className="profile-chevron" />
              </button>

              {profileMenuOpen ? (
                <div className="menu" role="menu" aria-label="Profile menu">
                  <button className="menu-item" type="button" onClick={() => navigate('/admin')}>
                    Go to dashboard
                  </button>
                  <div className="menu-divider" />
                  <button className="menu-item danger" type="button" onClick={onLogout}>
                    Sign out
                  </button>
                  <div className="menu-hint">Session expires automatically after inactivity.</div>
                </div>
              ) : null}
            </div>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
