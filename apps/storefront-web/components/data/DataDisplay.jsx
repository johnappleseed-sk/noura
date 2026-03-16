'use client'

import { useState, useMemo } from 'react'

/**
 * DataTable — Sortable, paginated table.
 */
export function DataTable({ columns = [], data = [], pageSize = 10, className = '' }) {
  const [sortCol, setSortCol] = useState(null)
  const [sortDir, setSortDir] = useState('asc')
  const [page, setPage] = useState(0)

  const sorted = useMemo(() => {
    if (!sortCol) return data
    const col = columns.find(c => c.key === sortCol)
    const arr = [...data]
    arr.sort((a, b) => {
      const va = col?.sortValue ? col.sortValue(a) : a[sortCol]
      const vb = col?.sortValue ? col.sortValue(b) : b[sortCol]
      if (va < vb) return sortDir === 'asc' ? -1 : 1
      if (va > vb) return sortDir === 'asc' ? 1 : -1
      return 0
    })
    return arr
  }, [data, sortCol, sortDir, columns])

  const totalPages = Math.ceil(sorted.length / pageSize)
  const paged = sorted.slice(page * pageSize, (page + 1) * pageSize)

  const toggleSort = (key) => {
    if (sortCol === key) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(key)
      setSortDir('asc')
    }
    setPage(0)
  }

  return (
    <div className={`data-table-wrap ${className}`}>
      <table className="data-table">
        <thead>
          <tr>
            {columns.map(col => (
              <th
                key={col.key}
                onClick={col.sortable !== false ? () => toggleSort(col.key) : undefined}
                style={col.sortable !== false ? { cursor: 'pointer', userSelect: 'none' } : undefined}
              >
                {col.label}
                {sortCol === col.key && (
                  <span style={{ marginLeft: 4 }}>{sortDir === 'asc' ? '↑' : '↓'}</span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {paged.length === 0 ? (
            <tr><td colSpan={columns.length} style={{ textAlign: 'center', padding: 32, color: 'var(--muted)' }}>No data</td></tr>
          ) : (
            paged.map((row, i) => (
              <tr key={row.id ?? i}>
                {columns.map(col => (
                  <td key={col.key}>{col.render ? col.render(row) : row[col.key]}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {totalPages > 1 && (
        <div className="data-table-pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span style={{ fontSize: '0.85rem' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}
    </div>
  )
}

/**
 * KPICard — Key metric display card.
 */
export function KPICard({ label, value, change, changeType = 'neutral', icon, className = '' }) {
  return (
    <div className={`kpi-card ${className}`}>
      {icon && <div className="kpi-icon">{icon}</div>}
      <div className="kpi-body">
        <span className="kpi-label">{label}</span>
        <span className="kpi-value">{value}</span>
        {change != null && (
          <span className={`kpi-change ${changeType}`}>
            {changeType === 'positive' ? '↑' : changeType === 'negative' ? '↓' : ''} {change}
          </span>
        )}
      </div>
    </div>
  )
}

/**
 * ActivityFeed — Chronological activity list.
 */
export function ActivityFeed({ items = [], className = '' }) {
  return (
    <div className={`activity-feed ${className}`}>
      {items.map((item, i) => (
        <div key={item.id || i} className="activity-item">
          <div className="activity-dot" />
          <div className="activity-content">
            <div className="activity-message">{item.message}</div>
            <div className="activity-time">{item.time}</div>
          </div>
        </div>
      ))}
    </div>
  )
}

/**
 * StatGrid — Grid of KPI cards.
 */
export function StatGrid({ stats = [], className = '' }) {
  return (
    <div className={`stat-grid ${className}`} style={{ display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
      {stats.map((s, i) => (
        <KPICard key={i} {...s} />
      ))}
    </div>
  )
}
