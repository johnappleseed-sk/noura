import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../features/auth/useAuth'
import { listBins, listWarehouses } from '../shared/api/endpoints/inventoryLocationsApi'
import {
  adjustStock,
  listMovements,
  receiveInbound,
  returnStock,
  shipOutbound,
  transferStock
} from '../shared/api/endpoints/movementsApi'
import { listProducts } from '../shared/api/endpoints/inventoryProductsApi'
import { MANAGER_ROLES, hasAnyRole } from '../shared/auth/roles'
import { formatDateTime, formatDecimal } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const MOVEMENT_TYPES = ['INBOUND', 'OUTBOUND', 'TRANSFER', 'ADJUSTMENT', 'RETURN']

function createLine() {
  return {
    productId: '',
    quantity: '',
    quantityDelta: '',
    unitCost: '',
    lotNumber: '',
    expiryDate: '',
    notes: '',
    serialNumbers: ''
  }
}

const DEFAULT_FORM = {
  warehouseId: '',
  binId: '',
  sourceWarehouseId: '',
  sourceBinId: '',
  destinationWarehouseId: '',
  destinationBinId: '',
  reasonCode: 'CYCLE_COUNT',
  referenceType: '',
  referenceId: '',
  externalReference: '',
  notes: '',
  lines: [createLine()]
}

function parseSerialNumbers(value) {
  return value
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function MovementsPage() {
  const { auth } = useAuth()
  const canManage = hasAnyRole(auth?.roles, MANAGER_ROLES)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [movementType, setMovementType] = useState('INBOUND')
  const [form, setForm] = useState(DEFAULT_FORM)
  const [productsPage, setProductsPage] = useState({ content: [] })
  const [warehousesPage, setWarehousesPage] = useState({ content: [] })
  const [binsPage, setBinsPage] = useState({ content: [] })
  const [movementPage, setMovementPage] = useState({ content: [], totalElements: 0 })

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [productsData, warehousesData, binsData, movementsData] = await Promise.all([
        listProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listWarehouses({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        listBins({ page: 0, size: 100, sortBy: 'binCode', direction: 'asc' }),
        listMovements({ page: 0, size: 12, sortBy: 'processedAt', direction: 'desc' })
      ])
      setProductsPage(productsData || { content: [] })
      setWarehousesPage(warehousesData || { content: [] })
      setBinsPage(binsData || { content: [] })
      setMovementPage(movementsData || { content: [], totalElements: 0 })
      setForm((current) => ({
        ...current,
        warehouseId: current.warehouseId || warehousesData?.content?.[0]?.id || '',
        sourceWarehouseId: current.sourceWarehouseId || warehousesData?.content?.[0]?.id || '',
        destinationWarehouseId: current.destinationWarehouseId || warehousesData?.content?.[1]?.id || warehousesData?.content?.[0]?.id || ''
      }))
    } catch (err) {
      setError(err.message || 'Failed to load movement workspace.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredSourceBins = useMemo(
    () => binsPage.content.filter((bin) => !form.sourceWarehouseId || bin.warehouse?.id === form.sourceWarehouseId),
    [binsPage.content, form.sourceWarehouseId]
  )

  const filteredDestinationBins = useMemo(
    () => binsPage.content.filter((bin) => !form.destinationWarehouseId || bin.warehouse?.id === form.destinationWarehouseId),
    [binsPage.content, form.destinationWarehouseId]
  )

  const filteredWarehouseBins = useMemo(
    () => binsPage.content.filter((bin) => !form.warehouseId || bin.warehouse?.id === form.warehouseId),
    [binsPage.content, form.warehouseId]
  )

  function updateLine(index, field, value) {
    setForm((current) => ({
      ...current,
      lines: current.lines.map((line, lineIndex) => (lineIndex === index ? { ...line, [field]: value } : line))
    }))
  }

  function addLine() {
    setForm((current) => ({
      ...current,
      lines: [...current.lines, createLine()]
    }))
  }

  function removeLine(index) {
    setForm((current) => ({
      ...current,
      lines: current.lines.length === 1 ? current.lines : current.lines.filter((_, lineIndex) => lineIndex !== index)
    }))
  }

  function resetForm() {
    setForm((current) => ({
      ...DEFAULT_FORM,
      warehouseId: current.warehouseId,
      sourceWarehouseId: current.sourceWarehouseId,
      destinationWarehouseId: current.destinationWarehouseId
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canManage) return
    setSubmitting(true)
    setError('')
    setFlash('')
    try {
      const sharedLines = form.lines.map((line) => ({
        productId: line.productId,
        quantity: Number(line.quantity),
        fromBinId: null,
        toBinId: null,
        batchId: null,
        lotNumber: line.lotNumber || null,
        expiryDate: line.expiryDate || null,
        manufacturedAt: null,
        supplierBatchRef: null,
        unitCost: line.unitCost === '' ? null : Number(line.unitCost),
        notes: line.notes || null,
        serialNumbers: parseSerialNumbers(line.serialNumbers)
      }))

      const adjustmentLines = form.lines.map((line) => ({
        productId: line.productId,
        quantityDelta: Number(line.quantityDelta),
        binId: form.binId || null,
        batchId: null,
        lotNumber: line.lotNumber || null,
        expiryDate: line.expiryDate || null,
        manufacturedAt: null,
        supplierBatchRef: null,
        unitCost: line.unitCost === '' ? null : Number(line.unitCost),
        notes: line.notes || null,
        serialNumbers: parseSerialNumbers(line.serialNumbers)
      }))

      if (movementType === 'INBOUND') {
        await receiveInbound({
          warehouseId: form.warehouseId,
          binId: form.binId || null,
          referenceType: form.referenceType || null,
          referenceId: form.referenceId || null,
          externalReference: form.externalReference || null,
          notes: form.notes || null,
          lines: sharedLines
        })
      } else if (movementType === 'OUTBOUND') {
        await shipOutbound({
          warehouseId: form.warehouseId,
          binId: form.binId || null,
          referenceType: form.referenceType || null,
          referenceId: form.referenceId || null,
          externalReference: form.externalReference || null,
          notes: form.notes || null,
          lines: sharedLines
        })
      } else if (movementType === 'RETURN') {
        await returnStock({
          warehouseId: form.warehouseId,
          binId: form.binId || null,
          referenceType: form.referenceType || null,
          referenceId: form.referenceId || null,
          externalReference: form.externalReference || null,
          notes: form.notes || null,
          lines: sharedLines
        })
      } else if (movementType === 'TRANSFER') {
        await transferStock({
          sourceWarehouseId: form.sourceWarehouseId,
          sourceBinId: form.sourceBinId || null,
          destinationWarehouseId: form.destinationWarehouseId,
          destinationBinId: form.destinationBinId || null,
          referenceType: form.referenceType || null,
          referenceId: form.referenceId || null,
          externalReference: form.externalReference || null,
          notes: form.notes || null,
          lines: sharedLines
        })
      } else {
        await adjustStock({
          warehouseId: form.warehouseId,
          binId: form.binId || null,
          reasonCode: form.reasonCode || null,
          referenceId: form.referenceId || null,
          externalReference: form.externalReference || null,
          notes: form.notes || null,
          lines: adjustmentLines
        })
      }

      setFlash(`${movementType.toLowerCase()} flow submitted.`)
      resetForm()
      await load()
    } catch (err) {
      setError(err.message || 'Failed to submit movement.')
    } finally {
      setSubmitting(false)
    }
  }

  function routeSummary(item) {
    const source = item.sourceWarehouseCode || item.sourceBinCode
    const destination = item.destinationWarehouseCode || item.destinationBinCode
    if (source && destination) {
      return `${source} -> ${destination}`
    }
    return destination || source || '-'
  }

  if (loading) {
    return <Spinner label="Loading movement desk..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Movement desk</h2>
        <p>Execute inbound, outbound, transfer, adjustment, and return transactions against the live inventory engine.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}
      {!canManage ? <div className="alert alert-error">Your role is read-only in this workspace. Managers can view history here, but only admins and warehouse managers can submit movements.</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Movement form</h3>
            <p>Select a flow type, complete the top-level context, then add one or more movement lines.</p>
          </div>
        </div>

        <div className="toggle-group">
          {MOVEMENT_TYPES.map((item) => (
            <button
              key={item}
              type="button"
              className={`toggle-chip ${movementType === item ? 'active' : ''}`}
              onClick={() => setMovementType(item)}
            >
              {item}
            </button>
          ))}
        </div>

        <form className="stack-form" onSubmit={handleSubmit}>
          {movementType === 'TRANSFER' ? (
            <div className="filters four-up">
              <label>
                Source warehouse
                <select value={form.sourceWarehouseId} onChange={(event) => setForm((current) => ({ ...current, sourceWarehouseId: event.target.value, sourceBinId: '' }))} required>
                  <option value="">Select source warehouse</option>
                  {warehousesPage.content.map((warehouse) => (
                    <option key={warehouse.id} value={warehouse.id}>
                      {warehouse.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Source bin
                <select value={form.sourceBinId} onChange={(event) => setForm((current) => ({ ...current, sourceBinId: event.target.value }))}>
                  <option value="">No specific source bin</option>
                  {filteredSourceBins.map((bin) => (
                    <option key={bin.id} value={bin.id}>
                      {bin.binCode}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Destination warehouse
                <select value={form.destinationWarehouseId} onChange={(event) => setForm((current) => ({ ...current, destinationWarehouseId: event.target.value, destinationBinId: '' }))} required>
                  <option value="">Select destination warehouse</option>
                  {warehousesPage.content.map((warehouse) => (
                    <option key={warehouse.id} value={warehouse.id}>
                      {warehouse.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Destination bin
                <select value={form.destinationBinId} onChange={(event) => setForm((current) => ({ ...current, destinationBinId: event.target.value }))}>
                  <option value="">No specific destination bin</option>
                  {filteredDestinationBins.map((bin) => (
                    <option key={bin.id} value={bin.id}>
                      {bin.binCode}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          ) : (
            <div className="filters four-up">
              <label>
                Warehouse
                <select value={form.warehouseId} onChange={(event) => setForm((current) => ({ ...current, warehouseId: event.target.value, binId: '' }))} required>
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
                <select value={form.binId} onChange={(event) => setForm((current) => ({ ...current, binId: event.target.value }))}>
                  <option value="">No specific bin</option>
                  {filteredWarehouseBins.map((bin) => (
                    <option key={bin.id} value={bin.id}>
                      {bin.binCode}
                    </option>
                  ))}
                </select>
              </label>

              {movementType === 'ADJUSTMENT' ? (
                <label>
                  Reason code
                  <input value={form.reasonCode} onChange={(event) => setForm((current) => ({ ...current, reasonCode: event.target.value }))} placeholder="CYCLE_COUNT" />
                </label>
              ) : (
                <label>
                  Reference type
                  <input value={form.referenceType} onChange={(event) => setForm((current) => ({ ...current, referenceType: event.target.value }))} placeholder="PO, SO, RMA" />
                </label>
              )}

              <label>
                Reference id
                <input value={form.referenceId} onChange={(event) => setForm((current) => ({ ...current, referenceId: event.target.value }))} placeholder="Optional reference" />
              </label>
            </div>
          )}

          <div className="filters two-up">
            <label>
              External reference
              <input value={form.externalReference} onChange={(event) => setForm((current) => ({ ...current, externalReference: event.target.value }))} placeholder="Carrier, supplier, or ERP reference" />
            </label>

            <label>
              Notes
              <input value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} placeholder="Optional operator note" />
            </label>
          </div>

          <div className="section-head compact">
            <div>
              <h3>Movement lines</h3>
              <p>Add the product lines to transact in this movement.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={addLine}>
              Add line
            </button>
          </div>

          <div className="stack-list">
            {form.lines.map((line, index) => (
              <article className="stack-card" key={`line-${index}`}>
                <div className="section-head compact">
                  <div>
                    <h3>Line {index + 1}</h3>
                    <p>Product and quantity payload for the selected movement type.</p>
                  </div>
                  {form.lines.length > 1 ? (
                    <button className="btn btn-outline btn-sm" type="button" onClick={() => removeLine(index)}>
                      Remove
                    </button>
                  ) : null}
                </div>

                <div className="filters four-up">
                  <label>
                    Product
                    <select value={line.productId} onChange={(event) => updateLine(index, 'productId', event.target.value)} required>
                      <option value="">Select product</option>
                      {productsPage.content.map((product) => (
                        <option key={product.id} value={product.id}>
                          {product.name} · {product.sku}
                        </option>
                      ))}
                    </select>
                  </label>

                  {movementType === 'ADJUSTMENT' ? (
                    <label>
                      Quantity delta
                      <input type="number" step="0.01" value={line.quantityDelta} onChange={(event) => updateLine(index, 'quantityDelta', event.target.value)} placeholder="-2 or 8" required />
                    </label>
                  ) : (
                    <label>
                      Quantity
                      <input type="number" min="0.01" step="0.01" value={line.quantity} onChange={(event) => updateLine(index, 'quantity', event.target.value)} placeholder="10" required />
                    </label>
                  )}

                  <label>
                    Unit cost
                    <input type="number" min="0" step="0.01" value={line.unitCost} onChange={(event) => updateLine(index, 'unitCost', event.target.value)} placeholder="Optional" />
                  </label>

                  <label>
                    Lot number
                    <input value={line.lotNumber} onChange={(event) => updateLine(index, 'lotNumber', event.target.value)} placeholder="Optional lot" />
                  </label>
                </div>

                <div className="filters two-up">
                  <label>
                    Expiry date
                    <input type="date" value={line.expiryDate} onChange={(event) => updateLine(index, 'expiryDate', event.target.value)} />
                  </label>

                  <label>
                    Serial numbers
                    <textarea
                      rows="3"
                      value={line.serialNumbers}
                      onChange={(event) => updateLine(index, 'serialNumbers', event.target.value)}
                      placeholder="Optional serials, comma or line separated"
                    />
                  </label>
                </div>

                <label>
                  Line notes
                  <input value={line.notes} onChange={(event) => updateLine(index, 'notes', event.target.value)} placeholder="Optional line note" />
                </label>
              </article>
            ))}
          </div>

          <div className="inline-actions">
            <button className="btn btn-primary" type="submit" disabled={!canManage || submitting}>
              {submitting ? 'Submitting...' : `Submit ${movementType.toLowerCase()}`}
            </button>
            <button className="btn btn-outline" type="button" onClick={resetForm}>
              Reset form
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Recent movement history</h3>
            <p>{movementPage.totalElements || movementPage.content.length} movement records are visible through the current API.</p>
          </div>
          <button className="btn btn-outline btn-sm" type="button" onClick={load}>
            Refresh
          </button>
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
              {movementPage.content.length ? (
                movementPage.content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.movementType}</strong>
                      <div className="subtle-meta mono">{item.movementNumber}</div>
                    </td>
                    <td>{routeSummary(item)}</td>
                    <td>{item.movementStatus}</td>
                    <td>{formatDecimal(item.lines?.length || 0, 0)}</td>
                    <td>{formatDateTime(item.processedAt || item.createdAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="empty-row">No movement history is available yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
