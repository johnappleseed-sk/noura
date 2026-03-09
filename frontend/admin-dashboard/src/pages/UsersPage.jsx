import { useEffect, useMemo, useState } from 'react'
import { listAdminUsers, updateAdminUser } from '../shared/api/endpoints/adminApi'
import { Spinner } from '../shared/ui/Spinner'

const ROLE_TYPES = ['ADMIN', 'CUSTOMER', 'B2B']

function rolesToFlags(roles = []) {
  const set = new Set(Array.isArray(roles) ? roles : [])
  return Object.fromEntries(ROLE_TYPES.map((value) => [value, set.has(value)]))
}

function selectedRoles(flags) {
  return ROLE_TYPES.filter((value) => Boolean(flags?.[value]))
}

export function UsersPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [usersPage, setUsersPage] = useState({ content: [], totalElements: 0 })
  const [filters, setFilters] = useState({ search: '' })

  const [selectedUserId, setSelectedUserId] = useState('')
  const [draft, setDraft] = useState({ enabled: true, roles: rolesToFlags([]) })

  const selectedUser = usersPage.content.find((item) => String(item.id) === String(selectedUserId)) || null

  const visibleUsers = useMemo(() => {
    const q = filters.search.trim().toLowerCase()
    if (!q) return usersPage.content
    return usersPage.content.filter((user) => {
      return (
        String(user.email || '').toLowerCase().includes(q) ||
        String(user.fullName || '').toLowerCase().includes(q) ||
        String(user.phone || '').toLowerCase().includes(q) ||
        String(user.id || '').toLowerCase().includes(q)
      )
    })
  }, [filters.search, usersPage.content])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const page = await listAdminUsers({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' })
      setUsersPage(page || { content: [], totalElements: 0 })
      if (selectedUserId && !(page?.content || []).some((item) => String(item.id) === String(selectedUserId))) {
        setSelectedUserId('')
        setDraft({ enabled: true, roles: rolesToFlags([]) })
      }
    } catch (err) {
      setError(err.message || 'Failed to load users.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function selectUser(user) {
    setFlash('')
    setError('')
    setSelectedUserId(user.id)
    setDraft({
      enabled: Boolean(user.enabled),
      roles: rolesToFlags(user.roles || [])
    })
  }

  function resetSelection() {
    setSelectedUserId('')
    setDraft({ enabled: true, roles: rolesToFlags([]) })
  }

  async function saveUser() {
    if (!selectedUserId) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        enabled: Boolean(draft.enabled),
        roles: selectedRoles(draft.roles)
      }
      const updated = await updateAdminUser(selectedUserId, payload)
      setFlash('User updated.')
      setUsersPage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(selectedUserId) ? updated : item))
      }))
      setDraft({
        enabled: Boolean(updated.enabled),
        roles: rolesToFlags(updated.roles || [])
      })
    } catch (err) {
      setError(err.message || 'Unable to update user.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading users..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Users</h2>
        <p>Manage platform users (enabled flag and roles). Inventory roles are handled by the warehouse module.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>User list</h3>
              <p>Admin-visible list from `/api/v1/admin/users`.</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={load}>
              Refresh
            </button>
          </div>

          <div className="filters">
            <label>
              Search
              <input
                value={filters.search}
                onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))}
                placeholder="Email, name, phone, id..."
              />
            </label>
            <button className="btn btn-outline" onClick={resetSelection}>
              Clear
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Roles</th>
                  <th>Enabled</th>
                </tr>
              </thead>
              <tbody>
                {visibleUsers.length ? (
                  visibleUsers.map((user) => (
                    <tr
                      key={user.id}
                      className={String(user.id) === String(selectedUserId) ? 'row-selected' : ''}
                      onClick={() => selectUser(user)}
                      role="button"
                      tabIndex={0}
                    >
                      <td>
                        <strong>{user.fullName || user.email}</strong>
                        <div className="subtle-meta mono">{user.email}</div>
                      </td>
                      <td className="mono">{(user.roles || []).join(', ') || '-'}</td>
                      <td>
                        <span className={`badge ${user.enabled ? 'badge-success' : 'badge-muted'}`}>
                          {user.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="3" className="empty-row">No users found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{selectedUser ? 'Edit user' : 'Select a user'}</h3>
              <p>Update enabled status and assign platform roles.</p>
            </div>
          </div>

          {selectedUser ? (
            <>
              <ul className="simple-list">
                <li>
                  <strong>User id</strong>: <span className="mono">{selectedUser.id}</span>
                </li>
                <li>
                  <strong>Email</strong>: <span className="mono">{selectedUser.email}</span>
                </li>
                <li>
                  <strong>Name</strong>: {selectedUser.fullName || '-'}
                </li>
                <li>
                  <strong>Phone</strong>: {selectedUser.phone || '-'}
                </li>
              </ul>

              <div className="divider" />

              <div className="toggle-row">
                <label className="toggle">
                  <input
                    type="checkbox"
                    checked={Boolean(draft.enabled)}
                    onChange={(event) => setDraft((current) => ({ ...current, enabled: event.target.checked }))}
                  />
                  Enabled
                </label>
              </div>

              <h4 style={{ marginTop: 0 }}>Roles</h4>
              <div className="toggle-row">
                {ROLE_TYPES.map((role) => (
                  <label className="toggle" key={role}>
                    <input
                      type="checkbox"
                      checked={Boolean(draft.roles?.[role])}
                      onChange={(event) =>
                        setDraft((current) => ({
                          ...current,
                          roles: { ...(current.roles || {}), [role]: event.target.checked }
                        }))
                      }
                    />
                    {role}
                  </label>
                ))}
              </div>

              <div className="inline-actions wrap" style={{ marginTop: 14 }}>
                <button className="btn btn-primary" disabled={saving} onClick={saveUser}>
                  {saving ? 'Saving...' : 'Save changes'}
                </button>
                <button className="btn btn-outline" disabled={saving} onClick={() => selectUser(selectedUser)}>
                  Reset
                </button>
              </div>
            </>
          ) : (
            <p className="empty-copy">Pick a user from the list to edit.</p>
          )}
        </section>
      </div>
    </div>
  )
}
