import { useEffect, useMemo, useState } from 'react'
import { listAdminUsers, updateAdminUser } from '../shared/api/endpoints/adminApi'
import { listAdminAuthorizationAuditLogs } from '../shared/api/endpoints/adminAuthorizationApi'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'
import { SortableHeader } from '../shared/ui/SortableHeader'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/UsersPage.css'

const ROLE_TYPES = ['ADMIN', 'CUSTOMER', 'B2B']
const DEFAULT_USER_FILTERS = { search: '', enabled: '', role: '' }

function rolesToFlags(roles = []) {
  const set = new Set(Array.isArray(roles) ? roles : [])
  return Object.fromEntries(ROLE_TYPES.map((value) => [value, set.has(value)]))
}

function selectedRoles(flags) {
  return ROLE_TYPES.filter((value) => Boolean(flags?.[value]))
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function formatTimelineDay(value) {
  if (!value) return 'Unknown date'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown date'
  return date.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' })
}

function formatTimelineTime(value) {
  if (!value) return '--:--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--:--'
  return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function truncate(value, max = 180) {
  const text = String(value || '')
  if (!text) return ''
  if (text.length <= max) return text
  return `${text.slice(0, max)}...`
}

function isUserEventError(event) {
  const outcome = normalizeText(event?.outcome)
  if (outcome && !['success', 'ok', 'completed'].includes(outcome)) {
    return true
  }
  const actionType = normalizeText(event?.actionType)
  return ['fail', 'error', 'deny', 'reject', 'timeout', 'abort'].some((token) => actionType.includes(token))
}

export function UsersPage() {
  const [loadingUsers, setLoadingUsers] = useState(true)
  const [loadingActivity, setLoadingActivity] = useState(false)
  const [usersInitialized, setUsersInitialized] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  useToastFeedback({ successMessage: flash, errorMessage: error })

  const [usersPage, setUsersPage] = useState({ content: [], totalElements: 0 })
  const [filters, setFilters] = useState(DEFAULT_USER_FILTERS)
  const [userSort, setUserSort] = useState({ sortBy: 'createdAt', direction: 'desc' })

  const [selectedUserId, setSelectedUserId] = useState('')
  const [draft, setDraft] = useState({ enabled: true, roles: rolesToFlags([]) })
  const [activityPage, setActivityPage] = useState({ content: [], totalElements: 0 })
  const [activityQuery, setActivityQuery] = useState('')
  const [activityOutcome, setActivityOutcome] = useState('')
  const [activityOnlyErrors, setActivityOnlyErrors] = useState(false)
  const [selectedActivityId, setSelectedActivityId] = useState('')

  const selectedUser = useMemo(
    () => usersPage.content.find((item) => String(item.id) === String(selectedUserId)) || null,
    [selectedUserId, usersPage.content]
  )

  const activityEvents = useMemo(
    () => (activityPage.content || []).map((event) => ({ ...event, isError: isUserEventError(event) })),
    [activityPage.content]
  )

  const activityOutcomeOptions = useMemo(
    () => Array.from(new Set([...activityEvents.map((event) => event.outcome).filter(Boolean), activityOutcome].filter(Boolean))).sort(),
    [activityEvents, activityOutcome]
  )

  const selectedActivity = useMemo(
    () => activityEvents.find((event) => String(event.id) === String(selectedActivityId)) || null,
    [activityEvents, selectedActivityId]
  )

  const activityTimelineGroups = useMemo(() => {
    const grouped = new Map()
    activityEvents.forEach((event) => {
      const key = event.occurredAt ? String(event.occurredAt).slice(0, 10) : 'unknown'
      if (!grouped.has(key)) {
        grouped.set(key, [])
      }
      grouped.get(key).push(event)
    })
    return Array.from(grouped.entries())
      .sort(([a], [b]) => (a < b ? 1 : -1))
      .map(([key, events]) => ({
        key,
        label: key === 'unknown' ? 'Unknown date' : formatTimelineDay(`${key}T00:00:00Z`),
        events: [...events].sort((left, right) => new Date(right.occurredAt || 0) - new Date(left.occurredAt || 0))
      }))
  }, [activityEvents])

  async function loadUsers() {
    setLoadingUsers(true)
    setError('')
    try {
      const page = await listAdminUsers({
        page: 0,
        size: 100,
        sortBy: userSort.sortBy,
        direction: userSort.direction,
        query: filters.search.trim() || undefined,
        enabled: filters.enabled === '' ? undefined : filters.enabled === 'enabled',
        role: filters.role || undefined
      })
      setUsersPage(page || { content: [], totalElements: 0 })
      if (selectedUserId && !(page?.content || []).some((item) => String(item.id) === String(selectedUserId))) {
        setSelectedUserId('')
        setDraft({ enabled: true, roles: rolesToFlags([]) })
        setActivityPage({ content: [], totalElements: 0 })
        setSelectedActivityId('')
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load users.'))
    } finally {
      setLoadingUsers(false)
      setUsersInitialized(true)
    }
  }

  async function loadUserActivity(userId = selectedUserId) {
    if (!userId) {
      setActivityPage({ content: [], totalElements: 0 })
      setSelectedActivityId('')
      return
    }

    setLoadingActivity(true)
    try {
      const queryParts = [String(userId), activityQuery.trim()].filter(Boolean)
      const page = await listAdminAuthorizationAuditLogs({
        page: 0,
        size: 50,
        sortBy: 'occurredAt',
        direction: 'desc',
        entityType: 'USER',
        query: queryParts.join(' '),
        outcome: activityOutcome || undefined,
        errorsOnly: activityOnlyErrors || undefined
      })
      setActivityPage(page || { content: [], totalElements: 0 })
    } catch (err) {
      setActivityPage({ content: [], totalElements: 0 })
      setError(getErrorMessage(err, 'Failed to load user activity timeline.'))
    } finally {
      setLoadingActivity(false)
    }
  }

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadUsers()
    }, 280)
    return () => window.clearTimeout(timeoutId)
  }, [userSort.sortBy, userSort.direction, filters.search, filters.enabled, filters.role])

  useEffect(() => {
    if (!selectedUserId) {
      setActivityPage({ content: [], totalElements: 0 })
      setSelectedActivityId('')
      return undefined
    }
    const timeoutId = window.setTimeout(() => {
      void loadUserActivity(selectedUserId)
    }, 280)
    return () => window.clearTimeout(timeoutId)
  }, [selectedUserId, activityQuery, activityOutcome, activityOnlyErrors])

  useEffect(() => {
    if (!activityEvents.length) {
      if (selectedActivityId) setSelectedActivityId('')
      return
    }
    if (!selectedActivityId || !activityEvents.some((event) => String(event.id) === String(selectedActivityId))) {
      setSelectedActivityId(activityEvents[0].id)
    }
  }, [activityEvents, selectedActivityId])

  function selectUser(user) {
    setFlash('')
    setError('')
    setSelectedUserId(user.id)
    setDraft({
      enabled: Boolean(user.enabled),
      roles: rolesToFlags(user.roles || [])
    })
    setSelectedActivityId('')
  }

  function clearWorkspace() {
    setFilters(DEFAULT_USER_FILTERS)
    setSelectedUserId('')
    setDraft({ enabled: true, roles: rolesToFlags([]) })
    setActivityQuery('')
    setActivityOutcome('')
    setActivityOnlyErrors(false)
    setActivityPage({ content: [], totalElements: 0 })
    setSelectedActivityId('')
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
      await loadUserActivity(selectedUserId)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to update user.'))
    } finally {
      setSaving(false)
    }
  }

  if (loadingUsers && !usersInitialized) {
    return <Spinner label="Loading users..." />
  }

  return (
    <div className="page users-page">
      <div className="page-head">
        <h2>Users</h2>
        <p>Manage platform users, apply server-side filters, and track user-role governance activity.</p>
      </div>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>User list</h3>
              <p>Server-filtered list from `/api/v1/admin/users`.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={loadUsers}>
              Refresh
            </button>
          </div>

          <div className="filters users-filters">
            <label className="users-search">
              Search
              <input
                value={filters.search}
                onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))}
                placeholder="Email, name, phone, id..."
              />
            </label>
            <label>
              Status
              <select
                value={filters.enabled}
                onChange={(event) => setFilters((current) => ({ ...current, enabled: event.target.value }))}
              >
                <option value="">All</option>
                <option value="enabled">Enabled</option>
                <option value="disabled">Disabled</option>
              </select>
            </label>
            <label>
              Role
              <select
                value={filters.role}
                onChange={(event) => setFilters((current) => ({ ...current, role: event.target.value }))}
              >
                <option value="">All roles</option>
                {ROLE_TYPES.map((role) => (
                  <option key={role} value={role}>{role}</option>
                ))}
              </select>
            </label>
            <button className="btn btn-outline" type="button" onClick={clearWorkspace}>
              Clear
            </button>
            <p className="users-filter-meta">
              Showing {usersPage.content.length} of {usersPage.totalElements || 0} users
            </p>
          </div>

          <div className="table-wrap">
            <table className="users-table">
              <thead>
                <tr>
                  <SortableHeader label="User" field="email" sortBy={userSort.sortBy} direction={userSort.direction} onSort={(f, d) => setUserSort({ sortBy: f, direction: d })} />
                  <th>Roles</th>
                  <SortableHeader label="Enabled" field="enabled" sortBy={userSort.sortBy} direction={userSort.direction} onSort={(f, d) => setUserSort({ sortBy: f, direction: d })} />
                </tr>
              </thead>
              <tbody>
                {usersPage.content.length ? (
                  usersPage.content.map((user) => (
                    <tr
                      key={user.id}
                      className={[
                        String(user.id) === String(selectedUserId) ? 'row-selected' : '',
                        !user.enabled ? 'row-error' : ''
                      ].filter(Boolean).join(' ')}
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
                        <span className={`badge ${user.enabled ? 'badge-success' : 'badge-danger'}`}>
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
                <p>Update enabled status and assign platform roles with immediate save feedback.</p>
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

      <section className="panel users-activity-panel">
        <div className="section-head">
          <div>
            <h3>User activity timeline</h3>
            <p>Authorization and role-assignment events scoped to the selected user.</p>
          </div>
          {selectedUser ? <span className="badge badge-info mono">{selectedUser.id}</span> : null}
        </div>

        {!selectedUser ? (
          <p className="empty-copy">Select a user to view their activity timeline.</p>
        ) : (
          <>
            <div className="users-activity-toolbar">
              <label className="users-activity-search">
                Search activity
                <input
                  type="search"
                  value={activityQuery}
                  onChange={(event) => setActivityQuery(event.target.value)}
                  placeholder="Action, actor, outcome, correlation, details"
                />
              </label>
              <label>
                Outcome
                <select value={activityOutcome} onChange={(event) => setActivityOutcome(event.target.value)}>
                  <option value="">All outcomes</option>
                  {activityOutcomeOptions.map((outcome) => (
                    <option key={outcome} value={outcome}>{outcome}</option>
                  ))}
                </select>
              </label>
              <label className="checkbox-row users-activity-errors">
                <input
                  type="checkbox"
                  checked={activityOnlyErrors}
                  onChange={(event) => setActivityOnlyErrors(event.target.checked)}
                />
                Errors only
              </label>
              <button className="btn btn-outline btn-sm" type="button" onClick={() => loadUserActivity(selectedUserId)}>
                Refresh activity
              </button>
              <p className="users-activity-meta">
                Showing {activityPage.content.length} of {activityPage.totalElements || 0} events
              </p>
            </div>

            {loadingActivity ? (
              <Spinner label="Loading user activity..." />
            ) : (
              <div className="users-activity-workspace">
                <aside className="users-activity-timeline">
                  {activityTimelineGroups.length ? (
                    activityTimelineGroups.map((group) => (
                      <div key={group.key} className="users-activity-group">
                        <p className="users-activity-date">{group.label}</p>
                        <ul className="users-activity-list">
                          {group.events.map((event) => (
                            <li key={event.id}>
                              <button
                                type="button"
                                className={`users-activity-item ${String(selectedActivityId) === String(event.id) ? 'active' : ''} ${event.isError ? 'is-error' : ''}`}
                                onClick={() => setSelectedActivityId(event.id)}
                              >
                                <span className="users-activity-time">{formatTimelineTime(event.occurredAt)}</span>
                                <span className="users-activity-action">{event.actionType}</span>
                                <span className="users-activity-entity">{event.outcome || 'UNKNOWN'} | {event.actorEmail || 'Unknown actor'}</span>
                              </button>
                            </li>
                          ))}
                        </ul>
                      </div>
                    ))
                  ) : (
                    <div className="empty-copy">No activity events match the current filters.</div>
                  )}
                </aside>

                <div className="users-activity-main">
                  <div className="table-wrap">
                    <table className="users-activity-table">
                      <thead>
                        <tr>
                          <th>Occurred</th>
                          <th>Action</th>
                          <th>Outcome</th>
                          <th>Actor</th>
                          <th>Correlation</th>
                          <th>Details</th>
                        </tr>
                      </thead>
                      <tbody>
                        {activityEvents.length ? (
                          activityEvents.map((event) => (
                            <tr
                              key={event.id}
                              className={[
                                String(selectedActivityId) === String(event.id) ? 'row-active' : '',
                                event.isError ? 'row-error' : ''
                              ].filter(Boolean).join(' ')}
                              onClick={() => setSelectedActivityId(event.id)}
                            >
                              <td>{formatDateTime(event.occurredAt)}</td>
                              <td className="mono">{event.actionType}</td>
                              <td>
                                <span className={`badge ${event.isError ? 'badge-danger' : 'badge-success'}`}>
                                  {event.outcome || 'UNKNOWN'}
                                </span>
                              </td>
                              <td>{event.actorEmail || 'Unknown actor'}</td>
                              <td className="mono">{event.correlationId || '—'}</td>
                              <td>{truncate(event.detailsJson) || 'No details payload.'}</td>
                            </tr>
                          ))
                        ) : (
                          <tr>
                            <td colSpan="6" className="empty-row">No user activity events recorded yet.</td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>

                  {selectedActivity ? (
                    <div className="users-activity-detail">
                      <div className="users-activity-detail-head">
                        <strong>{selectedActivity.actionType}</strong>
                        <span className={`badge ${selectedActivity.isError ? 'badge-danger' : 'badge-success'}`}>
                          {selectedActivity.outcome || 'UNKNOWN'}
                        </span>
                      </div>
                      <div className="subtle-meta">
                        {selectedActivity.actorEmail || 'Unknown actor'} | {formatDateTime(selectedActivity.occurredAt)}
                      </div>
                      <pre className="users-activity-json">{selectedActivity.detailsJson || 'No details payload.'}</pre>
                    </div>
                  ) : null}
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}
