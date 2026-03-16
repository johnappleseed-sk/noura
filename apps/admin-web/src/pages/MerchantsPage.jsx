import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { listAdminMerchants, updateAdminMerchantStatus } from '../shared/api/endpoints/merchantAdminApi'
import { PaginationControls } from '../shared/ui/PaginationControls'
import { Spinner } from '../shared/ui/Spinner'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/PlatformOpsPages.css'

const MERCHANT_STATUSES = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'INACTIVE']

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
  return date.toLocaleDateString()
}

function toStatusBadgeClass(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'ACTIVE') return 'badge badge-success'
  if (value === 'SUSPENDED') return 'badge badge-warning'
  if (value === 'INACTIVE') return 'badge badge-danger'
  return 'badge badge-muted'
}

function resolveStatusAction(merchant) {
  const current = String(merchant?.status || '').toUpperCase()
  if (current === 'ACTIVE') {
    return { nextStatus: 'SUSPENDED', label: 'Suspend' }
  }
  return { nextStatus: 'ACTIVE', label: 'Activate' }
}

export function MerchantsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [busyMerchantId, setBusyMerchantId] = useState('')
  const [filters, setFilters] = useState({ search: '', status: '' })
  const [draftFilters, setDraftFilters] = useState({ search: '', status: '' })
  const [pager, setPager] = useState({ page: 0, size: 20 })
  const [merchantPage, setMerchantPage] = useState(normalizePage())
  useToastFeedback({ successMessage: flash, errorMessage: error })

  useEffect(() => {
    if (!location.state?.flash) return
    setFlash(location.state.flash)
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  async function loadMerchants() {
    setLoading(true)
    setError('')
    try {
      const result = await listAdminMerchants({
        page: pager.page,
        size: pager.size,
        sortBy: 'createdAt',
        direction: 'desc',
        search: filters.search.trim() || undefined,
        status: filters.status || undefined
      })
      setMerchantPage(normalizePage(result))
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load merchants.'))
      setMerchantPage(normalizePage())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadMerchants()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pager.page, pager.size, filters.search, filters.status])

  async function handleStatusUpdate(merchant) {
    const { nextStatus, label } = resolveStatusAction(merchant)
    setBusyMerchantId(String(merchant.id))
    setError('')
    setFlash('')
    try {
      await updateAdminMerchantStatus(merchant.id, { status: nextStatus })
      setFlash(`Merchant ${label.toLowerCase()}d.`)
      await loadMerchants()
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to update merchant status.'))
    } finally {
      setBusyMerchantId('')
    }
  }

  const merchants = useMemo(() => merchantPage.content || [], [merchantPage.content])

  if (loading) {
    return <Spinner label="Loading merchants..." />
  }

  return (
    <div className="page platform-ops-page">
      <div className="page-head">
        <div>
          <h2>Merchants</h2>
          <p>Manage merchant onboarding records and operational status.</p>
        </div>
        <div className="page-head-actions">
          <Link className="btn btn-primary" to="/admin/merchants/create">
            Create merchant
          </Link>
        </div>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Merchant list</h3>
            <p>Direct integration with <code>/api/v1/admin/merchants</code>.</p>
          </div>
          <button className="btn btn-outline btn-sm" type="button" onClick={() => void loadMerchants()}>
            Refresh
          </button>
        </div>

        <div className="filters three-up platform-filters">
          <label className="grow">
            Search
            <input
              value={draftFilters.search}
              onChange={(event) => setDraftFilters((current) => ({ ...current, search: event.target.value }))}
              placeholder="Merchant code, legal name, display name"
            />
          </label>

          <label>
            Status
            <select
              value={draftFilters.status}
              onChange={(event) => setDraftFilters((current) => ({ ...current, status: event.target.value }))}
            >
              <option value="">All</option>
              {MERCHANT_STATUSES.map((status) => (
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
                const reset = { search: '', status: '' }
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
                <th>Code</th>
                <th>Legal name</th>
                <th>Display name</th>
                <th>Status</th>
                <th>Country</th>
                <th>Contract</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {merchants.length ? (
                merchants.map((merchant) => {
                  const statusAction = resolveStatusAction(merchant)
                  const busy = busyMerchantId === String(merchant.id)
                  return (
                    <tr key={merchant.id}>
                      <td className="mono">{merchant.merchantCode || '-'}</td>
                      <td>{merchant.legalName || '-'}</td>
                      <td>{merchant.displayName || '-'}</td>
                      <td>
                        <span className={toStatusBadgeClass(merchant.status)}>{merchant.status || 'UNKNOWN'}</span>
                      </td>
                      <td>{merchant.countryCode || '-'}</td>
                      <td>
                        <span className="subtle-meta">
                          {toDisplayDate(merchant.contractStartAt)} to {toDisplayDate(merchant.contractEndAt)}
                        </span>
                      </td>
                      <td>{toDisplayDate(merchant.createdAt)}</td>
                      <td>
                        <button
                          className="btn btn-outline btn-xs"
                          type="button"
                          disabled={busy}
                          onClick={() => void handleStatusUpdate(merchant)}
                        >
                          {busy ? 'Updating...' : statusAction.label}
                        </button>
                      </td>
                    </tr>
                  )
                })
              ) : (
                <tr>
                  <td className="empty-row" colSpan={8}>No merchants found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <PaginationControls
          page={merchantPage.page}
          totalPages={merchantPage.totalPages}
          totalElements={merchantPage.totalElements}
          first={merchantPage.first}
          last={merchantPage.last}
          pageSize={merchantPage.size}
          onPageSizeChange={(size) => setPager({ page: 0, size })}
          onPrev={() => setPager((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
          onNext={() => setPager((current) => ({ ...current, page: current.page + 1 }))}
          noun="merchants"
        />
      </section>
    </div>
  )
}
