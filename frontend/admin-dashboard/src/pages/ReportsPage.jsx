import { useEffect, useMemo, useState } from 'react'
import { listBins, listWarehouses } from '../shared/api/endpoints/inventoryLocationsApi'
import {
  exportReportCsv,
  getBarcodeAsset,
  getLowStockReport,
  getMovementHistory,
  getStockValuationReport,
  getTurnoverReport,
  listBatchLots,
  listSerialNumbers
} from '../shared/api/endpoints/reportsApi'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { formatCurrency, formatDateTime, formatDecimal } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const DEFAULT_FILTERS = {
  warehouseId: '',
  productId: '',
  dateFrom: '',
  dateTo: ''
}

function toApiInstant(value) {
  if (!value) return undefined
  return new Date(value).toISOString()
}

export function ReportsPage() {
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState('')
  const [barcodeLoading, setBarcodeLoading] = useState(false)
  const [scanLoading, setScanLoading] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [warehousesPage, setWarehousesPage] = useState({ content: [] })
  const [productsPage, setProductsPage] = useState({ content: [] })
  const [valuationReport, setValuationReport] = useState({ totalStockValue: 0, items: [] })
  const [lowStockItems, setLowStockItems] = useState([])
  const [turnoverReport, setTurnoverReport] = useState({ items: [] })
  const [movementHistory, setMovementHistory] = useState({ content: [] })
  const [batchPage, setBatchPage] = useState({ content: [] })
  const [barcodeForm, setBarcodeForm] = useState({
    resourceType: 'products',
    resourceId: '',
    qr: false,
    width: '360',
    height: '120'
  })
  const [barcodeUrl, setBarcodeUrl] = useState('')
  const [scanMode, setScanMode] = useState('products')
  const [scanQuery, setScanQuery] = useState('')
  const [scanResults, setScanResults] = useState([])

  useEffect(() => () => {
    if (barcodeUrl) {
      URL.revokeObjectURL(barcodeUrl)
    }
  }, [barcodeUrl])

  async function load(nextFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const [warehousesData, productsData, valuationData, lowStockData, turnoverData, movementData, batchData] = await Promise.all([
        listWarehouses({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        getStockValuationReport({
          warehouseId: nextFilters.warehouseId || undefined,
          productId: nextFilters.productId || undefined
        }),
        getLowStockReport({
          warehouseId: nextFilters.warehouseId || undefined
        }),
        getTurnoverReport({
          dateFrom: toApiInstant(nextFilters.dateFrom),
          dateTo: toApiInstant(nextFilters.dateTo)
        }),
        getMovementHistory({
          page: 0,
          size: 12,
          sortBy: 'processedAt',
          direction: 'desc',
          warehouseId: nextFilters.warehouseId || undefined,
          productId: nextFilters.productId || undefined,
          processedFrom: toApiInstant(nextFilters.dateFrom),
          processedTo: toApiInstant(nextFilters.dateTo)
        }),
        nextFilters.productId
          ? listBatchLots({
            page: 0,
            size: 12,
            sortBy: 'expiryDate',
            direction: 'asc',
            productId: nextFilters.productId
          })
          : Promise.resolve({ content: [] })
      ])

      setWarehousesPage(warehousesData || { content: [] })
      setProductsPage(productsData || { content: [] })
      setValuationReport(valuationData || { totalStockValue: 0, items: [] })
      setLowStockItems(lowStockData || [])
      setTurnoverReport(turnoverData || { items: [] })
      setMovementHistory(movementData || { content: [] })
      setBatchPage(batchData || { content: [] })
      setBarcodeForm((current) => ({
        ...current,
        resourceId: current.resourceId || nextFilters.productId || ''
      }))
    } catch (err) {
      setError(err.message || 'Failed to load reports.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const turnoverAverage = useMemo(() => {
    if (!turnoverReport.items?.length) return 0
    return turnoverReport.items.reduce((sum, item) => sum + Number(item.turnoverRatio || 0), 0) / turnoverReport.items.length
  }, [turnoverReport.items])

  async function applyFilters(event) {
    event.preventDefault()
    await load(filters)
  }

  async function handleExport(reportType) {
    setExporting(reportType)
    setFlash('')
    setError('')
    try {
      const blob = await exportReportCsv({
        reportType,
        warehouseId: filters.warehouseId || undefined,
        productId: filters.productId || undefined,
        dateFrom: toApiInstant(filters.dateFrom),
        dateTo: toApiInstant(filters.dateTo)
      })
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `${reportType}.csv`
      anchor.click()
      URL.revokeObjectURL(url)
      setFlash(`${reportType} exported.`)
    } catch (err) {
      setError(err.message || 'Failed to export report.')
    } finally {
      setExporting('')
    }
  }

  async function handleBarcodeGenerate(event) {
    event.preventDefault()
    setBarcodeLoading(true)
    setError('')
    try {
      const blob = await getBarcodeAsset(barcodeForm.resourceType, barcodeForm.resourceId, {
        qr: barcodeForm.qr,
        width: barcodeForm.width,
        height: barcodeForm.height
      })
      if (barcodeUrl) {
        URL.revokeObjectURL(barcodeUrl)
      }
      setBarcodeUrl(URL.createObjectURL(blob))
    } catch (err) {
      setError(err.message || 'Failed to generate barcode.')
    } finally {
      setBarcodeLoading(false)
    }
  }

  async function handleScan(event) {
    event.preventDefault()
    setScanLoading(true)
    setError('')
    try {
      if (scanMode === 'products') {
        const page = await listProducts({
          page: 0,
          size: 6,
          sortBy: 'name',
          direction: 'asc',
          query: scanQuery || undefined
        })
        setScanResults((page?.content || []).map((item) => ({
          id: item.id,
          primary: item.name,
          secondary: item.sku,
          detail: item.primaryCategory?.name || item.status
        })))
      } else if (scanMode === 'serials') {
        const page = await listSerialNumbers({
          page: 0,
          size: 6,
          query: scanQuery || undefined,
          sortBy: 'updatedAt',
          direction: 'desc'
        })
        setScanResults((page?.content || []).map((item) => ({
          id: item.id,
          primary: item.serialNumber,
          secondary: item.productSku,
          detail: `${item.serialStatus} · ${item.warehouseCode || 'No warehouse'}`
        })))
      } else {
        const page = await listBins({
          page: 0,
          size: 6,
          query: scanQuery || undefined,
          sortBy: 'binCode',
          direction: 'asc'
        })
        setScanResults((page?.content || []).map((item) => ({
          id: item.id,
          primary: item.binCode,
          secondary: item.warehouse?.warehouseCode,
          detail: `${item.zoneCode || 'No zone'} · ${item.binType}`
        })))
      }
    } catch (err) {
      setError(err.message || 'Failed to run scan simulation.')
    } finally {
      setScanLoading(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading reports..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Reports and barcodes</h2>
        <p>Review valuation, low-stock exposure, turnover, batch visibility, movement history, and on-demand barcode assets.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p>Apply a shared report context before exporting or drilling into details.</p>
          </div>
        </div>

        <form className="filters four-up" onSubmit={applyFilters}>
          <label>
            Warehouse
            <select value={filters.warehouseId} onChange={(event) => setFilters((current) => ({ ...current, warehouseId: event.target.value }))}>
              <option value="">All warehouses</option>
              {warehousesPage.content.map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Product
            <select value={filters.productId} onChange={(event) => setFilters((current) => ({ ...current, productId: event.target.value }))}>
              <option value="">All products</option>
              {productsPage.content.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} · {product.sku}
                </option>
              ))}
            </select>
          </label>

          <label>
            Date from
            <input type="datetime-local" value={filters.dateFrom} onChange={(event) => setFilters((current) => ({ ...current, dateFrom: event.target.value }))} />
          </label>

          <label>
            Date to
            <input type="datetime-local" value={filters.dateTo} onChange={(event) => setFilters((current) => ({ ...current, dateTo: event.target.value }))} />
          </label>

          <div className="inline-actions">
            <button className="btn btn-primary" type="submit">
              Apply filters
            </button>
            <button
              className="btn btn-outline"
              type="button"
              onClick={() => {
                setFilters(DEFAULT_FILTERS)
                load(DEFAULT_FILTERS)
              }}
            >
              Reset
            </button>
          </div>
        </form>
      </section>

      <div className="card-grid">
        <article className="metric-card">
          <h3>Stock value</h3>
          <strong>{formatCurrency(valuationReport.totalStockValue)}</strong>
          <p>Available stock valuation under the current filter context.</p>
        </article>
        <article className="metric-card">
          <h3>Low stock lines</h3>
          <strong>{lowStockItems.length}</strong>
          <p>Products below threshold in the selected warehouse scope.</p>
        </article>
        <article className="metric-card">
          <h3>Turnover lines</h3>
          <strong>{turnoverReport.items?.length || 0}</strong>
          <p>Outbound product summaries for the requested turnover period.</p>
        </article>
        <article className="metric-card">
          <h3>Average turnover</h3>
          <strong>{formatDecimal(turnoverAverage, 2)}</strong>
          <p>Average turnover ratio across all products returned in the report.</p>
        </article>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>CSV export</h3>
            <p>Export the currently filtered valuation, low-stock, or turnover reports.</p>
          </div>
        </div>
        <div className="inline-actions wrap">
          {['stock-valuation', 'low-stock', 'turnover'].map((reportType) => (
            <button
              key={reportType}
              className="btn btn-outline"
              type="button"
              disabled={exporting === reportType}
              onClick={() => handleExport(reportType)}
            >
              {exporting === reportType ? 'Exporting...' : `Export ${reportType}`}
            </button>
          ))}
        </div>
      </section>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Stock valuation</h3>
              <p>Line-level available stock valuation records.</p>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Warehouse</th>
                  <th>Available</th>
                  <th>Unit price</th>
                  <th>Stock value</th>
                </tr>
              </thead>
              <tbody>
                {valuationReport.items?.length ? (
                  valuationReport.items.slice(0, 10).map((item) => (
                    <tr key={`${item.productId}-${item.warehouseId}-${item.binId || 'none'}`}>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td>{item.warehouseCode}{item.binCode ? ` / ${item.binCode}` : ''}</td>
                      <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                      <td>{formatCurrency(item.unitPrice)}</td>
                      <td>{formatCurrency(item.stockValue)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No valuation rows are available for the current filter set.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Low stock report</h3>
              <p>Current reorder pressure by product and warehouse.</p>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Warehouse</th>
                  <th>Available</th>
                  <th>Threshold</th>
                  <th>Reorder qty</th>
                </tr>
              </thead>
              <tbody>
                {lowStockItems.length ? (
                  lowStockItems.slice(0, 10).map((item) => (
                    <tr key={`${item.productId}-${item.warehouseId}`}>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td>{item.warehouseCode}</td>
                      <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                      <td>{formatDecimal(item.lowStockThreshold, 2)}</td>
                      <td>{formatDecimal(item.reorderQuantity, 2)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No low-stock items are active right now.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Turnover</h3>
              <p>Outbound-to-available ratio by product over the active time window.</p>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Outbound</th>
                  <th>Current available</th>
                  <th>Ratio</th>
                </tr>
              </thead>
              <tbody>
                {turnoverReport.items?.length ? (
                  turnoverReport.items.slice(0, 10).map((item) => (
                    <tr key={item.productId}>
                      <td>
                        <strong>{item.productName}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td>{formatDecimal(item.outboundQuantity, 2)}</td>
                      <td>{formatDecimal(item.currentAvailable, 2)}</td>
                      <td>{formatDecimal(item.turnoverRatio, 2)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="empty-row">No turnover data is available yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Batch visibility</h3>
              <p>{filters.productId ? 'Batch lots for the selected product.' : 'Choose a product to inspect batch lots and expiry order.'}</p>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Lot</th>
                  <th>Expiry</th>
                  <th>On hand</th>
                  <th>Available</th>
                </tr>
              </thead>
              <tbody>
                {batchPage.content?.length ? (
                  batchPage.content.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.lotNumber}</strong>
                        <div className="subtle-meta mono">{item.productSku}</div>
                      </td>
                      <td>{item.expiryDate || '-'}</td>
                      <td>{formatDecimal(item.quantityOnHand, 2)}</td>
                      <td>{formatDecimal(item.quantityAvailable, 2)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="empty-row">No batch lots available for the current selection.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Movement history report</h3>
            <p>Recent transactions under the active report filters.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Movement</th>
                <th>Reference</th>
                <th>Status</th>
                <th>Processed</th>
              </tr>
            </thead>
            <tbody>
              {movementHistory.content?.length ? (
                movementHistory.content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.movementType}</strong>
                      <div className="subtle-meta mono">{item.movementNumber}</div>
                    </td>
                    <td>{item.referenceType || '-'}{item.referenceId ? ` / ${item.referenceId}` : ''}</td>
                    <td>{item.movementStatus}</td>
                    <td>{formatDateTime(item.processedAt || item.createdAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="empty-row">No movement history records are available.</td>
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
              <h3>Barcode asset tool</h3>
              <p>Generate a PNG barcode or QR image for a product, batch, or bin resource id.</p>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleBarcodeGenerate}>
            <div className="filters four-up">
              <label>
                Resource type
                <select value={barcodeForm.resourceType} onChange={(event) => setBarcodeForm((current) => ({ ...current, resourceType: event.target.value }))}>
                  <option value="products">Product</option>
                  <option value="batches">Batch</option>
                  <option value="bins">Bin</option>
                </select>
              </label>

              <label>
                Resource id
                <input value={barcodeForm.resourceId} onChange={(event) => setBarcodeForm((current) => ({ ...current, resourceId: event.target.value }))} placeholder="Paste entity id" required />
              </label>

              <label>
                Width
                <input type="number" min="120" value={barcodeForm.width} onChange={(event) => setBarcodeForm((current) => ({ ...current, width: event.target.value }))} />
              </label>

              <label>
                Height
                <input type="number" min="80" value={barcodeForm.height} onChange={(event) => setBarcodeForm((current) => ({ ...current, height: event.target.value }))} />
              </label>
            </div>

            <label className="checkbox-tile">
              <span>Render as QR</span>
              <input type="checkbox" checked={barcodeForm.qr} onChange={(event) => setBarcodeForm((current) => ({ ...current, qr: event.target.checked }))} />
            </label>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={barcodeLoading}>
                {barcodeLoading ? 'Generating...' : 'Generate barcode'}
              </button>
            </div>
          </form>

          {barcodeUrl ? (
            <div className="barcode-preview">
              <img src={barcodeUrl} alt="Generated barcode preview" />
            </div>
          ) : (
            <p className="empty-copy">Generate a barcode to preview it here.</p>
          )}
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Scan simulation</h3>
              <p>Use a barcode-like string to search products, serial numbers, or bins through the live API.</p>
            </div>
          </div>

          <form className="stack-form" onSubmit={handleScan}>
            <div className="filters two-up">
              <label>
                Scan mode
                <select value={scanMode} onChange={(event) => setScanMode(event.target.value)}>
                  <option value="products">Products</option>
                  <option value="serials">Serials</option>
                  <option value="bins">Bins</option>
                </select>
              </label>

              <label>
                Scanned value
                <input value={scanQuery} onChange={(event) => setScanQuery(event.target.value)} placeholder="Paste barcode, SKU, serial, or bin code" required />
              </label>
            </div>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={scanLoading}>
                {scanLoading ? 'Searching...' : 'Run scan'}
              </button>
            </div>
          </form>

          <div className="selection-list">
            {scanResults.length ? (
              scanResults.map((item) => (
                <article key={item.id} className="stack-card">
                  <strong>{item.primary}</strong>
                  <p className="subtle-meta mono">{item.secondary || '-'}</p>
                  <p className="subtle-meta">{item.detail}</p>
                </article>
              ))
            ) : (
              <p className="empty-copy">No scan results yet.</p>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
