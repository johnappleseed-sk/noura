import { useEffect, useMemo, useState } from 'react'
import { listAdminStores, getAdminStore, updateAdminStoreStatus } from '../shared/api/endpoints/storeAdminApi'
import { PaginationControls } from '../shared/ui/PaginationControls'
import { Spinner } from '../shared/ui/Spinner'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/PlatformOpsPages.css'

const STORE_TYPES = ['MERCHANT', 'BRANCH', 'PARTNER']
const STORE_STATUSES = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'CLOSED']
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function normalizePage(data) {
  return {
    content: data?.content || [],
    page: Number(data?.page) || 0,
    size: Number(data?.size) || 20,
    totalElements: Number(data?.totalElements) || 0,
    totalPages: Math.max(1, Number(data?.totalPages) || 1),
    first: Boolean(data?.first),
    last: Boolean(data?.last)
  }
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.error?.detail || error?.response?.data?.message || error?.message || fallbackMessage
}

function toDisplayDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString()
}

function toStatusBadgeClass(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'ACTIVE') return 'badge badge-success'
  if (value === 'SUSPENDED') return 'badge badge-warning'
  if (value === 'CLOSED') return 'badge badge-danger'
  return 'badge badge-muted'
}

function resolveStatusAction(store) {
  const current = String(store?.status || '').toUpperCase()
  if (current === 'ACTIVE') {
    return { nextStatus: 'SUSPENDED', label: 'Suspend' }
  }
  return { nextStatus: 'ACTIVE', label: 'Activate' }
}

function parseMerchantId(value) {
  const raw = String(value || '').trim()
  if (!raw) return { value: undefined, error: '' }
  if (!UUID_PATTERN.test(raw)) {
    return { value: undefined, error: 'Merchant ID must be a valid UUID.' }
  }
  return { value: raw, error: '' }
}

export function StoresPage() {
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [busyStoreId, setBusyStoreId] = useState('')
  const [selectedStoreId, setSelectedStoreId] = useState('')
  const [selectedStore, setSelectedStore] = useState(null)
  const [filters, setFilters] = useState({ search: '', merchantId: '', type: '', status: '' })
  const [draftFilters, setDraftFilters] = useState({ search: '', merchantId: '', type: '', status: '' })
  const [pager, setPager] = useState({ page: 0, size: 20 })
  const [storePage, setStorePage] = useState(normalizePage())
  useToastFeedback({ successMessage: flash, errorMessage: error })

  async function loadStores() {
    setLoading(true)
    setError('')

    const parsedMerchantId = parseMerchantId(filters.merchantId)
    if (parsedMerchantId.error) {
      setError(parsedMerchantId.error)
      setStorePage(normalizePage())
      setLoading(false)
      return
    }

    try {
      const result = await listAdminStores({
        page: pager.page,
        size: pager.size,
        sortBy: 'createdAt',
        direction: 'desc',
        search: filters.search.trim() || undefined,
        merchantId: parsedMerchantId.value,
        type: filters.type || undefined,
        status: filters.status || undefined
      })
      const normalized = normalizePage(result)
      setStorePage(normalized)
      if (selectedStoreId && !normalized.content.some((item) => String(item.id) === String(selectedStoreId))) {
        setSelectedStoreId('')
        setSelectedStore(null)
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load stores.'))
      setStorePage(normalizePage())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadStores()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pager.page, pager.size, filters.search, filters.merchantId, filters.type, filters.status])

  async function loadSelectedStore(storeId) {
    setSelectedStoreId(storeId)
    setSelectedStore(null)
    setDetailLoading(true)
    setError('')
    try {
      const detail = await getAdminStore(storeId)
      setSelectedStore(detail)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load store details.'))
    } finally {
      setDetailLoading(false)
    }
  }

  async function handleStatusUpdate(store) {
    const { nextStatus, label } = resolveStatusAction(store)
    setBusyStoreId(String(store.id))
    setError('')
    setFlash('')
    try {
      const updated = await updateAdminStoreStatus(store.id, { status: nextStatus })
      setFlash(`Store ${label.toLowerCase()}d.`)
      setStorePage((current) => ({
        ...current,
        content: current.content.map((item) => (String(item.id) === String(updated.id) ? updated : item))
      }))
      if (selectedStoreId && String(selectedStoreId) === String(updated.id)) {
        setSelectedStore(updated)
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to update store status.'))
    } finally {
      setBusyStoreId('')
    }
  }

  const stores = useMemo(() => storePage.content || [], [storePage.content])

  if (loading) {
    return <Spinner label="Loading stores..." />
  }

  return (
    <div className="page platform-ops-page">
      <div className="page-head">
        <div>
          <h2>Stores</h2>
          <p>Review store records by merchant, type, and status with live admin module data.</p>
        </div>
      </div>

      <div className="panel-grid platform-ops-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Store list</h3>
              <p>Data source: <code>/api/v1/admin/stores</code>.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={() => void loadStores()}>
              Refresh
            </button>
          </div>

          <div className="filters four-up platform-filters">
            <label className="grow">
              Search
              <input
                value={draftFilters.search}
                onChange={(event) => setDraftFilters((current) => ({ ...current, search: event.target.value }))}
                placeholder="Store code, name, or slug"
              />
            </label>

            <label>
              Merchant ID
              <input
                value={draftFilters.merchantId}
                onChange={(event) => setDraftFilters((current) => ({ ...current, merchantId: event.target.value }))}
                placeholder="UUID"
              />
            </label>

            <label>
              Type
              <select
                value={draftFilters.type}
                onChange={(event) => setDraftFilters((current) => ({ ...current, type: event.target.value }))}
              >
                <option value="">All</option>
                {STORE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Status
              <select
                value={draftFilters.status}
                onChange={(event) => setDraftFilters((current) => ({ ...current, status: event.target.value }))}
              >
                <option value="">All</option>
                {STORE_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>

            <div className="inline-actions platform-inline-actions">
              <button
                className="btn btn-outline"
                type="button"
                onClick={() => {
                  setFilters({ ...draftFilters })
                  setPager((current) => ({ ...current, page: 0 }))
                }}
              >
                Apply
              </button>
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => {
                  const reset = { search: '', merchantId: '', type: '', status: '' }
                  setDraftFilters(reset)
                  setFilters(reset)
                  setPager((current) => ({ ...current, page: 0 }))
                }}
              >
                Clear
              </button>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Store code</th>
                  <th>Name</th>
                  <th>Merchant ID</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>City</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {stores.length ? (
                  stores.map((store) => {
                    const statusAction = resolveStatusAction(store)
                    const busy = busyStoreId === String(store.id)
                    const selected = selectedStoreId === String(store.id)
                    return (
                      <tr key={store.id} className={selected ? 'row-selected' : ''}>
                        <td className="mono">{store.storeCode || '-'}</td>
                        <td>
                          <button
                            className="table-link-button"
                            type="button"
                            onClick={() => void loadSelectedStore(store.id)}
                          >
                            {store.name || '-'}
                          </button>
                        </td>
                        <td className="mono">{store.merchantId || '-'}</td>
                        <td>{store.type || '-'}</td>
                        <td>
                          <span className={toStatusBadgeClass(store.status)}>{store.status || 'UNKNOWN'}</span>
                        </td>
                        <td>{store.city || '-'}</td>
                        <td>
                          <button
                            className="btn btn-outline btn-xs"
                            type="button"
                            disabled={busy}
                            onClick={() => void handleStatusUpdate(store)}
                          >
                            {busy ? 'Updating...' : statusAction.label}
                          </button>
                        </td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td className="empty-row" colSpan={7}>No stores found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <PaginationControls
            page={storePage.page}
            totalPages={storePage.totalPages}
            totalElements={storePage.totalElements}
            first={storePage.first}
            last={storePage.last}
            pageSize={storePage.size}
            onPageSizeChange={(size) => setPager({ page: 0, size })}
            onPrev={() => setPager((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
            onNext={() => setPager((current) => ({ ...current, page: current.page + 1 }))}
            noun="stores"
          />
        </section>

        <section className="panel platform-side-panel">
          <div className="section-head">
            <div>
              <h3>Store details</h3>
              <p>Load details from <code>/api/v1/admin/stores/&#123;id&#125;</code>.</p>
            </div>
          </div>

          {detailLoading ? <Spinner label="Loading store details..." /> : null}

          {!detailLoading && !selectedStore ? (
            <p className="empty-copy">Select a store from the list to view details.</p>
          ) : null}

          {!detailLoading && selectedStore ? (
            <dl className="platform-detail-grid">
              <div><dt>ID</dt><dd className="mono">{selectedStore.id}</dd></div>
              <div><dt>Store code</dt><dd>{selectedStore.storeCode || '-'}</dd></div>
              <div><dt>Name</dt><dd>{selectedStore.name || '-'}</dd></div>
              <div><dt>Slug</dt><dd>{selectedStore.slug || '-'}</dd></div>
              <div><dt>Merchant ID</dt><dd className="mono">{selectedStore.merchantId || '-'}</dd></div>
              <div><dt>Type</dt><dd>{selectedStore.type || '-'}</dd></div>
              <div><dt>Status</dt><dd>{selectedStore.status || '-'}</dd></div>
              <div><dt>Contact</dt><dd>{selectedStore.contactEmail || selectedStore.contactPhone || '-'}</dd></div>
              <div><dt>Country</dt><dd>{selectedStore.countryCode || '-'}</dd></div>
              <div><dt>City</dt><dd>{selectedStore.city || '-'}</dd></div>
              <div><dt>Address 1</dt><dd>{selectedStore.addressLine1 || '-'}</dd></div>
              <div><dt>Address 2</dt><dd>{selectedStore.addressLine2 || '-'}</dd></div>
              <div><dt>Created</dt><dd>{toDisplayDate(selectedStore.createdAt)}</dd></div>
              <div><dt>Updated</dt><dd>{toDisplayDate(selectedStore.updatedAt)}</dd></div>
            </dl>
          ) : null}
        </section>
      </div>
    </div>
  )
}
