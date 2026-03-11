import { useEffect, useMemo, useState } from 'react'
import {
  activateServiceArea,
  createServiceArea,
  deactivateServiceArea,
  deleteServiceArea,
  listServiceAreas,
  updateServiceArea,
  validateServiceAreaRules
} from '../shared/api/endpoints/serviceAreasApi'
import { listStores } from '../shared/api/endpoints/storesApi'
import { Spinner } from '../shared/ui/Spinner'
import { formatDateTime } from '../shared/ui/formatters'

const SERVICE_AREA_TYPES = ['RADIUS', 'POLYGON', 'CITY', 'DISTRICT']
const SERVICE_AREA_STATUSES = ['ACTIVE', 'INACTIVE']
const SERVICE_TYPES = ['DELIVERY', 'PICKUP']

const DEFAULT_SERVICE_AREA_FORM = {
  name: '',
  type: 'RADIUS',
  status: 'ACTIVE',
  centerLatitude: '',
  centerLongitude: '',
  radiusMeters: '',
  polygonGeoJson: '',
  rulesJson: '',
  storeIds: {}
}

const DEFAULT_VALIDATION_FORM = {
  latitude: '',
  longitude: '',
  serviceType: 'DELIVERY'
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

function toOptionalInteger(value, label) {
  const numeric = toOptionalNumber(value, label)
  return numeric == null ? null : Math.trunc(numeric)
}

function normalizeText(value) {
  const trimmed = String(value || '').trim()
  return trimmed || null
}

function formatJson(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function servicesToFlags(storeIds = []) {
  const set = new Set(Array.isArray(storeIds) ? storeIds : [])
  return (id) => set.has(id)
}

function summarizeArea(area) {
  if (!area) return '-'
  switch (area.type) {
    case 'RADIUS':
      return [
        area.centerLatitude != null && area.centerLongitude != null
          ? `${area.centerLatitude}, ${area.centerLongitude}`
          : null,
        area.radiusMeters != null ? `${area.radiusMeters} m` : null
      ].filter(Boolean).join(' • ') || '-'
    case 'POLYGON': {
      try {
        const parsed = JSON.parse(area.polygonGeoJson || '{}')
        const points = parsed?.coordinates?.[0]?.length || 0
        return points ? `${points} polygon points` : 'Polygon JSON'
      } catch {
        return 'Polygon JSON'
      }
    }
    case 'CITY':
    case 'DISTRICT':
      return area.name || '-'
    default:
      return '-'
  }
}

function validationTone(result) {
  if (!result) return 'neutral'
  if (result.serviceAvailable) return 'success'
  return result.eligibilityReason === 'STORE_CLOSED' || result.eligibilityReason === 'OUT_OF_RANGE' ? 'warning' : 'danger'
}

export function ServiceAreasPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [serviceAreasPage, setServiceAreasPage] = useState({ content: [], totalElements: 0 })
  const [stores, setStores] = useState([])
  const [filters, setFilters] = useState({ query: '', status: '', type: '' })

  const [selectedServiceAreaId, setSelectedServiceAreaId] = useState('')
  const [serviceAreaForm, setServiceAreaForm] = useState(DEFAULT_SERVICE_AREA_FORM)
  const [validationForm, setValidationForm] = useState(DEFAULT_VALIDATION_FORM)
  const [validationResult, setValidationResult] = useState(null)

  const selectedServiceArea = serviceAreasPage.content.find((item) => String(item.id) === String(selectedServiceAreaId)) || null

  const visibleServiceAreas = useMemo(() => {
    const query = filters.query.trim().toLowerCase()
    return (serviceAreasPage.content || []).filter((area) => {
      if (filters.status && area.status !== filters.status) return false
      if (filters.type && area.type !== filters.type) return false
      if (!query) return true
      return [area.name, area.type, area.status].filter(Boolean).join(' ').toLowerCase().includes(query)
    })
  }, [filters, serviceAreasPage.content])

  const warnings = useMemo(() => {
    const activeAreas = (serviceAreasPage.content || []).filter((area) => area.status === 'ACTIVE')
    const items = []
    for (const area of activeAreas) {
      if ((area.type === 'RADIUS') && (area.centerLatitude == null || area.centerLongitude == null || area.radiusMeters == null)) {
        items.push(`${area.name} is active but missing radius geometry.`)
      }
      if (area.type === 'POLYGON' && !area.polygonGeoJson) {
        items.push(`${area.name} is active but missing polygon GeoJSON.`)
      }
      if (!Array.isArray(area.storeIds) || !area.storeIds.length) {
        items.push(`${area.name} has no explicitly assigned stores; selection will fall back to the global active store pool.`)
      }
    }
    return items
  }, [serviceAreasPage.content])

  async function load(nextSelectedId = selectedServiceAreaId) {
    setLoading(true)
    setError('')
    try {
      const [areas, storesPage] = await Promise.all([
        listServiceAreas({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }),
        listStores({ page: 0, size: 100 })
      ])
      const content = areas?.content || []
      setServiceAreasPage(areas || { content: [], totalElements: 0 })
      setStores(storesPage?.content || [])

      if (nextSelectedId && content.some((item) => String(item.id) === String(nextSelectedId))) {
        return
      }

      setSelectedServiceAreaId('')
      setServiceAreaForm(DEFAULT_SERVICE_AREA_FORM)
    } catch (requestError) {
      setError(requestError.message || 'Failed to load service area workspace.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function resetForm() {
    setSelectedServiceAreaId('')
    setServiceAreaForm(DEFAULT_SERVICE_AREA_FORM)
  }

  function selectServiceArea(area) {
    const hasStoreId = servicesToFlags(area.storeIds)
    setFlash('')
    setError('')
    setSelectedServiceAreaId(area.id)
    setServiceAreaForm({
      name: area.name || '',
      type: area.type || 'RADIUS',
      status: area.status || 'ACTIVE',
      centerLatitude: area.centerLatitude?.toString?.() || '',
      centerLongitude: area.centerLongitude?.toString?.() || '',
      radiusMeters: area.radiusMeters?.toString?.() || '',
      polygonGeoJson: formatJson(area.polygonGeoJson),
      rulesJson: formatJson(area.rulesJson),
      storeIds: Object.fromEntries((stores || []).map((store) => [store.id, hasStoreId(store.id)]))
    })
  }

  async function saveServiceArea(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const selectedStoreIds = Object.entries(serviceAreaForm.storeIds || {})
        .filter(([, checked]) => Boolean(checked))
        .map(([storeId]) => storeId)

      const payload = {
        name: serviceAreaForm.name.trim(),
        type: serviceAreaForm.type,
        status: serviceAreaForm.status,
        centerLatitude: serviceAreaForm.type === 'RADIUS' ? toOptionalNumber(serviceAreaForm.centerLatitude, 'Center latitude') : null,
        centerLongitude: serviceAreaForm.type === 'RADIUS' ? toOptionalNumber(serviceAreaForm.centerLongitude, 'Center longitude') : null,
        radiusMeters: serviceAreaForm.type === 'RADIUS' ? toOptionalInteger(serviceAreaForm.radiusMeters, 'Radius') : null,
        polygonGeoJson: serviceAreaForm.type === 'POLYGON' ? normalizeText(serviceAreaForm.polygonGeoJson) : null,
        rulesJson: normalizeText(serviceAreaForm.rulesJson),
        storeIds: selectedStoreIds.length ? selectedStoreIds : null
      }

      if (payload.type === 'RADIUS' && (payload.centerLatitude == null || payload.centerLongitude == null || payload.radiusMeters == null)) {
        throw new Error('Radius service areas require center latitude, center longitude, and radius meters.')
      }
      if (payload.type === 'POLYGON' && !payload.polygonGeoJson) {
        throw new Error('Polygon service areas require GeoJSON.')
      }

      const saved = selectedServiceAreaId
        ? await updateServiceArea(selectedServiceAreaId, payload)
        : await createServiceArea(payload)

      setFlash(selectedServiceAreaId ? 'Service area updated.' : 'Service area created.')
      await load(saved.id)
      resetForm()
    } catch (requestError) {
      setError(requestError.message || 'Unable to save service area.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleServiceAreaStatus(area) {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      if (area.status === 'ACTIVE') {
        await deactivateServiceArea(area.id)
        setFlash('Service area deactivated.')
      } else {
        await activateServiceArea(area.id)
        setFlash('Service area activated.')
      }
      await load(area.id)
    } catch (requestError) {
      setError(requestError.message || 'Unable to update service area status.')
    } finally {
      setSaving(false)
    }
  }

  async function removeServiceArea() {
    if (!selectedServiceAreaId) return
    if (!window.confirm('Delete this service area?')) return
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await deleteServiceArea(selectedServiceAreaId)
      setFlash('Service area deleted.')
      resetForm()
      await load('')
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete service area.')
    } finally {
      setSaving(false)
    }
  }

  async function runValidation(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const result = await validateServiceAreaRules({
        latitude: toOptionalNumber(validationForm.latitude, 'Latitude'),
        longitude: toOptionalNumber(validationForm.longitude, 'Longitude'),
        serviceType: validationForm.serviceType,
        at: null,
        maxDistanceMeters: null
      })
      setValidationResult(result)
    } catch (requestError) {
      setError(requestError.message || 'Unable to validate coordinates.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading service areas..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Service Areas</h2>
        <p>Manage delivery coverage rules, assign stores, and validate coordinates against the active location engine.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      {warnings.length ? (
        <div className="panel" style={{ marginBottom: 20 }}>
          <div className="section-head compact">
            <div>
              <h3>Coverage warnings</h3>
              <p>Operational issues detected from the current active service-area set.</p>
            </div>
          </div>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Service area list</h3>
              <p>Filter and select an area to edit or toggle activation.</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => load()}>
              Refresh
            </button>
          </div>

          <div className="filters">
            <label>
              Search
              <input
                value={filters.query}
                onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Search by name or type"
              />
            </label>
            <label>
              Status
              <select
                value={filters.status}
                onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}
              >
                <option value="">All</option>
                {SERVICE_AREA_STATUSES.map((value) => (
                  <option key={value} value={value}>{value}</option>
                ))}
              </select>
            </label>
            <label>
              Type
              <select
                value={filters.type}
                onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))}
              >
                <option value="">All</option>
                {SERVICE_AREA_TYPES.map((value) => (
                  <option key={value} value={value}>{value}</option>
                ))}
              </select>
            </label>
            <button className="btn btn-outline" onClick={resetForm}>New</button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Geometry</th>
                  <th>Stores</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {visibleServiceAreas.length ? (
                  visibleServiceAreas.map((area) => (
                    <tr
                      key={area.id}
                      className={String(area.id) === String(selectedServiceAreaId) ? 'row-selected' : ''}
                      onClick={() => selectServiceArea(area)}
                      role="button"
                      tabIndex={0}
                    >
                      <td>
                        <strong>{area.name}</strong>
                        <div className="subtle-meta">{area.id}</div>
                      </td>
                      <td>{area.type}</td>
                      <td>{area.status}</td>
                      <td>{summarizeArea(area)}</td>
                      <td>{Array.isArray(area.storeIds) ? area.storeIds.length : 0}</td>
                      <td>{formatDateTime(area.updatedAt || area.createdAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" className="empty-row">No service areas found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{selectedServiceArea ? 'Edit service area' : 'Create service area'}</h3>
              <p>Use radius, polygon, city, or district rules without exposing provider-specific logic to the UI.</p>
            </div>
          </div>

          <form onSubmit={saveServiceArea}>
            <div className="form-grid">
              <label className="span-2">
                Name
                <input
                  value={serviceAreaForm.name}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, name: event.target.value }))}
                  required
                />
              </label>
              <label>
                Type
                <select
                  value={serviceAreaForm.type}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, type: event.target.value }))}
                >
                  {SERVICE_AREA_TYPES.map((value) => (
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </label>
              <label>
                Status
                <select
                  value={serviceAreaForm.status}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, status: event.target.value }))}
                >
                  {SERVICE_AREA_STATUSES.map((value) => (
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </label>
              <label>
                Center latitude
                <input
                  value={serviceAreaForm.centerLatitude}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, centerLatitude: event.target.value }))}
                  placeholder={serviceAreaForm.type === 'RADIUS' ? 'Required for radius' : 'Unused'}
                />
              </label>
              <label>
                Center longitude
                <input
                  value={serviceAreaForm.centerLongitude}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, centerLongitude: event.target.value }))}
                  placeholder={serviceAreaForm.type === 'RADIUS' ? 'Required for radius' : 'Unused'}
                />
              </label>
              <label>
                Radius meters
                <input
                  value={serviceAreaForm.radiusMeters}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, radiusMeters: event.target.value }))}
                  placeholder={serviceAreaForm.type === 'RADIUS' ? 'Required for radius' : 'Unused'}
                />
              </label>
              <label className="span-2">
                Polygon GeoJSON
                <textarea
                  rows="6"
                  value={serviceAreaForm.polygonGeoJson}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, polygonGeoJson: event.target.value }))}
                  placeholder="Required for polygon service areas"
                />
              </label>
              <label className="span-2">
                Rules JSON
                <textarea
                  rows="6"
                  value={serviceAreaForm.rulesJson}
                  onChange={(event) => setServiceAreaForm((current) => ({ ...current, rulesJson: event.target.value }))}
                  placeholder='Optional JSON, e.g. {"defaultStoreId":"...","maxDistanceMeters":5000}'
                />
              </label>
            </div>

            <div className="section-head compact" style={{ marginTop: 18 }}>
              <div>
                <h3>Assigned stores</h3>
                <p>Assigned stores are preferred when the service area matches. Leave empty to fall back to the global active store pool.</p>
              </div>
            </div>
            <div className="toggle-row">
              {stores.map((store) => (
                <label className="toggle" key={store.id}>
                  <input
                    type="checkbox"
                    checked={Boolean(serviceAreaForm.storeIds?.[store.id])}
                    onChange={(event) =>
                      setServiceAreaForm((current) => ({
                        ...current,
                        storeIds: { ...(current.storeIds || {}), [store.id]: event.target.checked }
                      }))
                    }
                  />
                  {store.name}
                </label>
              ))}
            </div>

            <div className="inline-actions wrap" style={{ marginTop: 18 }}>
              <button className="btn btn-primary" type="submit" disabled={saving || !serviceAreaForm.name.trim()}>
                {saving ? 'Saving...' : selectedServiceAreaId ? 'Update service area' : 'Create service area'}
              </button>
              <button className="btn btn-outline" type="button" disabled={saving} onClick={resetForm}>
                Reset
              </button>
              {selectedServiceArea ? (
                <button className="btn btn-outline" type="button" disabled={saving} onClick={() => toggleServiceAreaStatus(selectedServiceArea)}>
                  {selectedServiceArea.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                </button>
              ) : null}
              {selectedServiceAreaId ? (
                <button className="btn btn-outline" type="button" disabled={saving} onClick={removeServiceArea}>
                  Delete
                </button>
              ) : null}
            </div>
          </form>
        </section>
      </div>

      <section className="panel" style={{ marginTop: 20 }}>
        <div className="section-head">
          <div>
            <h3>Validation sandbox</h3>
            <p>Test coordinates against the active location rules and current store assignment logic.</p>
          </div>
        </div>

        <form onSubmit={runValidation}>
          <div className="form-grid">
            <label>
              Latitude
              <input
                value={validationForm.latitude}
                onChange={(event) => setValidationForm((current) => ({ ...current, latitude: event.target.value }))}
                required
              />
            </label>
            <label>
              Longitude
              <input
                value={validationForm.longitude}
                onChange={(event) => setValidationForm((current) => ({ ...current, longitude: event.target.value }))}
                required
              />
            </label>
            <label>
              Service type
              <select
                value={validationForm.serviceType}
                onChange={(event) => setValidationForm((current) => ({ ...current, serviceType: event.target.value }))}
              >
                {SERVICE_TYPES.map((value) => (
                  <option key={value} value={value}>{value}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="inline-actions wrap" style={{ marginTop: 18 }}>
            <button className="btn btn-primary" type="submit" disabled={saving}>Validate coordinates</button>
          </div>
        </form>

        {validationResult ? (
          <div className={`alert alert-${validationTone(validationResult) === 'success' ? 'success' : validationTone(validationResult) === 'warning' ? 'warning' : 'error'}`} style={{ marginTop: 18 }}>
            <strong>{validationResult.serviceAvailable ? 'Service available' : 'Service unavailable'}</strong>
            <div className="subtle-meta" style={{ marginTop: 6 }}>
              Reason: {validationResult.eligibilityReason || '-'}
              {' • '}Matched service area: {validationResult.matchedServiceAreaId || '-'}
              {' • '}Matched store: {validationResult.matchedStoreId || '-'}
              {' • '}Distance: {validationResult.distanceMeters != null ? `${validationResult.distanceMeters} m` : '-'}
            </div>
          </div>
        ) : null}
      </section>
    </div>
  )
}
