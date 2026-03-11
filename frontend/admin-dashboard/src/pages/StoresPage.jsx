import { useEffect, useMemo, useState } from 'react'
import { createStore, deleteStore, listStores, updateStore } from '../shared/api/endpoints/storesApi'
import { Spinner } from '../shared/ui/Spinner'
import { SortableHeader } from '../shared/ui/SortableHeader'

const SERVICE_TYPES = ['PICKUP', 'DELIVERY', 'CURBSIDE', 'B2B_DESK']

const DEFAULT_STORE_FORM = {
  name: '',
  addressLine1: '',
  city: '',
  state: '',
  zipCode: '',
  country: 'US',
  region: '',
  latitude: '',
  longitude: '',
  serviceRadiusMeters: '',
  openTime: '09:00',
  closeTime: '18:00',
  active: true,
  services: Object.fromEntries(SERVICE_TYPES.map((value) => [value, value === 'PICKUP'])),
  shippingFee: '0',
  freeShippingThreshold: '0'
}

function normalizeTimeForInput(value) {
  const raw = value == null ? '' : String(value)
  if (!raw) return ''
  if (raw.length >= 5) return raw.slice(0, 5)
  return raw
}

function toLocalTime(value, label) {
  const raw = String(value || '').trim()
  if (!raw) {
    throw new Error(`${label} is required.`)
  }
  if (/^\\d{2}:\\d{2}$/.test(raw)) return `${raw}:00`
  if (/^\\d{2}:\\d{2}:\\d{2}$/.test(raw)) return raw
  throw new Error(`${label} must be in HH:MM or HH:MM:SS format.`)
}

function toNumber(value, label) {
  const raw = String(value ?? '').trim()
  if (!raw) {
    throw new Error(`${label} is required.`)
  }
  const numeric = Number(raw)
  if (Number.isNaN(numeric)) {
    throw new Error(`${label} must be a number.`)
  }
  return numeric
}

function toOptionalNumber(value, label) {
  const raw = String(value ?? '').trim()
  if (!raw) {
    return null
  }
  const numeric = Number(raw)
  if (Number.isNaN(numeric)) {
    throw new Error(`${label} must be a number.`)
  }
  return numeric
}

function selectedServices(services) {
  return SERVICE_TYPES.filter((type) => Boolean(services?.[type]))
}

function servicesToFlags(services = []) {
  const set = new Set(Array.isArray(services) ? services : [])
  return Object.fromEntries(SERVICE_TYPES.map((value) => [value, set.has(value)]))
}

export function StoresPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [storesPage, setStoresPage] = useState({ content: [], totalElements: 0 })
  const [filters, setFilters] = useState({ search: '', service: '', openNow: false })
  const [storeSort, setStoreSort] = useState({ sortBy: 'name', direction: 'asc' })

  const [selectedStoreId, setSelectedStoreId] = useState('')
  const [storeForm, setStoreForm] = useState(DEFAULT_STORE_FORM)

  const selectedStore = storesPage.content.find((item) => String(item.id) === String(selectedStoreId)) || null

  const visibleStores = useMemo(() => {
    const q = filters.search.trim().toLowerCase()
    if (!q) return storesPage.content
    return storesPage.content.filter((store) => String(store.name || '').toLowerCase().includes(q))
  }, [filters.search, storesPage.content])

  async function load(nextSelectedId = selectedStoreId) {
    setLoading(true)
    setError('')
    try {
      const page = await listStores({
        page: 0,
        size: 100,
        sortBy: storeSort.sortBy,
        direction: storeSort.direction,
        service: filters.service || undefined,
        openNow: filters.openNow ? true : undefined
      })
      const content = page?.content || []
      setStoresPage(page || { content: [], totalElements: 0 })

      if (nextSelectedId && content.some((item) => String(item.id) === String(nextSelectedId))) {
        return
      }

      setSelectedStoreId('')
      setStoreForm(DEFAULT_STORE_FORM)
    } catch (err) {
      setError(err.message || 'Failed to load stores.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [storeSort.sortBy, storeSort.direction])

  function selectStore(store) {
    setFlash('')
    setError('')
    setSelectedStoreId(store.id)
    setStoreForm({
      name: store.name || '',
      addressLine1: store.addressLine1 || '',
      city: store.city || '',
      state: store.state || '',
      zipCode: store.zipCode || '',
      country: store.country || 'US',
      region: store.region || '',
      latitude: store.latitude?.toString?.() || '',
      longitude: store.longitude?.toString?.() || '',
      serviceRadiusMeters: store.serviceRadiusMeters?.toString?.() || '',
      openTime: normalizeTimeForInput(store.openTime),
      closeTime: normalizeTimeForInput(store.closeTime),
      active: Boolean(store.active),
      services: servicesToFlags(store.services || []),
      shippingFee: store.shippingFee?.toString?.() || '0',
      freeShippingThreshold: store.freeShippingThreshold?.toString?.() || '0'
    })
  }

  function resetForm() {
    setSelectedStoreId('')
    setStoreForm(DEFAULT_STORE_FORM)
  }

  async function saveStore(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const services = selectedServices(storeForm.services)
      if (!services.length) {
        throw new Error('Select at least one service type.')
      }
      const payload = {
        name: storeForm.name.trim(),
        addressLine1: storeForm.addressLine1.trim(),
        city: storeForm.city.trim(),
        state: storeForm.state.trim(),
        zipCode: storeForm.zipCode.trim(),
        country: storeForm.country.trim(),
        region: storeForm.region.trim(),
        latitude: toNumber(storeForm.latitude, 'Latitude'),
        longitude: toNumber(storeForm.longitude, 'Longitude'),
        serviceRadiusMeters: toOptionalNumber(storeForm.serviceRadiusMeters, 'Service radius'),
        openTime: toLocalTime(storeForm.openTime, 'Open time'),
        closeTime: toLocalTime(storeForm.closeTime, 'Close time'),
        active: Boolean(storeForm.active),
        services,
        shippingFee: toNumber(storeForm.shippingFee, 'Shipping fee'),
        freeShippingThreshold: toNumber(storeForm.freeShippingThreshold, 'Free shipping threshold')
      }

      const saved = selectedStoreId
        ? await updateStore(selectedStoreId, payload)
        : await createStore(payload)

      setFlash(selectedStoreId ? 'Store updated.' : 'Store created.')
      await load(saved.id)
      resetForm()
    } catch (err) {
      setError(err.message || 'Unable to save store.')
    } finally {
      setSaving(false)
    }
  }

  async function removeStore() {
    if (!selectedStoreId) return
    if (!window.confirm('Delete this store?')) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteStore(selectedStoreId)
      setFlash('Store deleted.')
      resetForm()
      await load('')
    } catch (err) {
      setError(err.message || 'Unable to delete store.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading stores..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Stores</h2>
        <p>Create and manage store coverage, operating hours, service types, and shipping thresholds.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Store list</h3>
              <p>Stores are listed via the public store endpoint (currently returns active records only).</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => load()}>
              Refresh
            </button>
          </div>

          <div className="filters">
            <label>
              Search
              <input
                value={filters.search}
                onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))}
                placeholder="Search by name"
              />
            </label>
            <label>
              Service filter
              <select
                value={filters.service}
                onChange={(event) => setFilters((current) => ({ ...current, service: event.target.value }))}
              >
                <option value="">All</option>
                {SERVICE_TYPES.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={filters.openNow}
                onChange={(event) => setFilters((current) => ({ ...current, openNow: event.target.checked }))}
              />
              Open now
            </label>
            <button className="btn btn-outline" onClick={() => load()}>
              Apply
            </button>
            <button className="btn btn-outline" onClick={resetForm}>
              New
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <SortableHeader label="Store" field="name" sortBy={storeSort.sortBy} direction={storeSort.direction} onSort={(f, d) => setStoreSort({ sortBy: f, direction: d })} />
                  <SortableHeader label="Region" field="region" sortBy={storeSort.sortBy} direction={storeSort.direction} onSort={(f, d) => setStoreSort({ sortBy: f, direction: d })} />
                  <th>Services</th>
                  <th>Radius</th>
                  <th>Hours</th>
                  <th>Shipping</th>
                </tr>
              </thead>
              <tbody>
                {visibleStores.length ? (
                  visibleStores.map((store) => (
                    <tr
                      key={store.id}
                      className={String(store.id) === String(selectedStoreId) ? 'row-selected' : ''}
                      onClick={() => selectStore(store)}
                      role="button"
                      tabIndex={0}
                    >
                      <td>
                        <strong>{store.name}</strong>
                        <div className="subtle-meta">{store.city}, {store.state}</div>
                      </td>
                      <td>{store.region || '-'}</td>
                      <td className="mono">{(store.services || []).join(', ') || '-'}</td>
                      <td className="mono">{store.serviceRadiusMeters != null ? `${store.serviceRadiusMeters} m` : '-'}</td>
                      <td className="mono">
                        {normalizeTimeForInput(store.openTime)}-{normalizeTimeForInput(store.closeTime)}
                      </td>
                      <td className="mono">
                        {store.shippingFee != null ? `$${store.shippingFee}` : '-'} / {store.freeShippingThreshold != null ? `$${store.freeShippingThreshold}` : '-'}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" className="empty-row">No stores found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{selectedStore ? 'Edit store' : 'Create store'}</h3>
              <p>All fields are required by the platform store API.</p>
            </div>
          </div>

          <form onSubmit={saveStore}>
            <div className="form-grid">
              <label className="span-2">
                Name
                <input
                  value={storeForm.name}
                  onChange={(event) => setStoreForm((current) => ({ ...current, name: event.target.value }))}
                  required
                />
              </label>
              <label className="span-2">
                Address line 1
                <input
                  value={storeForm.addressLine1}
                  onChange={(event) => setStoreForm((current) => ({ ...current, addressLine1: event.target.value }))}
                  required
                />
              </label>
              <label>
                City
                <input value={storeForm.city} onChange={(event) => setStoreForm((c) => ({ ...c, city: event.target.value }))} required />
              </label>
              <label>
                State
                <input value={storeForm.state} onChange={(event) => setStoreForm((c) => ({ ...c, state: event.target.value }))} required />
              </label>
              <label>
                Zip
                <input value={storeForm.zipCode} onChange={(event) => setStoreForm((c) => ({ ...c, zipCode: event.target.value }))} required />
              </label>
              <label>
                Country
                <input value={storeForm.country} onChange={(event) => setStoreForm((c) => ({ ...c, country: event.target.value }))} required />
              </label>
              <label className="span-2">
                Region
                <input value={storeForm.region} onChange={(event) => setStoreForm((c) => ({ ...c, region: event.target.value }))} required />
              </label>
              <label>
                Latitude
                <input value={storeForm.latitude} onChange={(event) => setStoreForm((c) => ({ ...c, latitude: event.target.value }))} required />
              </label>
              <label>
                Longitude
                <input value={storeForm.longitude} onChange={(event) => setStoreForm((c) => ({ ...c, longitude: event.target.value }))} required />
              </label>
              <label>
                Service radius (meters)
                <input
                  value={storeForm.serviceRadiusMeters}
                  onChange={(event) => setStoreForm((c) => ({ ...c, serviceRadiusMeters: event.target.value }))}
                  placeholder="Optional"
                />
              </label>
              <label>
                Open time
                <input type="time" value={storeForm.openTime} onChange={(event) => setStoreForm((c) => ({ ...c, openTime: event.target.value }))} required />
              </label>
              <label>
                Close time
                <input type="time" value={storeForm.closeTime} onChange={(event) => setStoreForm((c) => ({ ...c, closeTime: event.target.value }))} required />
              </label>
              <label>
                Shipping fee
                <input value={storeForm.shippingFee} onChange={(event) => setStoreForm((c) => ({ ...c, shippingFee: event.target.value }))} required />
              </label>
              <label>
                Free shipping threshold
                <input
                  value={storeForm.freeShippingThreshold}
                  onChange={(event) => setStoreForm((c) => ({ ...c, freeShippingThreshold: event.target.value }))}
                  required
                />
              </label>
            </div>

            <div className="toggle-row">
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={storeForm.active}
                  onChange={(event) => setStoreForm((current) => ({ ...current, active: event.target.checked }))}
                />
                Active
              </label>
              {SERVICE_TYPES.map((type) => (
                <label className="toggle" key={type}>
                  <input
                    type="checkbox"
                    checked={Boolean(storeForm.services?.[type])}
                    onChange={(event) =>
                      setStoreForm((current) => ({
                        ...current,
                        services: { ...(current.services || {}), [type]: event.target.checked }
                      }))
                    }
                  />
                  {type}
                </label>
              ))}
            </div>

            <div className="inline-actions wrap">
              <button className="btn btn-primary" type="submit" disabled={saving || !storeForm.name.trim()}>
                {saving ? 'Saving...' : selectedStoreId ? 'Update store' : 'Create store'}
              </button>
              <button className="btn btn-outline" type="button" disabled={saving} onClick={resetForm}>
                Reset
              </button>
              {selectedStoreId ? (
                <button className="btn btn-outline" type="button" disabled={saving} onClick={removeStore}>
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
