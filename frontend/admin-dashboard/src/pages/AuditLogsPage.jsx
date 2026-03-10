import { useEffect, useMemo, useState } from 'react'
import { listAuditLogs } from '../shared/api/endpoints/auditLogsApi'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const SORT_FIELDS = [
  { value: 'occurredAt', label: 'Occurred at' },
  { value: 'entityType', label: 'Entity type' },
  { value: 'actionCode', label: 'Action code' },
  { value: 'actorEmail', label: 'Actor email' }
]

const DEFAULT_FILTERS = {
  entityType: '',
  entityId: '',
  actionCode: '',
  actorEmail: '',
  occurredFrom: '',
  occurredTo: '',
  size: '20',
  sortBy: 'occurredAt',
  direction: 'desc'
}

function toApiInstant(value) {
  if (!value) return undefined
  return new Date(value).toISOString()
}

function formatJson(value) {
  if (!value) return ''
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function shortId(value, len = 10) {
  if (!value) return '-'
  if (value.length <= len) return value
  return `${value.slice(0, len)}…`
}

export function AuditLogsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [filters, setFilters] = useState(DEFAULT_FILTERS)

  const [auditPage, setAuditPage] = useState({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })

  const [selectedId, setSelectedId] = useState('')

  const selected = useMemo(
    () => auditPage.content.find((item) => item.id === selectedId) || null,
    [auditPage.content, selectedId]
  )

  async function load({ nextPage = auditPage.page, nextFilters = filters, preserveSelection = true } = {}) {
    setLoading(true)
    setError('')

    try {
      const data = await listAuditLogs({
        entityType: nextFilters.entityType || undefined,
        entityId: nextFilters.entityId || undefined,
        actionCode: nextFilters.actionCode || undefined,
        actorEmail: nextFilters.actorEmail || undefined,
        occurredFrom: toApiInstant(nextFilters.occurredFrom),
        occurredTo: toApiInstant(nextFilters.occurredTo),
        page: nextPage,
        size: Number(nextFilters.size || 20),
        sortBy: nextFilters.sortBy || 'occurredAt',
        direction: nextFilters.direction || 'desc'
      })

      setAuditPage(
        data || {
          content: [],
          page: 0,
          size: Number(nextFilters.size || 20),
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true
        }
      )

      if (!preserveSelection) {
        setSelectedId('')
        return
      }

      const resolved =
        preserveSelection &&
        selectedId &&
        (data?.content || []).some((item) => item.id === selectedId)
          ? selectedId
          : ''

      setSelectedId(resolved)

    } catch (err) {
      setError(err.message || 'Failed to load audit logs.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function applyFilters(event) {
    event.preventDefault()
    setFlash('')
    await load({ nextPage: 0, nextFilters: filters, preserveSelection: false })
  }

  async function resetFilters() {
    setFlash('')
    setFilters(DEFAULT_FILTERS)
    await load({ nextPage: 0, nextFilters: DEFAULT_FILTERS, preserveSelection: false })
  }

  async function goPrev() {
    if (auditPage.first) return
    await load({ nextPage: Math.max(0, auditPage.page - 1), nextFilters: filters })
  }

  async function goNext() {
    if (auditPage.last) return
    await load({ nextPage: auditPage.page + 1, nextFilters: filters })
  }

  if (loading) {
    return <Spinner label="Loading audit logs..." />
  }

  return (
    <div className="audit-page">

      <div className="page-head">
        <h2>Audit Logs</h2>
        <p className="subtle-meta">
          Immutable history of system actions. Only administrators can access this view.
        </p>
      </div>

      {flash && <div className="alert alert-success">{flash}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* FILTER PANEL */}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p className="subtle-meta">
              {auditPage.totalElements} events • Page {auditPage.page + 1} of {Math.max(1, auditPage.totalPages || 1)}
            </p>
          </div>

          <div className="pagination-controls">
            <button className="btn btn-outline btn-sm" onClick={goPrev} disabled={auditPage.first}>
              ← Prev
            </button>

            <button className="btn btn-outline btn-sm" onClick={goNext} disabled={auditPage.last}>
              Next →
            </button>
          </div>
        </div>

        <form className="filters-grid" onSubmit={applyFilters}>

          <label>
            Entity type
            <input
              value={filters.entityType}
              onChange={(e) => setFilters((c) => ({ ...c, entityType: e.target.value }))}
            />
          </label>

          <label>
            Entity id
            <input
              value={filters.entityId}
              onChange={(e) => setFilters((c) => ({ ...c, entityId: e.target.value }))}
            />
          </label>

          <label>
            Action code
            <input
              value={filters.actionCode}
              onChange={(e) => setFilters((c) => ({ ...c, actionCode: e.target.value }))}
            />
          </label>

          <label>
            Actor email
            <input
              value={filters.actorEmail}
              onChange={(e) => setFilters((c) => ({ ...c, actorEmail: e.target.value }))}
            />
          </label>

          <label>
            Occurred from
            <input
              type="datetime-local"
              value={filters.occurredFrom}
              onChange={(e) => setFilters((c) => ({ ...c, occurredFrom: e.target.value }))}
            />
          </label>

          <label>
            Occurred to
            <input
              type="datetime-local"
              value={filters.occurredTo}
              onChange={(e) => setFilters((c) => ({ ...c, occurredTo: e.target.value }))}
            />
          </label>

          <label>
            Sort by
            <select
              value={filters.sortBy}
              onChange={(e) => setFilters((c) => ({ ...c, sortBy: e.target.value }))}
            >
              {SORT_FIELDS.map((field) => (
                <option key={field.value} value={field.value}>
                  {field.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Direction
            <select
              value={filters.direction}
              onChange={(e) => setFilters((c) => ({ ...c, direction: e.target.value }))}
            >
              <option value="desc">Newest first</option>
              <option value="asc">Oldest first</option>
            </select>
          </label>

          <label>
            Page size
            <select
              value={filters.size}
              onChange={(e) => setFilters((c) => ({ ...c, size: e.target.value }))}
            >
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>

          <div className="filter-actions">
            <button className="btn btn-primary" type="submit">
              Apply
            </button>

            <button className="btn btn-outline" type="button" onClick={resetFilters}>
              Reset
            </button>
          </div>
        </form>
      </section>

      {/* MAIN WORKBENCH */}

      <div className="audit-grid">

        {/* EVENT DETAILS */}

        <section className="panel">

          <div className="section-head compact">
            <h3>Event Details</h3>

            {selected && (
              <span className="badge badge-muted mono" title={selected.id}>
                {shortId(selected.id, 12)}
              </span>
            )}
          </div>

          {selected ? (
            <div className="audit-details-stack">

              <div className="event-meta">

                <p><strong>Action</strong> {selected.actionCode}</p>
                <p><strong>Entity</strong> {selected.entityType}</p>
                <p><strong>Actor</strong> {selected.actorEmail || '-'}</p>
                <p><strong>Occurred</strong> {formatDateTime(selected.occurredAt)}</p>
                <p><strong>Entity ID</strong> {selected.entityId || '-'}</p>
                <p><strong>IP</strong> {selected.ipAddress || '-'}</p>

              </div>

              <details open>
                <summary>After State</summary>
                <pre className="json-viewer">{formatJson(selected.afterStateJson) || '—'}</pre>
              </details>

              <details>
                <summary>Before State</summary>
                <pre className="json-viewer">{formatJson(selected.beforeStateJson) || '—'}</pre>
              </details>

              <details>
                <summary>Metadata</summary>
                <pre className="json-viewer">{formatJson(selected.metadataJson) || '—'}</pre>
              </details>

            </div>
          ) : (
            <p className="empty-copy">Select an audit event to inspect details.</p>
          )}
        </section>

        {/* EVENTS TABLE */}

        <section className="panel">

          <div className="section-head compact">
            <h3>Events</h3>
          </div>

          <div className="table-wrap">
            <table className="audit-table">

              <thead>
                <tr>
                  <th>Occurred</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Entity</th>
                  <th>Entity id</th>
                  <th>Correlation</th>
                </tr>
              </thead>

              <tbody>

                {auditPage.content.length ? (
                  auditPage.content.map((item) => (

                    <tr
                      key={item.id}
                      className={item.id === selectedId ? 'row-active' : ''}
                      onClick={() => setSelectedId(item.id)}
                    >
                      <td>{formatDateTime(item.occurredAt)}</td>
                      <td>{item.actorEmail || '-'}</td>
                      <td className="mono">{item.actionCode}</td>
                      <td className="mono">{item.entityType}</td>
                      <td className="mono">{shortId(item.entityId, 12)}</td>
                      <td className="mono">{shortId(item.correlationId, 12)}</td>
                    </tr>

                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="empty-row">
                      No audit events found
                    </td>
                  </tr>
                )}

              </tbody>
            </table>
          </div>

        </section>

      </div>
    </div>
  )
}