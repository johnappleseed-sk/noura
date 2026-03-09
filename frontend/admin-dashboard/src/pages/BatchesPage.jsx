import { useEffect, useMemo, useState } from 'react'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { getBatchLot, listBatchLots } from '../shared/api/endpoints/reportsApi'
import { formatDecimal, formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const SORT_FIELDS = [
  { value: 'expiryDate', label: 'Expiry date' },
  { value: 'receivedAt', label: 'Received at' },
  { value: 'lotNumber', label: 'Lot number' },
  { value: 'status', label: 'Status' },
  { value: 'createdAt', label: 'Created at' }
]

const STATUS_SUGGESTIONS = ['ACTIVE', 'QUARANTINED', 'EXPIRED', 'INACTIVE']

const DEFAULT_FILTERS = {
  productId: '',
  status: '',
  expiringBefore: '',
  expiringAfter: '',
  size: '20',
  sortBy: 'expiryDate',
  direction: 'asc'
}

function formatLocalDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`))
}

function shortId(value, len = 12) {
  if (!value) return '-'
  if (value.length <= len) return value
  return `${value.slice(0, len)}…`
}

export function BatchesPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [productsPage, setProductsPage] = useState({ content: [] })
  const [batchPage, setBatchPage] = useState({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })
  const [selectedId, setSelectedId] = useState('')
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState(null)

  const selected = useMemo(
    () => batchPage.content.find((item) => item.id === selectedId) || null,
    [batchPage.content, selectedId]
  )

  async function loadReferences() {
    try {
      const products = await listProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' })
      setProductsPage(products || { content: [] })
    } catch (err) {
      setError(err.message || 'Failed to load products for batch filters.')
    }
  }

  async function load({ nextPage = batchPage.page, nextFilters = filters } = {}) {
    setLoading(true)
    setError('')
    try {
      const result = await listBatchLots({
        productId: nextFilters.productId || undefined,
        status: nextFilters.status || undefined,
        expiringBefore: nextFilters.expiringBefore || undefined,
        expiringAfter: nextFilters.expiringAfter || undefined,
        page: nextPage,
        size: Number(nextFilters.size || 20),
        sortBy: nextFilters.sortBy || 'expiryDate',
        direction: nextFilters.direction || 'asc'
      })

      setBatchPage(
        result || {
          content: [],
          page: 0,
          size: Number(nextFilters.size || 20),
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true
        }
      )

      const keepSelected = selectedId && (result?.content || []).some((item) => item.id === selectedId)
      if (!keepSelected) {
        setSelectedId('')
        setDetail(null)
      }
    } catch (err) {
      setError(err.message || 'Failed to load batch lots.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadReferences()
    load({ nextPage: 0, nextFilters: DEFAULT_FILTERS })
  }, [])

  useEffect(() => {
    if (!selectedId) return
    let cancelled = false
    setDetailLoading(true)
    getBatchLot(selectedId)
      .then((response) => {
        if (cancelled) return
        setDetail(response || null)
      })
      .catch((err) => {
        if (cancelled) return
        setError(err.message || 'Failed to load batch lot detail.')
      })
      .finally(() => {
        if (cancelled) return
        setDetailLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [selectedId])

  async function applyFilters(event) {
    event.preventDefault()
    setFlash('')
    await load({ nextPage: 0, nextFilters: filters })
  }

  async function resetFilters() {
    setFlash('')
    setFilters(DEFAULT_FILTERS)
    setSelectedId('')
    setDetail(null)
    await load({ nextPage: 0, nextFilters: DEFAULT_FILTERS })
  }

  async function goPrev() {
    if (batchPage.first) return
    await load({ nextPage: Math.max(0, batchPage.page - 1), nextFilters: filters })
  }

  async function goNext() {
    if (batchPage.last) return
    await load({ nextPage: batchPage.page + 1, nextFilters: filters })
  }

  if (loading) {
    return <Spinner label="Loading batch lots..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Batch lots</h2>
        <p>Query received lot numbers with expiry visibility and on-hand quantities. Batch lots are created during inbound movements for batch-tracked SKUs.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p>
              {batchPage.totalElements ? `${batchPage.totalElements} batch lot(s)` : 'No batches matched yet.'} · Page {batchPage.page + 1} of{' '}
              {Math.max(1, batchPage.totalPages || 1)}
            </p>
          </div>
          <div className="inline-actions wrap">
            <button className="btn btn-outline btn-sm" type="button" onClick={goPrev} disabled={batchPage.first}>
              Prev
            </button>
            <button className="btn btn-outline btn-sm" type="button" onClick={goNext} disabled={batchPage.last}>
              Next
            </button>
          </div>
        </div>

        <form className="filters four-up" onSubmit={applyFilters}>
          <label>
            Product
            <select value={filters.productId} onChange={(event) => setFilters((c) => ({ ...c, productId: event.target.value }))}>
              <option value="">All products</option>
              {(productsPage.content || []).map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} ({product.sku})
                </option>
              ))}
            </select>
          </label>

          <label>
            Status
            <input
              list="batch-statuses"
              value={filters.status}
              onChange={(event) => setFilters((c) => ({ ...c, status: event.target.value }))}
              placeholder="ACTIVE"
            />
            <datalist id="batch-statuses">
              {STATUS_SUGGESTIONS.map((status) => (
                <option key={status} value={status} />
              ))}
            </datalist>
          </label>

          <label>
            Expiring before
            <input
              type="date"
              value={filters.expiringBefore}
              onChange={(event) => setFilters((c) => ({ ...c, expiringBefore: event.target.value }))}
            />
          </label>

          <label>
            Expiring after
            <input
              type="date"
              value={filters.expiringAfter}
              onChange={(event) => setFilters((c) => ({ ...c, expiringAfter: event.target.value }))}
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
              <option value="asc">Ascending</option>
              <option value="desc">Descending</option>
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
              <h3>Lot detail</h3>
              <p>{selected ? 'Inspect full lot metadata and quantities.' : 'Select a batch lot to view detail.'}</p>
            </div>
            {selected ? (
              <span className="badge badge-muted mono" title={selected.id}>
                {shortId(selected.id, 10)}
              </span>
            ) : null}
          </div>

          {detailLoading ? <p className="empty-copy">Loading detail…</p> : null}

          {detail ? (
            <div className="stack-list">
              <div className="stack-card">
                <div className="stack-card-top">
                  <strong className="mono">{detail.lotNumber}</strong>
                  <span className="badge badge-info">{detail.status || 'ACTIVE'}</span>
                </div>
                <p className="subtle-meta">
                  {detail.productName} · <span className="mono">{detail.productSku}</span>
                </p>
                <p className="subtle-meta">Expiry {formatLocalDate(detail.expiryDate)}</p>
                <p className="subtle-meta">Manufactured {formatLocalDate(detail.manufacturedAt)}</p>
                <p className="subtle-meta">Received {formatDateTime(detail.receivedAt)}</p>
                <p className="subtle-meta mono" title={detail.supplierBatchRef || ''}>
                  Supplier ref {detail.supplierBatchRef || '-'}
                </p>
                <p className="subtle-meta">On hand {formatDecimal(detail.quantityOnHand, 2)}</p>
                <p className="subtle-meta">Available {formatDecimal(detail.quantityAvailable, 2)}</p>
                {detail.notes ? <p className="subtle-meta">{detail.notes}</p> : null}
              </div>
            </div>
          ) : (
            <p className="empty-copy">No batch selected.</p>
          )}
        </section>

        <section className="panel">
          <div className="section-head compact">
            <div>
              <h3>Batch lots</h3>
              <p>Click a row to load the batch lot record.</p>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Expiry</th>
                  <th>Lot</th>
                  <th>Product</th>
                  <th>Status</th>
                  <th>Available</th>
                  <th>On hand</th>
                </tr>
              </thead>
              <tbody>
                {batchPage.content.length ? (
                  batchPage.content.map((item) => (
                    <tr
                      key={item.id}
                      className={item.id === selectedId ? 'row-active' : ''}
                      onClick={() => setSelectedId(item.id)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td>{formatLocalDate(item.expiryDate)}</td>
                      <td className="mono">{item.lotNumber}</td>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td className="mono">{item.status || '-'}</td>
                      <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                      <td>{formatDecimal(item.quantityOnHand, 2)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-row" colSpan={6}>
                      No batch lots found.
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
