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
      const resolved = preserveSelection && selectedId && (data?.content || []).some((item) => item.id === selectedId) ? selectedId : ''
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
    <div className="page">
      <div className="page-head">
        <h2>Audit logs</h2>
        <p>Immutable history for critical changes: who did what, when, and what changed. Only admins can access this view.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p>
              {auditPage.totalElements ? `${auditPage.totalElements} event(s)` : 'No events matched yet.'} · Page {auditPage.page + 1} of{' '}
              {Math.max(1, auditPage.totalPages || 1)}
            </p>
          </div>
          <div className="inline-actions wrap">
            <button className="btn btn-outline btn-sm" type="button" onClick={goPrev} disabled={auditPage.first}>
              Prev
            </button>
            <button className="btn btn-outline btn-sm" type="button" onClick={goNext} disabled={auditPage.last}>
              Next
            </button>
          </div>
        </div>

        <form className="filters four-up" onSubmit={applyFilters}>
          <label>
            Entity type
            <input value={filters.entityType} onChange={(event) => setFilters((c) => ({ ...c, entityType: event.target.value }))} />
          </label>
          <label>
            Entity id
            <input value={filters.entityId} onChange={(event) => setFilters((c) => ({ ...c, entityId: event.target.value }))} />
          </label>
          <label>
            Action code
            <input value={filters.actionCode} onChange={(event) => setFilters((c) => ({ ...c, actionCode: event.target.value }))} />
          </label>
          <label>
            Actor email
            <input value={filters.actorEmail} onChange={(event) => setFilters((c) => ({ ...c, actorEmail: event.target.value }))} />
          </label>

          <label>
            Occurred from
            <input
              type="datetime-local"
              value={filters.occurredFrom}
              onChange={(event) => setFilters((c) => ({ ...c, occurredFrom: event.target.value }))}
            />
          </label>
          <label>
            Occurred to
            <input
              type="datetime-local"
              value={filters.occurredTo}
              onChange={(event) => setFilters((c) => ({ ...c, occurredTo: event.target.value }))}
            />
          </label>
          <label>
            Sort by
            <select value={filters.sortBy} onChange={(event) => setFilters((c) => ({ ...c, sortBy: event.target.value }))}>
              {SORT_FIELDS.map((field) => (
                <option key={field.value} value={field.value}>
                  {field.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Direction
            <select value={filters.direction} onChange={(event) => setFilters((c) => ({ ...c, direction: event.target.value }))}>
              <option value="desc">Newest first</option>
              <option value="asc">Oldest first</option>
            </select>
          </label>
          <label>
            Page size
            <select value={filters.size} onChange={(event) => setFilters((c) => ({ ...c, size: event.target.value }))}>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>

          <div className="inline-actions wrap">
            <button className="btn btn-primary" type="submit">
              Apply
            </button>
            <button className="btn btn-outline" type="button" onClick={resetFilters}>
              Reset
            </button>
          </div>
        </form>
      </section>

      <div className="workbench-grid">
        <section className="panel">
          <div className="section-head compact">
            <div>
              <h3>Event detail</h3>
              <p>{selected ? 'Review metadata and state diffs.' : 'Select an audit event to inspect JSON snapshots.'}</p>
            </div>
            {selected ? (
              <span className="badge badge-muted mono" title={selected.id}>
                {shortId(selected.id, 12)}
              </span>
            ) : null}
          </div>

          {selected ? (
            <div className="stack-list">
              <div className="stack-card">
                <div className="stack-card-top">
                  <strong className="mono">{selected.actionCode}</strong>
                  <span className="badge badge-info">{selected.entityType}</span>
                </div>
                <p className="subtle-meta">Occurred {formatDateTime(selected.occurredAt)}</p>
                <p className="subtle-meta">Actor {selected.actorEmail || selected.actorUserId || '-'}</p>
                <p className="subtle-meta mono" title={selected.entityId || ''}>
                  Entity id {selected.entityId || '-'}
                </p>
                <p className="subtle-meta mono" title={selected.correlationId || ''}>
                  Correlation {selected.correlationId || '-'}
                </p>
                <p className="subtle-meta mono" title={selected.ipAddress || ''}>
                  IP {selected.ipAddress || '-'}
                </p>
                <p className="subtle-meta mono" title={selected.eventHash || ''}>
                  Hash {shortId(selected.eventHash, 18)}
                </p>
              </div>

              <details className="audit-details" open>
                <summary>After state</summary>
                <pre className="result-code">{formatJson(selected.afterStateJson) || '—'}</pre>
              </details>

              <details className="audit-details">
                <summary>Before state</summary>
                <pre className="result-code">{formatJson(selected.beforeStateJson) || '—'}</pre>
              </details>

              <details className="audit-details">
                <summary>Metadata</summary>
                <pre className="result-code">{formatJson(selected.metadataJson) || '—'}</pre>
              </details>

              <details className="audit-details">
                <summary>User agent</summary>
                <pre className="result-code">{selected.userAgent || '—'}</pre>
              </details>
            </div>
          ) : (
            <p className="empty-copy">No event selected.</p>
          )}
        </section>

        <section className="panel">
          <div className="section-head compact">
            <div>
              <h3>Events</h3>
              <p>Click a row to inspect details and JSON diffs.</p>
            </div>
          </div>

          <div className="table-wrap">
            <table>
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
                      style={{ cursor: 'pointer' }}
                    >
                      <td>{formatDateTime(item.occurredAt)}</td>
                      <td>{item.actorEmail || '-'}</td>
                      <td className="mono">{item.actionCode || '-'}</td>
                      <td className="mono">{item.entityType || '-'}</td>
                      <td className="mono" title={item.entityId || ''}>
                        {shortId(item.entityId, 12)}
                      </td>
                      <td className="mono" title={item.correlationId || ''}>
                        {shortId(item.correlationId, 12)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-row" colSpan={6}>
                      No audit events found.
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
