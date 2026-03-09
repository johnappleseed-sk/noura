import { useEffect, useState } from 'react'
import { listApprovalQueue, listAdminUsers, updateAdminUser, updateApproval } from '../shared/api/endpoints/adminApi'
import { endpointCatalog, endpointCount } from '../features/control-center/endpointCatalog'
import { executeRawApiRequest } from '../shared/api/rawRequest'
import { Spinner } from '../shared/ui/Spinner'

const ROLE_OPTIONS = ['ADMIN', 'CUSTOMER', 'B2B']
const APPROVAL_STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

function formatCurrency(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2
  }).format(Number(amount || 0))
}

function formatJson(value) {
  return JSON.stringify(value, null, 2)
}

function parseJsonField(text, fieldLabel) {
  const trimmed = text.trim()
  if (!trimmed) {
    return undefined
  }

  try {
    return JSON.parse(trimmed)
  } catch (_) {
    throw new Error(`${fieldLabel} must be valid JSON.`)
  }
}

function initialWorkbenchState(endpoint) {
  return {
    endpointId: endpoint.id,
    method: endpoint.method,
    path: endpoint.path,
    bodyMode: endpoint.bodyMode,
    withAuth: endpoint.withAuth,
    queryText: endpoint.defaultQuery ? formatJson(endpoint.defaultQuery) : '',
    bodyText: endpoint.defaultBody !== undefined ? formatJson(endpoint.defaultBody) : ''
  }
}

function buildUserDrafts(users) {
  return Object.fromEntries(
    users.map((user) => [
      user.id,
      {
        roles: Array.isArray(user.roles) ? user.roles : [],
        enabled: Boolean(user.enabled)
      }
    ])
  )
}

function buildApprovalDrafts(approvals) {
  return Object.fromEntries(
    approvals.map((approval) => [
      approval.id,
      {
        status: approval.status || 'PENDING',
        reviewerNotes: approval.reviewerNotes || ''
      }
    ])
  )
}

const flattenedEndpointCatalog = endpointCatalog.flatMap((group) =>
  group.endpoints.map((endpoint) => ({
    ...endpoint,
    groupKey: group.key,
    groupLabel: group.label,
    groupDescription: group.description
  }))
)

export function ControlCenterPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [users, setUsers] = useState([])
  const [approvals, setApprovals] = useState([])
  const [userDrafts, setUserDrafts] = useState({})
  const [approvalDrafts, setApprovalDrafts] = useState({})
  const [savingUserId, setSavingUserId] = useState(null)
  const [savingApprovalId, setSavingApprovalId] = useState(null)
  const [userSearch, setUserSearch] = useState('')
  const [approvalFilter, setApprovalFilter] = useState('')
  const [endpointSearch, setEndpointSearch] = useState('')
  const [workbenchState, setWorkbenchState] = useState(() => initialWorkbenchState(flattenedEndpointCatalog[0]))
  const [workbenchLoading, setWorkbenchLoading] = useState(false)
  const [workbenchError, setWorkbenchError] = useState('')
  const [workbenchResult, setWorkbenchResult] = useState(null)

  async function load() {
    setLoading(true)
    setError('')

    try {
      const [userPage, approvalList] = await Promise.all([
        listAdminUsers({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }),
        listApprovalQueue()
      ])

      const nextUsers = userPage?.content || []
      const nextApprovals = Array.isArray(approvalList) ? approvalList : []

      setUsers(nextUsers)
      setApprovals(nextApprovals)
      setUserDrafts(buildUserDrafts(nextUsers))
      setApprovalDrafts(buildApprovalDrafts(nextApprovals))
    } catch (err) {
      setError(err.message || 'Unable to load control center data.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function applyEndpoint(endpoint) {
    setWorkbenchError('')
    setWorkbenchState(initialWorkbenchState(endpoint))
  }

  async function saveUser(userId) {
    const draft = userDrafts[userId]
    if (!draft) {
      return
    }

    setSavingUserId(userId)
    setFlash('')
    setError('')

    try {
      const updated = await updateAdminUser(userId, draft)
      setUsers((current) => current.map((user) => (user.id === userId ? updated : user)))
      setUserDrafts((current) => ({
        ...current,
        [userId]: {
          roles: Array.isArray(updated.roles) ? updated.roles : [],
          enabled: Boolean(updated.enabled)
        }
      }))
      setFlash('User updated.')
    } catch (err) {
      setError(err.message || 'Unable to update user.')
    } finally {
      setSavingUserId(null)
    }
  }

  async function saveApproval(approvalId) {
    const draft = approvalDrafts[approvalId]
    if (!draft) {
      return
    }

    setSavingApprovalId(approvalId)
    setFlash('')
    setError('')

    try {
      const updated = await updateApproval(approvalId, draft)
      setApprovals((current) => current.map((approval) => (approval.id === approvalId ? updated : approval)))
      setApprovalDrafts((current) => ({
        ...current,
        [approvalId]: {
          status: updated.status || 'PENDING',
          reviewerNotes: updated.reviewerNotes || ''
        }
      }))
      setFlash('Approval updated.')
    } catch (err) {
      setError(err.message || 'Unable to update approval.')
    } finally {
      setSavingApprovalId(null)
    }
  }

  async function executeWorkbench() {
    setWorkbenchLoading(true)
    setWorkbenchError('')
    setWorkbenchResult(null)

    try {
      const query = parseJsonField(workbenchState.queryText, 'Query JSON')
      const body = workbenchState.bodyMode === 'none' ? undefined : parseJsonField(workbenchState.bodyText, 'Body JSON')
      const startedAt = performance.now()
      const response = await executeRawApiRequest({
        method: workbenchState.method,
        path: workbenchState.path,
        query,
        body,
        bodyMode: workbenchState.bodyMode,
        withAuth: workbenchState.withAuth
      })

      setWorkbenchResult({
        ...response,
        durationMs: Math.round(performance.now() - startedAt)
      })
    } catch (err) {
      setWorkbenchError(err.message || 'Unable to execute request.')
    } finally {
      setWorkbenchLoading(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading control center..." />
  }

  const normalizedUserSearch = userSearch.trim().toLowerCase()
  const filteredUsers = users.filter((user) => {
    if (!normalizedUserSearch) {
      return true
    }

    return (
      user.fullName?.toLowerCase().includes(normalizedUserSearch) ||
      user.email?.toLowerCase().includes(normalizedUserSearch) ||
      user.id?.toLowerCase().includes(normalizedUserSearch)
    )
  })

  const filteredApprovals = approvals.filter((approval) => !approvalFilter || approval.status === approvalFilter)

  const filteredCatalog = endpointCatalog
    .map((group) => ({
      ...group,
      endpoints: group.endpoints.filter((endpoint) => {
        const search = endpointSearch.trim().toLowerCase()
        if (!search) {
          return true
        }

        return (
          group.label.toLowerCase().includes(search) ||
          endpoint.method.toLowerCase().includes(search) ||
          endpoint.path.toLowerCase().includes(search)
        )
      })
    }))
    .filter((group) => group.endpoints.length)

  const selectedEndpoint =
    flattenedEndpointCatalog.find((endpoint) => endpoint.id === workbenchState.endpointId) || flattenedEndpointCatalog[0]

  const pendingApprovals = approvals.filter((approval) => approval.status === 'PENDING').length
  const disabledUsers = users.filter((user) => !user.enabled).length

  return (
    <div className="page">
      <div className="page-head">
        <h2>Control center</h2>
        <p>Manage users and approvals directly, then fall through to a live request workbench for the broader backend surface.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card-grid">
        <article className="metric-card">
          <h3>Users</h3>
          <strong>{users.length}</strong>
          <p>{disabledUsers} currently disabled accounts in the visible admin slice.</p>
        </article>
        <article className="metric-card">
          <h3>Approvals</h3>
          <strong>{approvals.length}</strong>
          <p>{pendingApprovals} approval requests still waiting on action.</p>
        </article>
        <article className="metric-card">
          <h3>API endpoints</h3>
          <strong>{endpointCount}</strong>
          <p>Controller routes wired into the dashboard workbench.</p>
        </article>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>User access</h3>
            <p>Update enabled state and assigned roles through the admin user APIs.</p>
          </div>
          <button className="btn btn-outline" onClick={load}>
            Refresh
          </button>
        </div>

        <div className="filters">
          <label className="grow">
            Search users
            <input
              value={userSearch}
              onChange={(event) => setUserSearch(event.target.value)}
              placeholder="Filter by name, email, or id"
            />
          </label>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>User</th>
                <th>Roles</th>
                <th>Enabled</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length ? (
                filteredUsers.map((user) => {
                  const draft = userDrafts[user.id] || { roles: [], enabled: false }
                  return (
                    <tr key={user.id}>
                      <td>
                        <strong>{user.fullName || 'Unnamed user'}</strong>
                        <div className="subtle-meta">{user.email || 'No email on file'}</div>
                        <div className="mono">{user.id}</div>
                      </td>
                      <td>
                        <div className="toggle-group">
                          {ROLE_OPTIONS.map((role) => {
                            const active = draft.roles.includes(role)
                            return (
                              <button
                                key={role}
                                type="button"
                                className={`toggle-chip${active ? ' active' : ''}`}
                                onClick={() =>
                                  setUserDrafts((current) => ({
                                    ...current,
                                    [user.id]: {
                                      ...draft,
                                      roles: active
                                        ? draft.roles.filter((item) => item !== role)
                                        : [...draft.roles, role]
                                    }
                                  }))
                                }
                              >
                                {role}
                              </button>
                            )
                          })}
                        </div>
                      </td>
                      <td>
                        <label className="checkbox-row">
                          <input
                            type="checkbox"
                            checked={draft.enabled}
                            onChange={(event) =>
                              setUserDrafts((current) => ({
                                ...current,
                                [user.id]: {
                                  ...draft,
                                  enabled: event.target.checked
                                }
                              }))
                            }
                          />
                          Active
                        </label>
                      </td>
                      <td>
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => saveUser(user.id)}
                          disabled={savingUserId === user.id}
                        >
                          {savingUserId === user.id ? 'Saving...' : 'Save'}
                        </button>
                      </td>
                    </tr>
                  )
                })
              ) : (
                <tr>
                  <td colSpan="4" className="empty-row">No users match the current filter.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>B2B approvals</h3>
            <p>Approve or reject requests without leaving the dashboard.</p>
          </div>
        </div>

        <div className="filters">
          <label>
            Approval status
            <select value={approvalFilter} onChange={(event) => setApprovalFilter(event.target.value)}>
              <option value="">All</option>
              {APPROVAL_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Approval</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Reviewer notes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredApprovals.length ? (
                filteredApprovals.map((approval) => {
                  const draft = approvalDrafts[approval.id] || { status: 'PENDING', reviewerNotes: '' }
                  return (
                    <tr key={approval.id}>
                      <td>
                        <div className="mono">{approval.id}</div>
                        <div className="subtle-meta">Requester {approval.requesterId || '-'}</div>
                        <div className="subtle-meta">Order {approval.orderId || '-'}</div>
                      </td>
                      <td>{formatCurrency(approval.amount)}</td>
                      <td>
                        <select
                          value={draft.status}
                          onChange={(event) =>
                            setApprovalDrafts((current) => ({
                              ...current,
                              [approval.id]: {
                                ...draft,
                                status: event.target.value
                              }
                            }))
                          }
                        >
                          {APPROVAL_STATUSES.map((status) => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <textarea
                          className="code-editor compact"
                          rows="3"
                          value={draft.reviewerNotes}
                          onChange={(event) =>
                            setApprovalDrafts((current) => ({
                              ...current,
                              [approval.id]: {
                                ...draft,
                                reviewerNotes: event.target.value
                              }
                            }))
                          }
                          placeholder="Optional reviewer notes"
                        />
                      </td>
                      <td>
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => saveApproval(approval.id)}
                          disabled={savingApprovalId === approval.id}
                        >
                          {savingApprovalId === approval.id ? 'Saving...' : 'Save'}
                        </button>
                      </td>
                    </tr>
                  )
                })
              ) : (
                <tr>
                  <td colSpan="5" className="empty-row">No approvals match the current filter.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>API workbench</h3>
            <p>Browse the backend route map, seed a request, then execute it against the active environment.</p>
          </div>
        </div>

        <div className="workbench-grid">
          <div className="workbench-sidebar">
            <label>
              Search endpoints
              <input
                value={endpointSearch}
                onChange={(event) => setEndpointSearch(event.target.value)}
                placeholder="Method, path, or domain"
              />
            </label>

            <div className="endpoint-groups">
              {filteredCatalog.map((group) => (
                <section key={group.key} className="endpoint-group">
                  <h4>{group.label}</h4>
                  <p>{group.description}</p>
                  <div className="endpoint-list">
                    {group.endpoints.map((endpoint) => (
                      <button
                        key={endpoint.id}
                        type="button"
                        className={`endpoint-item${workbenchState.endpointId === endpoint.id ? ' active' : ''}`}
                        onClick={() => applyEndpoint(endpoint)}
                      >
                        <span className={`method-pill method-${endpoint.method.toLowerCase()}`}>{endpoint.method}</span>
                        <code>{endpoint.path}</code>
                      </button>
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </div>

          <div className="workbench-console">
            <div className="console-head">
              <div>
                <span className="badge badge-info">{selectedEndpoint.groupLabel}</span>
                <h3>{selectedEndpoint.path}</h3>
                <p>
                  {selectedEndpoint.path.includes('{')
                    ? 'Replace any bracketed path variables before executing.'
                    : 'This endpoint is ready to execute as-is, subject to auth and payload requirements.'}
                </p>
              </div>
              <button className="btn btn-outline" onClick={() => applyEndpoint(selectedEndpoint)}>
                Reset
              </button>
            </div>

            {workbenchError ? <div className="alert alert-error">{workbenchError}</div> : null}

            <div className="filters">
              <label>
                Method
                <input value={workbenchState.method} readOnly />
              </label>
              <label className="grow">
                Path
                <input
                  value={workbenchState.path}
                  onChange={(event) =>
                    setWorkbenchState((current) => ({
                      ...current,
                      path: event.target.value
                    }))
                  }
                />
              </label>
            </div>

            <div className="filters three-up">
              <label>
                Body mode
                <select
                  value={workbenchState.bodyMode}
                  onChange={(event) =>
                    setWorkbenchState((current) => ({
                      ...current,
                      bodyMode: event.target.value
                    }))
                  }
                >
                  <option value="none">None</option>
                  <option value="json">JSON</option>
                  <option value="form">Form URL encoded</option>
                </select>
              </label>
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={workbenchState.withAuth}
                  onChange={(event) =>
                    setWorkbenchState((current) => ({
                      ...current,
                      withAuth: event.target.checked
                    }))
                  }
                />
                Attach stored admin token
              </label>
            </div>

            <div className="editor-grid">
              <label>
                Query params JSON
                <textarea
                  className="code-editor"
                  rows="10"
                  value={workbenchState.queryText}
                  onChange={(event) =>
                    setWorkbenchState((current) => ({
                      ...current,
                      queryText: event.target.value
                    }))
                  }
                  placeholder='{"page":0,"size":20}'
                />
              </label>
              <label>
                Request body JSON
                <textarea
                  className="code-editor"
                  rows="10"
                  value={workbenchState.bodyText}
                  onChange={(event) =>
                    setWorkbenchState((current) => ({
                      ...current,
                      bodyText: event.target.value
                    }))
                  }
                  disabled={workbenchState.bodyMode === 'none'}
                  placeholder='{"key":"value"}'
                />
              </label>
            </div>

            <div className="inline-actions wrap">
              <button className="btn btn-primary" onClick={executeWorkbench} disabled={workbenchLoading}>
                {workbenchLoading ? 'Running...' : 'Execute request'}
              </button>
              <button className="btn btn-outline" onClick={() => setWorkbenchResult(null)}>
                Clear result
              </button>
            </div>

            {workbenchResult ? (
              <div className="result-panel">
                <div className="inline-actions wrap">
                  <span className={`badge ${workbenchResult.ok ? 'badge-success' : 'badge-danger'}`}>
                    HTTP {workbenchResult.status}
                  </span>
                  <span className="badge badge-muted">{workbenchResult.durationMs} ms</span>
                  <span className="badge badge-muted">{workbenchResult.contentType || 'no content type'}</span>
                </div>
                <pre className="result-code">
                  {typeof workbenchResult.data === 'string'
                    ? workbenchResult.data || workbenchResult.statusText
                    : formatJson(workbenchResult.data)}
                </pre>
              </div>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  )
}
