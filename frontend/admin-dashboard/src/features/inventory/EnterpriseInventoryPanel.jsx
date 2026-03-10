import { useEffect, useState } from 'react'
import {
  createInventoryTransfer,
  createRestockSchedule,
  listCommerceWarehouses,
  listInventoryReservations,
  listInventoryTransfers,
  listLowStockAlerts,
  listRestockSchedules
} from '../../shared/api/endpoints/inventoryEnterpriseApi'

const DEFAULT_TRANSFER = {
  variantId: '',
  fromWarehouseId: '',
  toWarehouseId: '',
  quantity: '1',
  scheduledFor: '',
  note: ''
}

const DEFAULT_RESTOCK = {
  variantId: '',
  warehouseId: '',
  targetQuantity: '10',
  scheduledFor: '',
  note: ''
}

function toIso(value) {
  const raw = String(value || '').trim()
  if (!raw) return null
  return new Date(raw).toISOString()
}

export function EnterpriseInventoryPanel() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [warehouses, setWarehouses] = useState([])
  const [alerts, setAlerts] = useState([])
  const [reservations, setReservations] = useState([])
  const [transfers, setTransfers] = useState([])
  const [restocks, setRestocks] = useState([])
  const [transferForm, setTransferForm] = useState(DEFAULT_TRANSFER)
  const [restockForm, setRestockForm] = useState(DEFAULT_RESTOCK)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [warehouseData, alertsData, reservationsData, transfersData, restocksData] = await Promise.all([
        listCommerceWarehouses(),
        listLowStockAlerts(),
        listInventoryReservations(),
        listInventoryTransfers(),
        listRestockSchedules()
      ])
      setWarehouses(Array.isArray(warehouseData) ? warehouseData : [])
      setAlerts(Array.isArray(alertsData) ? alertsData : [])
      setReservations(Array.isArray(reservationsData) ? reservationsData : [])
      setTransfers(Array.isArray(transfersData) ? transfersData : [])
      setRestocks(Array.isArray(restocksData) ? restocksData : [])
    } catch (err) {
      setError(err.message || 'Unable to load enterprise inventory data.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleTransferSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await createInventoryTransfer({
        variantId: transferForm.variantId.trim(),
        fromWarehouseId: transferForm.fromWarehouseId,
        toWarehouseId: transferForm.toWarehouseId,
        quantity: Number(transferForm.quantity),
        scheduledFor: toIso(transferForm.scheduledFor),
        note: transferForm.note.trim() || null
      })
      setFlash('Transfer created.')
      setTransferForm(DEFAULT_TRANSFER)
      await load()
    } catch (err) {
      setError(err.message || 'Unable to create transfer.')
    } finally {
      setSaving(false)
    }
  }

  async function handleRestockSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await createRestockSchedule({
        variantId: restockForm.variantId.trim(),
        warehouseId: restockForm.warehouseId,
        targetQuantity: Number(restockForm.targetQuantity),
        scheduledFor: toIso(restockForm.scheduledFor),
        note: restockForm.note.trim() || null
      })
      setFlash('Restock schedule created.')
      setRestockForm(DEFAULT_RESTOCK)
      await load()
    } catch (err) {
      setError(err.message || 'Unable to create restock schedule.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="panel" style={{ marginBottom: 24 }}>
      <div className="section-head">
        <div>
          <h3>Enterprise inventory controls</h3>
          <p>Manage warehouse-to-warehouse transfers, scheduled restocks, and operational alerts on the commerce inventory surface.</p>
        </div>
        <button className="btn btn-outline btn-sm" type="button" onClick={load} disabled={loading}>Refresh</button>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}
      {loading ? <p className="muted-text">Loading enterprise inventory…</p> : null}

      <div className="card-grid">
        <article className="metric-card"><h3>Low stock alerts</h3><strong>{alerts.length}</strong></article>
        <article className="metric-card"><h3>Reservations</h3><strong>{reservations.length}</strong></article>
        <article className="metric-card"><h3>Transfers</h3><strong>{transfers.length}</strong></article>
        <article className="metric-card"><h3>Restock schedules</h3><strong>{restocks.length}</strong></article>
      </div>

      <div className="panel-grid" style={{ marginTop: 20 }}>
        <form className="panel" onSubmit={handleTransferSubmit}>
          <div className="section-head"><div><h4>Create transfer</h4><p>Use the product variant UUID from the commerce catalog.</p></div></div>
          <div className="filters two-up">
            <label>
              Variant UUID
              <input value={transferForm.variantId} onChange={(event) => setTransferForm((current) => ({ ...current, variantId: event.target.value }))} required />
            </label>
            <label>
              Quantity
              <input type="number" min="1" value={transferForm.quantity} onChange={(event) => setTransferForm((current) => ({ ...current, quantity: event.target.value }))} required />
            </label>
            <label>
              From warehouse
              <select value={transferForm.fromWarehouseId} onChange={(event) => setTransferForm((current) => ({ ...current, fromWarehouseId: event.target.value }))} required>
                <option value="">Select</option>
                {warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}
              </select>
            </label>
            <label>
              To warehouse
              <select value={transferForm.toWarehouseId} onChange={(event) => setTransferForm((current) => ({ ...current, toWarehouseId: event.target.value }))} required>
                <option value="">Select</option>
                {warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}
              </select>
            </label>
            <label>
              Schedule for
              <input type="datetime-local" value={transferForm.scheduledFor} onChange={(event) => setTransferForm((current) => ({ ...current, scheduledFor: event.target.value }))} />
            </label>
            <label>
              Note
              <input value={transferForm.note} onChange={(event) => setTransferForm((current) => ({ ...current, note: event.target.value }))} />
            </label>
          </div>
          <div className="inline-actions"><button className="btn btn-primary" type="submit" disabled={saving}>Create transfer</button></div>
        </form>

        <form className="panel" onSubmit={handleRestockSubmit}>
          <div className="section-head"><div><h4>Schedule restock</h4><p>Create a future inbound target for a warehouse and variant.</p></div></div>
          <div className="filters two-up">
            <label>
              Variant UUID
              <input value={restockForm.variantId} onChange={(event) => setRestockForm((current) => ({ ...current, variantId: event.target.value }))} required />
            </label>
            <label>
              Target quantity
              <input type="number" min="1" value={restockForm.targetQuantity} onChange={(event) => setRestockForm((current) => ({ ...current, targetQuantity: event.target.value }))} required />
            </label>
            <label>
              Warehouse
              <select value={restockForm.warehouseId} onChange={(event) => setRestockForm((current) => ({ ...current, warehouseId: event.target.value }))} required>
                <option value="">Select</option>
                {warehouses.map((warehouse) => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}
              </select>
            </label>
            <label>
              Schedule for
              <input type="datetime-local" value={restockForm.scheduledFor} onChange={(event) => setRestockForm((current) => ({ ...current, scheduledFor: event.target.value }))} required />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Note
              <input value={restockForm.note} onChange={(event) => setRestockForm((current) => ({ ...current, note: event.target.value }))} />
            </label>
          </div>
          <div className="inline-actions"><button className="btn btn-primary" type="submit" disabled={saving}>Schedule restock</button></div>
        </form>
      </div>

      <div className="panel-grid" style={{ marginTop: 20 }}>
        <div className="table-card">
          <h4 style={{ marginTop: 0 }}>Low stock</h4>
          <table className="data-table compact">
            <thead><tr><th>Variant</th><th>Warehouse</th><th>Available</th><th>Reorder point</th></tr></thead>
            <tbody>
              {alerts.slice(0, 8).map((item) => (
                <tr key={item.inventoryId}>
                  <td>{item.variantId}</td>
                  <td>{item.warehouseName}</td>
                  <td>{item.availableQuantity}</td>
                  <td>{item.reorderPoint}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="table-card">
          <h4 style={{ marginTop: 0 }}>Recent reservations</h4>
          <table className="data-table compact">
            <thead><tr><th>Variant</th><th>Warehouse</th><th>Qty</th><th>Status</th></tr></thead>
            <tbody>
              {reservations.slice(0, 8).map((item) => (
                <tr key={item.id}>
                  <td>{item.variantId}</td>
                  <td>{item.warehouseName}</td>
                  <td>{item.quantity}</td>
                  <td>{item.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
