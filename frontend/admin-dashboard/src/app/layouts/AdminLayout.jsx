import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from '../../features/auth/useAuth'
import { ADMIN_ROLES, hasAnyRole, hasCapability } from '../../shared/auth/roles'
import { NAV_SECTIONS, ADMIN_ONLY_ITEMS } from '../navigation'
import { Icon } from '../../shared/ui/Icon'
import { useTheme } from '../../shared/ui/ThemeProvider'
import { getUnreadCount } from '../../shared/api/endpoints/notificationsApi'
import { CommandPalette } from '../../shared/ui/CommandPalette'

const SIDEBAR_COLLAPSE_KEY = 'noura.admin.sidebar.collapsed'

function safeReadCollapsed() {
  try {
    return localStorage.getItem(SIDEBAR_COLLAPSE_KEY) === 'true'
  } catch (_) {
    return false
  }
}

function safeWriteCollapsed(value) {
  try {
    localStorage.setItem(SIDEBAR_COLLAPSE_KEY, value ? 'true' : 'false')
  } catch (_) {
    // ignore
  }
}

function initialsForUser(auth) {
  const fullName = String(auth?.fullName || '').trim()
  if (fullName) {
    const parts = fullName.split(/\s+/).slice(0, 2)
    const letters = parts.map((part) => part[0]).join('')
    return letters.toUpperCase()
  }
  const email = String(auth?.email || '').trim()
  if (email) {
    return email.slice(0, 2).toUpperCase()
  }
  return 'AD'
}

function navClass({ isActive }) {
  return `side-link${isActive ? ' active' : ''}`
}

export function AdminLayout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = hasAnyRole(auth?.roles, ADMIN_ROLES)
  const { theme, toggleTheme } = useTheme()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => safeReadCollapsed())
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [commandOpen, setCommandOpen] = useState(false)
  const profileRef = useRef(null)
  const [unreadCount, setUnreadCount] = useState(0)

  const visibleSections = useMemo(() => (
    NAV_SECTIONS
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => hasCapability(auth, item.capability))
      }))
      .filter((section) => section.items.length > 0)
  ), [auth])

  const visibleAdminOnlyItems = useMemo(
    () => ADMIN_ONLY_ITEMS.filter((item) => hasCapability(auth, item.capability)),
    [auth]
  )

  const avatarLabel = useMemo(() => initialsForUser(auth), [auth])
  const primaryRole = useMemo(() => (auth?.roles?.[0] ? String(auth.roles[0]) : 'STAFF'), [auth])

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  useEffect(() => {
    let active = true
    getUnreadCount()
      .then((count) => {
        if (!active) return
        setUnreadCount(Number(count || 0))
      })
      .catch(() => {
        if (!active) return
        setUnreadCount(0)
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!profileOpen) return undefined
    const handleOutside = (event) => {
      if (!profileRef.current) return
      if (profileRef.current.contains(event.target)) return
      setProfileOpen(false)
    }
    document.addEventListener('mousedown', handleOutside)
    return () => document.removeEventListener('mousedown', handleOutside)
  }, [profileOpen])

  useEffect(() => {
    const handleKeyDown = (event) => {
      const isMac = navigator.platform?.toLowerCase().includes('mac')
      const wantsSearch = (isMac ? event.metaKey : event.ctrlKey) && event.key.toLowerCase() === 'k'
      if (!wantsSearch) return
      event.preventDefault()
      setCommandOpen(true)
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const toggleSidebarCollapsed = () => {
    setSidebarCollapsed((current) => {
      const next = !current
      safeWriteCollapsed(next)
      return next
    })
  }

  const commandItems = useMemo(() => {
    const navItems = visibleSections.flatMap((section) =>
      section.items.map((item) => ({
        id: item.to,
        label: item.label,
        description: section.label,
        icon: item.icon,
        keywords: `${section.label} ${item.label}`,
        onSelect: () => {
          setCommandOpen(false)
          navigate(item.to)
        }
      }))
    )

    const adminItems = isAdmin
      ? visibleAdminOnlyItems.map((item) => ({
          id: item.to,
          label: item.label,
          description: 'Admin',
          icon: item.icon,
          keywords: `Admin ${item.label}`,
          onSelect: () => {
            setCommandOpen(false)
            navigate(item.to)
          }
        }))
      : []

    const actions = [
      {
        id: 'toggle-theme',
        label: theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
        description: 'Appearance',
        icon: theme === 'dark' ? 'sun' : 'moon',
        keywords: 'theme dark light appearance',
        onSelect: () => {
          toggleTheme()
          setCommandOpen(false)
        }
      },
      {
        id: 'api-docs',
        label: 'Open API docs',
        description: 'Help',
        icon: 'help',
        keywords: 'swagger openapi docs help',
        onSelect: () => {
          window.open('http://localhost:8080/swagger-ui', '_blank', 'noopener,noreferrer')
          setCommandOpen(false)
        }
      },
      {
        id: 'sign-out',
        label: 'Sign out',
        description: 'Session',
        icon: 'users',
        keywords: 'logout sign out session',
        onSelect: handleLogout
      }
    ]

    return [...navItems, ...adminItems, ...actions]
  }, [handleLogout, isAdmin, navigate, theme, toggleTheme, visibleAdminOnlyItems, visibleSections])

  return (
    <div className={`admin-shell${sidebarCollapsed ? ' sidebar-collapsed' : ''}`}>
      {mobileSidebarOpen ? (
        <button
          type="button"
          className="sidebar-backdrop"
          aria-label="Close navigation"
          onClick={() => setMobileSidebarOpen(false)}
        />
      ) : null}

      <aside className={`sidebar${sidebarCollapsed ? ' collapsed' : ''}${mobileSidebarOpen ? ' open' : ''}`}>
        <div className="sidebar-top">
          <div 
            className="brand" 
            onClick={sidebarCollapsed ? toggleSidebarCollapsed : undefined}
            role={sidebarCollapsed ? 'button' : undefined}
            tabIndex={sidebarCollapsed ? 0 : undefined}
            onKeyDown={sidebarCollapsed ? (e) => e.key === 'Enter' && toggleSidebarCollapsed() : undefined}
            aria-label={sidebarCollapsed ? 'Expand sidebar' : undefined}
          >
            <div className="logo-mark" aria-hidden>
              N
            </div>
            {sidebarCollapsed ? null : (
              <div>
                <p className="brand-kicker">Noura</p>
                <h1>Admin</h1>
              </div>
            )}
          </div>

          {!sidebarCollapsed && (
            <button
              type="button"
              className="icon-btn"
              onClick={toggleSidebarCollapsed}
              aria-label="Collapse sidebar"
            >
              <Icon name="chevronDown" />
            </button>
          )}
        </div>

        <nav className="side-nav" aria-label="Primary">
          {visibleSections.map((section) => (
            <div className="nav-group" key={section.label}>
              {sidebarCollapsed ? null : <p className="side-section">{section.label}</p>}
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={navClass}
                  title={sidebarCollapsed ? item.label : undefined}
                  onClick={() => setMobileSidebarOpen(false)}
                >
                  <Icon name={item.icon} className="side-icon" />
                  {sidebarCollapsed ? null : <span>{item.label}</span>}
                </NavLink>
              ))}
            </div>
          ))}

          {isAdmin && visibleAdminOnlyItems.length > 0 ? (
            <div className="nav-group">
              {sidebarCollapsed ? null : <p className="side-section">Admin</p>}
              {visibleAdminOnlyItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={navClass}
                  title={sidebarCollapsed ? item.label : undefined}
                  onClick={() => setMobileSidebarOpen(false)}
                >
                  <Icon name={item.icon} className="side-icon" />
                  {sidebarCollapsed ? null : <span>{item.label}</span>}
                </NavLink>
              ))}
            </div>
          ) : null}
        </nav>

        <div className="sidebar-footer">
          <a className="doc-link" href="http://localhost:8080/swagger-ui" target="_blank" rel="noreferrer">
            {sidebarCollapsed ? <Icon name="help" /> : 'API docs'}
          </a>
        </div>
      </aside>

      <main className="content-shell">
        <header className="topbar app-header">
          <div className="header-left">
            <button
              type="button"
              className="icon-btn mobile-only"
              onClick={() => setMobileSidebarOpen(true)}
              aria-label="Open navigation"
            >
              <Icon name="menu" />
            </button>
            <div className="header-greeting">
              <p className="subtle-meta">Welcome back</p>
              <strong>{auth?.fullName || auth?.email || 'Admin user'}</strong>
            </div>
          </div>

          <button
            type="button"
            className="search-trigger"
            aria-label="Search (Ctrl+K)"
            title="Search (Ctrl+K)"
            onClick={() => setCommandOpen(true)}
          >
            <Icon name="search" className="search-icon" />
            <span className="search-placeholder">Search pages and actions…</span>
            <span className="kbd">Ctrl K</span>
          </button>

          <div className="header-right">
            <button type="button" className="icon-btn" onClick={toggleTheme} aria-label="Toggle theme">
              <Icon name={theme === 'dark' ? 'sun' : 'moon'} />
            </button>

            <NavLink to="/admin/notifications" className="icon-btn" aria-label="Notifications">
              <Icon name="bell" />
              {unreadCount > 0 ? (
                <span className="badge badge-info badge-dot" aria-label={`${unreadCount} unread notifications`}>
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              ) : null}
            </NavLink>

            <a className="icon-btn" href="http://localhost:8080/swagger-ui" target="_blank" rel="noreferrer" aria-label="Help">
              <Icon name="help" />
            </a>

            <div className="profile" ref={profileRef}>
              <button
                type="button"
                className="profile-trigger"
                onClick={() => setProfileOpen((current) => !current)}
                aria-haspopup="menu"
                aria-expanded={profileOpen}
              >
                <span className="avatar" aria-hidden>
                  {avatarLabel}
                </span>
                <span className="profile-meta">
                  <span className="profile-name">{auth?.fullName || auth?.email || 'Admin user'}</span>
                  <span className="profile-role">{primaryRole}</span>
                </span>
                <Icon name="chevronDown" className="profile-chevron" />
              </button>

              {profileOpen ? (
                <div className="menu" role="menu" aria-label="User menu">
                  <button type="button" className="menu-item" role="menuitem" onClick={() => setProfileOpen(false)}>
                    Profile
                  </button>
                  <button type="button" className="menu-item" role="menuitem" onClick={() => setProfileOpen(false)}>
                    Settings
                  </button>
                  <button type="button" className="menu-item danger" role="menuitem" onClick={handleLogout}>
                    Sign out
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </header>

        <section className="page-content">
          <Outlet />
        </section>
      </main>

      <CommandPalette open={commandOpen} onClose={() => setCommandOpen(false)} items={commandItems} />
    </div>
  )
}
