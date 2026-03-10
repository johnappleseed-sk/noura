import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../features/auth/useAuth'
import { listStockLevels } from '../shared/api/endpoints/inventoryApi'
import { listBins, listWarehouses } from '../shared/api/endpoints/inventoryLocationsApi'
import { adjustStock } from '../shared/api/endpoints/movementsApi'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { MANAGER_ROLES, hasAnyRole } from '../shared/auth/roles'
import { formatDateTime, formatDecimal } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'
import { EnterpriseInventoryPanel } from '../features/inventory/EnterpriseInventoryPanel'

const DEFAULT_ADJUSTMENT = {
  warehouseId: '',
  binId: '',
  productId: '',
  quantityDelta: '',
  reasonCode: 'CYCLE_COUNT',
  referenceId: '',
  notes: ''
}

export function InventoryPage() {
  const { auth } = useAuth()
  const canAdjust = hasAnyRole(auth?.roles, MANAGER_ROLES)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [productsPage, setProductsPage] = useState({ content: [] })
  const [warehousesPage, setWarehousesPage] = useState({ content: [] })
  const [binsPage, setBinsPage] = useState({ content: [] })
  const [stockPage, setStockPage] = useState({ content: [], totalElements: 0 })
  const [filters, setFilters] = useState({
    productId: '',
    warehouseId: '',
    binId: '',
    lowStockOnly: false
  })
  const [adjustment, setAdjustment] = useState(DEFAULT_ADJUSTMENT)

  async function load(selectedFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const [productsData, warehousesData, binsData, stockData] = await Promise.all([
        listProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listWarehouses({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listBins({
          page: 0,
          size: 100,
          sortBy: 'binCode',
          direction: 'asc'
        }),
        listStockLevels({
          page: 0,
          size: 100,
          sortBy: 'updatedAt',
          direction: 'desc',
          productId: selectedFilters.productId || undefined,
          warehouseId: selectedFilters.warehouseId || undefined,
          binId: selectedFilters.binId || undefined,
          lowStockOnly: selectedFilters.lowStockOnly || undefined
        })
      ])

      setProductsPage(productsData || { content: [] })
      setWarehousesPage(warehousesData || { content: [] })
      setBinsPage(binsData || { content: [] })
      setStockPage(stockData || { content: [] })
      setAdjustment((current) => ({
        ...current,
        warehouseId: current.warehouseId || selectedFilters.warehouseId || warehousesData?.content?.[0]?.id || '',
        binId: current.binId && binsData?.content?.some((item) => item.id === current.binId) ? current.binId : '',
        productId: current.productId || selectedFilters.productId || ''
      }))
    } catch (err) {
      setError(err.message || 'Failed to load stock levels.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  useEffect(() => {
    if (!filters.warehouseId) {
      return
    }
    if (!binsPage.content.some((item) => item.id === filters.binId && item.warehouse?.id === filters.warehouseId)) {
      setFilters((current) => ({ ...current, binId: '' }))
    }
  }, [binsPage.content, filters.binId, filters.warehouseId])

  useEffect(() => {
    if (!adjustment.warehouseId) {
      return
    }
    if (!binsPage.content.some((item) => item.id === adjustment.binId && item.warehouse?.id === adjustment.warehouseId)) {
      setAdjustment((current) => ({ ...current, binId: '' }))
    }
  }, [adjustment.binId, adjustment.warehouseId, binsPage.content])

  const totalAvailable = useMemo(
    () => stockPage.content.reduce((sum, item) => sum + Number(item.quantityAvailable || 0), 0),
    [stockPage.content]
  )

  const lowStockCount = useMemo(
    () => stockPage.content.filter((item) => item.lowStock).length,
    [stockPage.content]
  )

  const filteredStockBins = useMemo(
    () => binsPage.content.filter((bin) => !filters.warehouseId || bin.warehouse?.id === filters.warehouseId),
    [binsPage.content, filters.warehouseId]
  )

  const filteredAdjustmentBins = useMemo(
    () => binsPage.content.filter((bin) => !adjustment.warehouseId || bin.warehouse?.id === adjustment.warehouseId),
    [adjustment.warehouseId, binsPage.content]
  )

  async function handleFilterSubmit(event) {
    event.preventDefault()
    await load(filters)
  }

  async function handleAdjustmentSubmit(event) {
    event.preventDefault()
    if (!canAdjust) {
      return
    }
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await adjustStock({
        warehouseId: adjustment.warehouseId,
        binId: adjustment.binId || null,
        reasonCode: adjustment.reasonCode || null,
        referenceId: adjustment.referenceId || null,
        externalReference: null,
        notes: adjustment.notes || null,
        lines: [
          {
            productId: adjustment.productId,
            quantityDelta: Number(adjustment.quantityDelta),
            binId: adjustment.binId || null,
            batchId: null,
            lotNumber: null,
            expiryDate: null,
            manufacturedAt: null,
            supplierBatchRef: null,
            unitCost: null,
            notes: adjustment.notes || null,
            serialNumbers: []
          }
        ]
      })
      setFlash('Stock adjustment posted successfully.')
      setAdjustment((current) => ({
        ...DEFAULT_ADJUSTMENT,
        warehouseId: current.warehouseId,
        productId: current.productId
      }))
      await load(filters)
    } catch (err) {
      setError(err.message || 'Failed to apply stock adjustment.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading stock levels..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Stock levels</h2>
        <p>Inspect live product stock by warehouse, bin, and batch. Managers can also post focused adjustment entries without leaving the dashboard.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <EnterpriseInventoryPanel />

      <div className="card-grid">
        <article className="metric-card">
          <h3>Visible stock rows</h3>
          <strong>{stockPage.totalElements || stockPage.content.length}</strong>
          <p>Filtered stock-level records in the current view.</p>
        </article>
        <article className="metric-card">
          <h3>Available quantity</h3>
          <strong>{formatDecimal(totalAvailable, 2)}</strong>
          <p>Total available units across the current filter set.</p>
        </article>
        <article className="metric-card">
          <h3>Low stock rows</h3>
          <strong>{lowStockCount}</strong>
          <p>Rows flagged by active low-stock thresholds.</p>
        </article>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Stock filters</h3>
            <p>Narrow the current stock grid by product, warehouse, bin, or low-stock state.</p>
          </div>
        </div>

        <form className="filters four-up" onSubmit={handleFilterSubmit}>
          <label>
            Product
            <select
              value={filters.productId}
              onChange={(event) => setFilters((current) => ({ ...current, productId: event.target.value }))}
            >
              <option value="">All products</option>
              {productsPage.content.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} · {product.sku}
                </option>
              ))}
            </select>
          </label>

          <label>
            Warehouse
            <select
              value={filters.warehouseId}
              onChange={(event) => setFilters((current) => ({ ...current, warehouseId: event.target.value, binId: '' }))}
            >
              <option value="">All warehouses</option>
              {warehousesPage.content.map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Bin
            <select
              value={filters.binId}
              onChange={(event) => setFilters((current) => ({ ...current, binId: event.target.value }))}
              disabled={!filters.warehouseId}
            >
              <option value="">{filters.warehouseId ? 'All bins' : 'Choose warehouse first'}</option>
              {filteredStockBins.map((bin) => (
                <option key={bin.id} value={bin.id}>
                  {bin.binCode}
                </option>
              ))}
            </select>
          </label>

          <label className="checkbox-tile">
            <span>Low stock only</span>
            <input
              type="checkbox"
              checked={filters.lowStockOnly}
              onChange={(event) => setFilters((current) => ({ ...current, lowStockOnly: event.target.checked }))}
            />
          </label>

          <div className="inline-actions">
            <button className="btn btn-primary" type="submit">
              Apply filters
            </button>
            <button
              className="btn btn-outline"
              type="button"
              onClick={() => {
                const nextFilters = { productId: '', warehouseId: '', binId: '', lowStockOnly: false }
                setFilters(nextFilters)
                load(nextFilters)
              }}
            >
              Reset
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Quick adjustment</h3>
            <p>Post a manual stock delta with a reason code. This calls the inventory adjustment endpoint directly.</p>
          </div>
        </div>

        {!canAdjust ? <div className="alert alert-error">Your role is read-only in this screen. Use an admin or warehouse-manager account to post adjustments.</div> : null}

        <form className="filters four-up" onSubmit={handleAdjustmentSubmit}>
          <label>
            Warehouse
            <select
              value={adjustment.warehouseId}
              onChange={(event) => setAdjustment((current) => ({ ...current, warehouseId: event.target.value, binId: '' }))}
              required
            >
              <option value="">Select warehouse</option>
              {warehousesPage.content.map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Bin
            <select
              value={adjustment.binId}
              onChange={(event) => setAdjustment((current) => ({ ...current, binId: event.target.value }))}
            >
              <option value="">No specific bin</option>
              {filteredAdjustmentBins.map((bin) => (
                <option key={bin.id} value={bin.id}>
                  {bin.binCode}
                </option>
              ))}
            </select>
          </label>

          <label>
            Product
            <select
              value={adjustment.productId}
              onChange={(event) => setAdjustment((current) => ({ ...current, productId: event.target.value }))}
              required
            >
              <option value="">Select product</option>
              {productsPage.content.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} · {product.sku}
                </option>
              ))}
            </select>
          </label>

          <label>
            Quantity delta
            <input
              type="number"
              step="0.01"
              value={adjustment.quantityDelta}
              onChange={(event) => setAdjustment((current) => ({ ...current, quantityDelta: event.target.value }))}
              placeholder="-4 or 12"
              required
            />
          </label>

          <label>
            Reason code
            <input
              value={adjustment.reasonCode}
              onChange={(event) => setAdjustment((current) => ({ ...current, reasonCode: event.target.value }))}
              placeholder="CYCLE_COUNT"
            />
          </label>

          <label>
            Reference id
            <input
              value={adjustment.referenceId}
              onChange={(event) => setAdjustment((current) => ({ ...current, referenceId: event.target.value }))}
              placeholder="COUNT-2026-03-08"
            />
          </label>

          <label className="grow">
            Notes
            <input
              value={adjustment.notes}
              onChange={(event) => setAdjustment((current) => ({ ...current, notes: event.target.value }))}
              placeholder="Manual recount, damage write-off, shelf correction"
            />
          </label>

          <div className="inline-actions">
            <button className="btn btn-primary" type="submit" disabled={saving || !canAdjust}>
              {saving ? 'Posting...' : 'Post adjustment'}
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Stock-level table</h3>
            <p>Live warehouse and bin availability from the stock-level endpoint.</p>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Warehouse / bin</th>
                <th>Lot</th>
                <th>On hand</th>
                <th>Reserved</th>
                <th>Available</th>
                <th>Low stock</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {stockPage.content.length ? (
                stockPage.content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.productName}</strong>
                      <div className="subtle-meta mono">{item.productSku}</div>
                    </td>
                    <td>
                      <strong>{item.warehouseName}</strong>
                      <div className="subtle-meta mono">{item.warehouseCode}{item.binCode ? ` / ${item.binCode}` : ''}</div>
                    </td>
                    <td>{item.lotNumber || '-'}</td>
                    <td>{formatDecimal(item.quantityOnHand, 2)}</td>
                    <td>{formatDecimal(item.quantityReserved, 2)}</td>
                    <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                    <td>
                      <span className={`badge ${item.lowStock ? 'badge-warning' : 'badge-success'}`}>
                        {item.lowStock ? `Below ${formatDecimal(item.lowStockThreshold, 2)}` : 'Healthy'}
                      </span>
                    </td>
                    <td>{formatDateTime(item.updatedAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8" className="empty-row">No stock levels match the current filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
