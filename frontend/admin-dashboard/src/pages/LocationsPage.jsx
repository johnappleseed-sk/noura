import { useEffect, useState } from 'react'
import { useAuth } from '../features/auth/useAuth'
import {
  createWarehouse,
  createWarehouseBin,
  deleteWarehouse,
  deleteWarehouseBin,
  listWarehouseBins,
  listWarehouses,
  updateWarehouse,
  updateWarehouseBin
} from '../shared/api/endpoints/inventoryLocationsApi'
import { MANAGER_ROLES, hasAnyRole } from '../shared/auth/roles'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const DEFAULT_WAREHOUSE_FORM = {
  warehouseCode: '',
  name: '',
  warehouseType: 'FULFILLMENT',
  addressLine1: '',
  addressLine2: '',
  city: '',
  stateProvince: '',
  postalCode: '',
  countryCode: 'US',
  active: true
}

const DEFAULT_BIN_FORM = {
  binCode: '',
  zoneCode: '',
  aisleCode: '',
  shelfCode: '',
  binType: 'STANDARD',
  barcodeValue: '',
  qrCodeValue: '',
  pickSequence: '0',
  active: true
}

export function LocationsPage() {
  const { auth } = useAuth()
  const canManage = hasAnyRole(auth?.roles, MANAGER_ROLES)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [warehousesPage, setWarehousesPage] = useState({ content: [], totalElements: 0 })
  const [binsPage, setBinsPage] = useState({ content: [], totalElements: 0 })
  const [selectedWarehouseId, setSelectedWarehouseId] = useState('')
  const [selectedBinId, setSelectedBinId] = useState('')
  const [warehouseForm, setWarehouseForm] = useState(DEFAULT_WAREHOUSE_FORM)
  const [binForm, setBinForm] = useState(DEFAULT_BIN_FORM)
  const [warehouseSaving, setWarehouseSaving] = useState(false)
  const [binSaving, setBinSaving] = useState(false)

  const selectedWarehouse = warehousesPage.content.find((item) => item.id === selectedWarehouseId) || null

  async function load(nextWarehouseId = selectedWarehouseId) {
    setLoading(true)
    setError('')
    try {
      const warehousesData = await listWarehouses({
        page: 0,
        size: 100,
        sortBy: 'name',
        direction: 'asc'
      })
      const resolvedWarehouseId = nextWarehouseId || warehousesData?.content?.[0]?.id || ''
      const binsData = resolvedWarehouseId
        ? await listWarehouseBins(resolvedWarehouseId, {
          page: 0,
          size: 100,
          sortBy: 'binCode',
          direction: 'asc'
        })
        : { content: [], totalElements: 0 }

      setWarehousesPage(warehousesData || { content: [], totalElements: 0 })
      setBinsPage(binsData || { content: [], totalElements: 0 })
      setSelectedWarehouseId(resolvedWarehouseId)
      setSelectedBinId('')
    } catch (err) {
      setError(err.message || 'Failed to load location workspace.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function handleWarehouseSelect(warehouse) {
    setSelectedWarehouseId(warehouse.id)
    setWarehouseForm({
      warehouseCode: warehouse.warehouseCode || '',
      name: warehouse.name || '',
      warehouseType: warehouse.warehouseType || 'FULFILLMENT',
      addressLine1: warehouse.addressLine1 || '',
      addressLine2: warehouse.addressLine2 || '',
      city: warehouse.city || '',
      stateProvince: warehouse.stateProvince || '',
      postalCode: warehouse.postalCode || '',
      countryCode: warehouse.countryCode || '',
      active: Boolean(warehouse.active)
    })
    load(warehouse.id)
  }

  function handleBinSelect(bin) {
    setSelectedBinId(bin.id)
    setBinForm({
      binCode: bin.binCode || '',
      zoneCode: bin.zoneCode || '',
      aisleCode: bin.aisleCode || '',
      shelfCode: bin.shelfCode || '',
      binType: bin.binType || 'STANDARD',
      barcodeValue: bin.barcodeValue || '',
      qrCodeValue: bin.qrCodeValue || '',
      pickSequence: String(bin.pickSequence ?? 0),
      active: Boolean(bin.active)
    })
  }

  async function saveWarehouse(event) {
    event.preventDefault()
    if (!canManage) return
    setWarehouseSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        warehouseCode: warehouseForm.warehouseCode.trim(),
        name: warehouseForm.name.trim(),
        warehouseType: warehouseForm.warehouseType.trim(),
        addressLine1: warehouseForm.addressLine1 || null,
        addressLine2: warehouseForm.addressLine2 || null,
        city: warehouseForm.city || null,
        stateProvince: warehouseForm.stateProvince || null,
        postalCode: warehouseForm.postalCode || null,
        countryCode: warehouseForm.countryCode || null,
        active: warehouseForm.active
      }
      const saved = selectedWarehouseId
        ? await updateWarehouse(selectedWarehouseId, payload)
        : await createWarehouse(payload)
      setFlash(selectedWarehouseId ? 'Warehouse updated.' : 'Warehouse created.')
      setWarehouseForm(DEFAULT_WAREHOUSE_FORM)
      await load(saved.id)
    } catch (err) {
      setError(err.message || 'Failed to save warehouse.')
    } finally {
      setWarehouseSaving(false)
    }
  }

  async function removeWarehouse() {
    if (!canManage || !selectedWarehouseId) return
    if (!window.confirm('Delete this warehouse?')) return
    setWarehouseSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteWarehouse(selectedWarehouseId)
      setFlash('Warehouse deleted.')
      setSelectedWarehouseId('')
      setWarehouseForm(DEFAULT_WAREHOUSE_FORM)
      await load('')
    } catch (err) {
      setError(err.message || 'Failed to delete warehouse.')
    } finally {
      setWarehouseSaving(false)
    }
  }

  async function saveBin(event) {
    event.preventDefault()
    if (!canManage || !selectedWarehouseId) return
    setBinSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        binCode: binForm.binCode.trim(),
        zoneCode: binForm.zoneCode || null,
        aisleCode: binForm.aisleCode || null,
        shelfCode: binForm.shelfCode || null,
        binType: binForm.binType.trim(),
        barcodeValue: binForm.barcodeValue || null,
        qrCodeValue: binForm.qrCodeValue || null,
        pickSequence: Number(binForm.pickSequence || 0),
        active: binForm.active
      }
      await (selectedBinId
        ? updateWarehouseBin(selectedBinId, payload)
        : createWarehouseBin(selectedWarehouseId, payload))
      setFlash(selectedBinId ? 'Bin updated.' : 'Bin created.')
      setSelectedBinId('')
      setBinForm(DEFAULT_BIN_FORM)
      await load(selectedWarehouseId)
    } catch (err) {
      setError(err.message || 'Failed to save bin.')
    } finally {
      setBinSaving(false)
    }
  }

  async function removeBin() {
    if (!canManage || !selectedBinId) return
    if (!window.confirm('Delete this bin?')) return
    setBinSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteWarehouseBin(selectedBinId)
      setFlash('Bin deleted.')
      setSelectedBinId('')
      setBinForm(DEFAULT_BIN_FORM)
      await load(selectedWarehouseId)
    } catch (err) {
      setError(err.message || 'Failed to delete bin.')
    } finally {
      setBinSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading locations..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Location workspace</h2>
        <p>Manage warehouses, their physical metadata, and the bins used for picking, storage, and transfers.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}
      {!canManage ? <div className="alert alert-error">Your role is read-only in this workspace. Use an admin or warehouse-manager account to manage warehouses and bins.</div> : null}

      <div className="workspace-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Warehouses</h3>
              <p>{warehousesPage.totalElements || warehousesPage.content.length} warehouse records in the active environment.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={() => {
              setSelectedWarehouseId('')
              setWarehouseForm(DEFAULT_WAREHOUSE_FORM)
            }}>
              New warehouse
            </button>
          </div>

          <div className="selection-list">
            {warehousesPage.content.length ? (
              warehousesPage.content.map((warehouse) => (
                <button
                  key={warehouse.id}
                  type="button"
                  className={`selection-item ${selectedWarehouseId === warehouse.id ? 'active' : ''}`}
                  onClick={() => handleWarehouseSelect(warehouse)}
                >
                  <span>
                    <strong>{warehouse.name}</strong>
                    <small className="mono">{warehouse.warehouseCode}</small>
                  </span>
                  <span className={`badge ${warehouse.active ? 'badge-success' : 'badge-muted'}`}>
                    {warehouse.active ? 'Active' : 'Inactive'}
                  </span>
                </button>
              ))
            ) : (
              <p className="empty-copy">No warehouses are available yet.</p>
            )}
          </div>

          <form className="stack-form" onSubmit={saveWarehouse}>
            <div className="section-head compact">
              <div>
                <h3>{selectedWarehouse ? 'Edit warehouse' : 'Create warehouse'}</h3>
                <p>{selectedWarehouse ? `Editing ${selectedWarehouse.name}` : 'Create a new warehouse record.'}</p>
              </div>
            </div>

            <div className="filters two-up">
              <label>
                Warehouse code
                <input value={warehouseForm.warehouseCode} onChange={(event) => setWarehouseForm((current) => ({ ...current, warehouseCode: event.target.value }))} required />
              </label>
              <label>
                Name
                <input value={warehouseForm.name} onChange={(event) => setWarehouseForm((current) => ({ ...current, name: event.target.value }))} required />
              </label>
            </div>

            <div className="filters three-up">
              <label>
                Type
                <input value={warehouseForm.warehouseType} onChange={(event) => setWarehouseForm((current) => ({ ...current, warehouseType: event.target.value }))} required />
              </label>
              <label>
                City
                <input value={warehouseForm.city} onChange={(event) => setWarehouseForm((current) => ({ ...current, city: event.target.value }))} />
              </label>
              <label>
                Country code
                <input value={warehouseForm.countryCode} onChange={(event) => setWarehouseForm((current) => ({ ...current, countryCode: event.target.value.toUpperCase() }))} maxLength="2" />
              </label>
            </div>

            <div className="filters two-up">
              <label>
                Address line 1
                <input value={warehouseForm.addressLine1} onChange={(event) => setWarehouseForm((current) => ({ ...current, addressLine1: event.target.value }))} />
              </label>
              <label>
                Address line 2
                <input value={warehouseForm.addressLine2} onChange={(event) => setWarehouseForm((current) => ({ ...current, addressLine2: event.target.value }))} />
              </label>
            </div>

            <div className="filters three-up">
              <label>
                State / province
                <input value={warehouseForm.stateProvince} onChange={(event) => setWarehouseForm((current) => ({ ...current, stateProvince: event.target.value }))} />
              </label>
              <label>
                Postal code
                <input value={warehouseForm.postalCode} onChange={(event) => setWarehouseForm((current) => ({ ...current, postalCode: event.target.value }))} />
              </label>
              <label className="checkbox-tile">
                <span>Active</span>
                <input type="checkbox" checked={warehouseForm.active} onChange={(event) => setWarehouseForm((current) => ({ ...current, active: event.target.checked }))} />
              </label>
            </div>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={!canManage || warehouseSaving}>
                {warehouseSaving ? 'Saving...' : selectedWarehouseId ? 'Update warehouse' : 'Create warehouse'}
              </button>
              <button className="btn btn-outline" type="button" onClick={() => {
                setSelectedWarehouseId('')
                setWarehouseForm(DEFAULT_WAREHOUSE_FORM)
              }}>
                Reset
              </button>
              {selectedWarehouseId ? (
                <button className="btn btn-outline btn-danger" type="button" disabled={!canManage || warehouseSaving} onClick={removeWarehouse}>
                  Delete
                </button>
              ) : null}
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Bins</h3>
              <p>{selectedWarehouse ? `Bins for ${selectedWarehouse.name}` : 'Select a warehouse to manage bins.'}</p>
            </div>
            <button
              className="btn btn-outline btn-sm"
              type="button"
              disabled={!selectedWarehouseId}
              onClick={() => {
                setSelectedBinId('')
                setBinForm(DEFAULT_BIN_FORM)
              }}
            >
              New bin
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Bin</th>
                  <th>Zone / aisle</th>
                  <th>Type</th>
                  <th>Sequence</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {binsPage.content.length ? (
                  binsPage.content.map((bin) => (
                    <tr
                      key={bin.id}
                      className={selectedBinId === bin.id ? 'row-active' : ''}
                      onClick={() => handleBinSelect(bin)}
                    >
                      <td>
                        <strong>{bin.binCode}</strong>
                        <div className="subtle-meta mono">{bin.barcodeValue || bin.qrCodeValue || '-'}</div>
                      </td>
                      <td>{bin.zoneCode || '-'} {bin.aisleCode ? `/ ${bin.aisleCode}` : ''}</td>
                      <td>{bin.binType}</td>
                      <td>{bin.pickSequence}</td>
                      <td>{formatDateTime(bin.updatedAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No bins exist for the selected warehouse yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <form className="stack-form" onSubmit={saveBin}>
            <div className="section-head compact">
              <div>
                <h3>{selectedBinId ? 'Edit bin' : 'Create bin'}</h3>
                <p>{selectedWarehouse ? `Warehouse: ${selectedWarehouse.name}` : 'Select a warehouse before creating bins.'}</p>
              </div>
            </div>

            <div className="filters two-up">
              <label>
                Bin code
                <input value={binForm.binCode} onChange={(event) => setBinForm((current) => ({ ...current, binCode: event.target.value }))} required disabled={!selectedWarehouseId} />
              </label>
              <label>
                Bin type
                <input value={binForm.binType} onChange={(event) => setBinForm((current) => ({ ...current, binType: event.target.value }))} required disabled={!selectedWarehouseId} />
              </label>
            </div>

            <div className="filters four-up">
              <label>
                Zone
                <input value={binForm.zoneCode} onChange={(event) => setBinForm((current) => ({ ...current, zoneCode: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
              <label>
                Aisle
                <input value={binForm.aisleCode} onChange={(event) => setBinForm((current) => ({ ...current, aisleCode: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
              <label>
                Shelf
                <input value={binForm.shelfCode} onChange={(event) => setBinForm((current) => ({ ...current, shelfCode: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
              <label>
                Pick sequence
                <input type="number" min="0" value={binForm.pickSequence} onChange={(event) => setBinForm((current) => ({ ...current, pickSequence: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
            </div>

            <div className="filters two-up">
              <label>
                Barcode value
                <input value={binForm.barcodeValue} onChange={(event) => setBinForm((current) => ({ ...current, barcodeValue: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
              <label>
                QR value
                <input value={binForm.qrCodeValue} onChange={(event) => setBinForm((current) => ({ ...current, qrCodeValue: event.target.value }))} disabled={!selectedWarehouseId} />
              </label>
            </div>

            <label className="checkbox-tile">
              <span>Active</span>
              <input type="checkbox" checked={binForm.active} onChange={(event) => setBinForm((current) => ({ ...current, active: event.target.checked }))} disabled={!selectedWarehouseId} />
            </label>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={!canManage || !selectedWarehouseId || binSaving}>
                {binSaving ? 'Saving...' : selectedBinId ? 'Update bin' : 'Create bin'}
              </button>
              <button className="btn btn-outline" type="button" onClick={() => {
                setSelectedBinId('')
                setBinForm(DEFAULT_BIN_FORM)
              }}>
                Reset
              </button>
              {selectedBinId ? (
                <button className="btn btn-outline btn-danger" type="button" disabled={!canManage || binSaving} onClick={removeBin}>
                  Delete
                </button>
              ) : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}
