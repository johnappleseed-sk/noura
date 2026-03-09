import { useEffect, useMemo, useState } from 'react'
import { listWarehouseBins, listWarehouses } from '../shared/api/endpoints/inventoryLocationsApi'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { getSerialNumber, listSerialNumbers } from '../shared/api/endpoints/reportsApi'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const SORT_FIELDS = [
  { value: 'updatedAt', label: 'Updated at' },
  { value: 'createdAt', label: 'Created at' },
  { value: 'serialNumber', label: 'Serial number' },
  { value: 'serialStatus', label: 'Status' }
]

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'IN_STOCK', label: 'IN_STOCK' },
  { value: 'SOLD', label: 'SOLD' },
  { value: 'ADJUSTED_OUT', label: 'ADJUSTED_OUT' },
  { value: 'RETURNED', label: 'RETURNED' }
]

const DEFAULT_FILTERS = {
  query: '',
  productId: '',
  serialStatus: '',
  warehouseId: '',
  binId: '',
  batchId: '',
  size: '20',
  sortBy: 'updatedAt',
  direction: 'desc'
}

function shortId(value, len = 12) {
  if (!value) return '-'
  if (value.length <= len) return value
  return `${value.slice(0, len)}…`
}

export function SerialsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [productsPage, setProductsPage] = useState({ content: [] })
  const [warehousesPage, setWarehousesPage] = useState({ content: [] })
  const [binsPage, setBinsPage] = useState({ content: [] })
  const [serialPage, setSerialPage] = useState({
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
    () => serialPage.content.find((item) => item.id === selectedId) || null,
    [serialPage.content, selectedId]
  )

  async function loadReferences() {
    try {
      const [products, warehouses] = await Promise.all([
        listProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listWarehouses({ page: 0, size: 100, sortBy: 'name', direction: 'asc' })
      ])
      setProductsPage(products || { content: [] })
      setWarehousesPage(warehouses || { content: [] })
    } catch (err) {
      setError(err.message || 'Failed to load reference lists for serial filters.')
    }
  }

  async function loadBins(warehouseId) {
    if (!warehouseId) {
      setBinsPage({ content: [] })
      return
    }
    try {
      const bins = await listWarehouseBins(warehouseId, { page: 0, size: 100, sortBy: 'binCode', direction: 'asc' })
      setBinsPage(bins || { content: [] })
    } catch (err) {
      setError(err.message || 'Failed to load bins for warehouse filter.')
    }
  }

  async function load({ nextPage = serialPage.page, nextFilters = filters } = {}) {
    setLoading(true)
    setError('')
    try {
      const result = await listSerialNumbers({
        query: nextFilters.query || undefined,
        productId: nextFilters.productId || undefined,
        serialStatus: nextFilters.serialStatus || undefined,
        warehouseId: nextFilters.warehouseId || undefined,
        binId: nextFilters.binId || undefined,
        batchId: nextFilters.batchId || undefined,
        page: nextPage,
        size: Number(nextFilters.size || 20),
        sortBy: nextFilters.sortBy || 'updatedAt',
        direction: nextFilters.direction || 'desc'
      })

      setSerialPage(
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
      setError(err.message || 'Failed to load serial numbers.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadReferences()
    load({ nextPage: 0, nextFilters: DEFAULT_FILTERS })
  }, [])

  useEffect(() => {
    loadBins(filters.warehouseId)
    if (!filters.warehouseId) {
      setFilters((current) => ({ ...current, binId: '' }))
    }
  }, [filters.warehouseId])

  useEffect(() => {
    if (!selectedId) return
    let cancelled = false
    setDetailLoading(true)
    getSerialNumber(selectedId)
      .then((response) => {
        if (cancelled) return
        setDetail(response || null)
      })
      .catch((err) => {
        if (cancelled) return
        setError(err.message || 'Failed to load serial detail.')
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
    if (serialPage.first) return
    await load({ nextPage: Math.max(0, serialPage.page - 1), nextFilters: filters })
  }

  async function goNext() {
    if (serialPage.last) return
    await load({ nextPage: serialPage.page + 1, nextFilters: filters })
  }

  if (loading) {
    return <Spinner label="Loading serial numbers..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Serial tracking</h2>
        <p>Locate individual units across warehouses and bins. Serials are created on inbound for serial-tracked SKUs and move through outbound and return flows.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p>
              {serialPage.totalElements ? `${serialPage.totalElements} serial(s)` : 'No serials matched yet.'} · Page {serialPage.page + 1} of{' '}
              {Math.max(1, serialPage.totalPages || 1)}
            </p>
          </div>
          <div className="inline-actions wrap">
            <button className="btn btn-outline btn-sm" type="button" onClick={goPrev} disabled={serialPage.first}>
              Prev
            </button>
            <button className="btn btn-outline btn-sm" type="button" onClick={goNext} disabled={serialPage.last}>
              Next
            </button>
          </div>
        </div>

        <form className="filters four-up" onSubmit={applyFilters}>
          <label className="grow">
            Search
            <input
              value={filters.query}
              onChange={(event) => setFilters((c) => ({ ...c, query: event.target.value }))}
              placeholder="Serial number, SKU, or product name"
            />
          </label>

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
            <select value={filters.serialStatus} onChange={(event) => setFilters((c) => ({ ...c, serialStatus: event.target.value }))}>
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value || 'all'} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Warehouse
            <select value={filters.warehouseId} onChange={(event) => setFilters((c) => ({ ...c, warehouseId: event.target.value }))}>
              <option value="">All warehouses</option>
              {(warehousesPage.content || []).map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name} ({warehouse.warehouseCode})
                </option>
              ))}
            </select>
          </label>

          <label>
            Bin
            <select value={filters.binId} onChange={(event) => setFilters((c) => ({ ...c, binId: event.target.value }))} disabled={!filters.warehouseId}>
              <option value="">All bins</option>
              {(binsPage.content || []).map((bin) => (
                <option key={bin.id} value={bin.id}>
                  {bin.binCode}
                </option>
              ))}
            </select>
          </label>

          <label>
            Batch id
            <input value={filters.batchId} onChange={(event) => setFilters((c) => ({ ...c, batchId: event.target.value }))} placeholder="Optional batch id" />
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
              <option value="desc">Descending</option>
              <option value="asc">Ascending</option>
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
              <h3>Serial detail</h3>
              <p>{selected ? 'Inspect the current location and movement pointer.' : 'Select a serial to view detail.'}</p>
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
                  <strong className="mono">{detail.serialNumber}</strong>
                  <span className={`badge ${detail.serialStatus === 'IN_STOCK' ? 'badge-success' : 'badge-muted'}`}>{detail.serialStatus}</span>
                </div>
                <p className="subtle-meta">
                  {detail.productName} · <span className="mono">{detail.productSku}</span>
                </p>
                <p className="subtle-meta">Updated {formatDateTime(detail.updatedAt)}</p>
                <p className="subtle-meta mono" title={detail.warehouseId || ''}>
                  Warehouse {detail.warehouseCode || '-'}
                </p>
                <p className="subtle-meta mono" title={detail.binId || ''}>
                  Bin {detail.binCode || '-'}
                </p>
                <p className="subtle-meta mono" title={detail.batchId || ''}>
                  Batch {detail.lotNumber ? `${detail.lotNumber}` : detail.batchId ? shortId(detail.batchId, 18) : '-'}
                </p>
                <p className="subtle-meta mono" title={detail.lastMovementLineId || ''}>
                  Movement line {detail.lastMovementLineId ? shortId(detail.lastMovementLineId, 18) : '-'}
                </p>
              </div>
            </div>
          ) : (
            <p className="empty-copy">No serial selected.</p>
          )}
        </section>

        <section className="panel">
          <div className="section-head compact">
            <div>
              <h3>Serial numbers</h3>
              <p>Click a row to load the serial record.</p>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Updated</th>
                  <th>Serial</th>
                  <th>Status</th>
                  <th>Product</th>
                  <th>Location</th>
                  <th>Lot</th>
                </tr>
              </thead>
              <tbody>
                {serialPage.content.length ? (
                  serialPage.content.map((item) => (
                    <tr
                      key={item.id}
                      className={item.id === selectedId ? 'row-active' : ''}
                      onClick={() => setSelectedId(item.id)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td>{formatDateTime(item.updatedAt)}</td>
                      <td className="mono">{item.serialNumber}</td>
                      <td className="mono">{item.serialStatus}</td>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td className="mono">
                        {item.warehouseCode || '-'}
                        {item.binCode ? ` · ${item.binCode}` : ''}
                      </td>
                      <td className="mono">{item.lotNumber || '-'}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-row" colSpan={6}>
                      No serial numbers found.
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
