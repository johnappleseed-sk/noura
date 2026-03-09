import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listCategories } from '../shared/api/endpoints/inventoryCategoriesApi'
import { listWarehouses } from '../shared/api/endpoints/inventoryLocationsApi'
import { listMovements } from '../shared/api/endpoints/movementsApi'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { getLowStockReport, getStockValuationReport, getTurnoverReport } from '../shared/api/endpoints/reportsApi'
import { listStockLevels } from '../shared/api/endpoints/inventoryApi'
import { getInventorySystemStatus } from '../shared/api/endpoints/systemApi'
import { formatCurrency, formatDateTime, formatDecimal } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

function averageTurnover(items = []) {
  if (!items.length) return 0
  const total = items.reduce((sum, item) => sum + Number(item.turnoverRatio || 0), 0)
  return total / items.length
}

function movementRoute(item) {
  const source = item.sourceWarehouseCode || item.sourceBinCode
  const destination = item.destinationWarehouseCode || item.destinationBinCode
  if (source && destination) {
    return `${source} -> ${destination}`
  }
  return destination || source || item.referenceType || '-'
}

export function DashboardPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [snapshot, setSnapshot] = useState(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [
        productsPage,
        categoriesPage,
        warehousesPage,
        stockLevelsPage,
        lowStock,
        valuation,
        turnover,
        systemStatus,
        movementPage
      ] = await Promise.all([
        listProducts({ page: 0, size: 1 }),
        listCategories({ page: 0, size: 1 }),
        listWarehouses({ page: 0, size: 8, sortBy: 'name', direction: 'asc' }),
        listStockLevels({ page: 0, size: 8, sortBy: 'updatedAt', direction: 'desc' }),
        getLowStockReport(),
        getStockValuationReport(),
        getTurnoverReport(),
        getInventorySystemStatus().catch(() => null),
        listMovements({ page: 0, size: 6, sortBy: 'processedAt', direction: 'desc' })
      ])

      setSnapshot({
        productTotal: productsPage?.totalElements || 0,
        categoryTotal: categoriesPage?.totalElements || 0,
        warehouseTotal: warehousesPage?.totalElements || 0,
        warehouses: warehousesPage?.content || [],
        recentStockLevels: stockLevelsPage?.content || [],
        stockValue: valuation?.totalStockValue || 0,
        valuationItems: valuation?.items || [],
        lowStock: lowStock || [],
        turnover: turnover?.items || [],
        system: systemStatus,
        movements: movementPage?.content || [],
        movementTotal: movementPage?.totalElements || 0
      })
    } catch (err) {
      setError(err.message || 'Unable to load inventory dashboard.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  if (loading) {
    return <Spinner label="Loading inventory dashboard..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Inventory command view</h2>
        <p>Operational summary across catalog, warehouse footprint, stock exposure, and recent movement activity from the inventory service.</p>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card-grid">
        <article className="metric-card">
          <h3>Products</h3>
          <strong>{snapshot?.productTotal || 0}</strong>
          <p>Inventory SKUs currently loaded into the catalog.</p>
        </article>
        <article className="metric-card">
          <h3>Categories</h3>
          <strong>{snapshot?.categoryTotal || 0}</strong>
          <p>Hierarchical category nodes available for assignment.</p>
        </article>
        <article className="metric-card">
          <h3>Warehouses</h3>
          <strong>{snapshot?.warehouseTotal || 0}</strong>
          <p>Active and archived warehouse records across the module.</p>
        </article>
        <article className="metric-card">
          <h3>Stock value</h3>
          <strong>{formatCurrency(snapshot?.stockValue)}</strong>
          <p>Available stock valuation calculated from current quantity and base price.</p>
        </article>
        <article className="metric-card">
          <h3>Low stock alerts</h3>
          <strong>{snapshot?.lowStock?.length || 0}</strong>
          <p>SKUs at or below configured thresholds right now.</p>
        </article>
        <article className="metric-card">
          <h3>30 day turnover</h3>
          <strong>{formatDecimal(averageTurnover(snapshot?.turnover || []), 2)}</strong>
          <p>Average outbound-to-available ratio over the rolling turnover window.</p>
        </article>
      </div>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Quick actions</h3>
              <p>Move directly into the main inventory workflows.</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={load}>
              Refresh
            </button>
          </div>

          <div className="quick-links">
            <Link className="quick-link" to="/admin/commerce/catalog">
              <strong>Commerce catalog</strong>
              <span>Manage product records, variants, media, store pricing, and storefront visibility.</span>
            </Link>
            <Link className="quick-link" to="/admin/orders">
              <strong>Orders</strong>
              <span>Review orders, update status, and inspect timeline events.</span>
            </Link>
            <Link className="quick-link" to="/admin/warehouse/movements">
              <strong>Warehouse movements</strong>
              <span>Run inbound, outbound, transfer, return, and adjustment flows against live stock.</span>
            </Link>
            <Link className="quick-link" to="/admin/tools/control-center">
              <strong>Control center</strong>
              <span>Execute any API endpoint, manage users/approvals, and inspect raw responses.</span>
            </Link>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Warehouse footprint</h3>
              <p>Current warehouse records returned by the inventory API.</p>
            </div>
          </div>

          {snapshot?.warehouses?.length ? (
            <div className="stack-list">
              {snapshot.warehouses.map((warehouse) => (
                <article key={warehouse.id} className="stack-card">
                  <div className="stack-card-top">
                    <strong>{warehouse.name}</strong>
                    <span className={`badge ${warehouse.active ? 'badge-success' : 'badge-muted'}`}>
                      {warehouse.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <p className="subtle-meta">{warehouse.warehouseCode} · {warehouse.warehouseType}</p>
                  <p className="subtle-meta">{warehouse.city || 'No city'} {warehouse.countryCode ? `· ${warehouse.countryCode}` : ''}</p>
                </article>
              ))}
            </div>
          ) : (
            <p className="empty-copy">No warehouse records available yet.</p>
          )}
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>System status</h3>
              <p>Runtime details reported by the inventory service.</p>
            </div>
          </div>

          {snapshot?.system ? (
            <ul className="simple-list">
              <li>
                <strong>Service</strong>: {snapshot.system.service}
              </li>
              <li>
                <strong>Version</strong>: {snapshot.system.foundationVersion}
              </li>
              <li>
                <strong>Profile</strong>: <span className="mono">{snapshot.system.activeProfiles}</span>
              </li>
              <li>
                <strong>Datasource</strong>: <span className="mono">{snapshot.system.datasourceUrl}</span>
              </li>
              <li>
                <strong>Timestamp</strong>: {formatDateTime(snapshot.system.timestamp)}
              </li>
            </ul>
          ) : (
            <p className="empty-copy">System status endpoint is unavailable.</p>
          )}
        </section>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Low stock attention list</h3>
            <p>Products that have crossed configured low-stock thresholds.</p>
          </div>
          <Link className="btn btn-outline btn-sm" to="/admin/warehouse/reports">
            Open reports
          </Link>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Warehouse</th>
                <th>Available</th>
                <th>Threshold</th>
                <th>Reorder point</th>
              </tr>
            </thead>
            <tbody>
              {snapshot?.lowStock?.length ? (
                snapshot.lowStock.slice(0, 6).map((item) => (
                  <tr key={`${item.productId}-${item.warehouseId}`}>
                    <td>
                      <strong>{item.productName}</strong>
                      <div className="subtle-meta mono">{item.productSku}</div>
                    </td>
                    <td className="mono">{item.warehouseCode}</td>
                    <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                    <td>{formatDecimal(item.lowStockThreshold, 2)}</td>
                    <td>{formatDecimal(item.reorderPoint, 2)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="empty-row">No low-stock alerts are open.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Recent movements</h3>
              <p>Latest processed stock transactions across inbound, outbound, transfer, return, and adjustment flows.</p>
            </div>
            <Link className="btn btn-outline btn-sm" to="/admin/warehouse/movements">
              Open movement desk
            </Link>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Movement</th>
                  <th>Route</th>
                  <th>Status</th>
                  <th>Lines</th>
                  <th>Processed</th>
                </tr>
              </thead>
              <tbody>
                {snapshot?.movements?.length ? (
                  snapshot.movements.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.movementType}</strong>
                        <div className="subtle-meta mono">{item.movementNumber}</div>
                      </td>
                      <td>{movementRoute(item)}</td>
                      <td>{item.movementStatus}</td>
                      <td>{item.lines?.length || 0}</td>
                      <td>{formatDateTime(item.processedAt || item.createdAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No movement records are available yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Latest stock updates</h3>
              <p>Most recently touched stock-level records.</p>
            </div>
            <Link className="btn btn-outline btn-sm" to="/admin/warehouse/stock">
              Open stock view
            </Link>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Warehouse</th>
                  <th>Available</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {snapshot?.recentStockLevels?.length ? (
                  snapshot.recentStockLevels.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td>{item.warehouseCode}{item.binCode ? ` / ${item.binCode}` : ''}</td>
                      <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                      <td>{formatDateTime(item.updatedAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="empty-row">No stock updates are available yet.</td>
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
