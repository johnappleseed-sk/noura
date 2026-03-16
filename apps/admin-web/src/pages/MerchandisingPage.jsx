import { useEffect, useMemo, useRef, useState } from 'react'
import {
  createMerchandisingBoost,
  deleteMerchandisingBoost,
  getMerchandisingPreview,
  getMerchandisingSettings,
  listMerchandisingBoosts,
  updateMerchandisingSettings
} from '../shared/api/endpoints/merchandisingAdminApi'
import { useConfirmDialog } from '../shared/ui/ConfirmDialogProvider'
import { useToastFeedback } from '../shared/ui/useToastFeedback'

const SETTING_FIELDS = [
  { key: 'popularityWeight', label: 'Popularity weight', step: 0.1 },
  { key: 'inventoryWeight', label: 'Inventory weight', step: 0.1 },
  { key: 'impressionWeight', label: 'Impression weight', step: 0.1 },
  { key: 'clickWeight', label: 'Click weight', step: 0.1 },
  { key: 'clickThroughRateWeight', label: 'CTR weight', step: 0.1 },
  { key: 'manualBoostWeight', label: 'Manual boost weight', step: 0.1 },
  { key: 'newArrivalWindowDays', label: 'New arrival window days', step: 1 },
  { key: 'newArrivalBoost', label: 'New arrival boost', step: 0.1 },
  { key: 'trendingBoost', label: 'Trending boost', step: 0.1 },
  { key: 'bestSellerBoost', label: 'Best seller boost', step: 0.1 },
  { key: 'lowStockPenalty', label: 'Low stock penalty', step: 0.1 },
  { key: 'maxPageSize', label: 'Max page size', step: 1 }
]

const DEFAULT_SETTINGS = {
  popularityWeight: 1,
  inventoryWeight: 0.5,
  impressionWeight: 0.75,
  clickWeight: 4,
  clickThroughRateWeight: 0.6,
  manualBoostWeight: 1,
  newArrivalWindowDays: 30,
  newArrivalBoost: 25,
  trendingBoost: 20,
  bestSellerBoost: 15,
  lowStockPenalty: 20,
  maxPageSize: 48
}

function createDefaultBoost() {
  return {
    productId: '',
    label: '',
    boostValue: 10,
    active: true,
    startAt: '',
    endAt: ''
  }
}

const PAGE_GAP = 24

const styles = {
  page: {
    display: 'grid',
    gap: PAGE_GAP
  },
  stackedSection: {
    display: 'grid',
    gap: 20
  },
  twoColumnLayout: {
    display: 'grid',
    gridTemplateColumns: 'minmax(320px, 420px) minmax(0, 1fr)',
    gap: 24,
    alignItems: 'start'
  },
  panel: {
    padding: 20,
    display: 'grid',
    gap: 12
  },
  compactPanel: {
    padding: 18
  },
  loadingPanel: {
    padding: 24
  },
  form: {
    padding: 20,
    display: 'grid',
    gap: 14
  },
  input: {
    padding: '10px 12px',
    borderRadius: 10,
    border: '1px solid var(--line)',
    width: '100%',
    minWidth: 0
  },
  field: {
    display: 'grid',
    gap: 6
  },
  fieldLabel: {
    fontSize: '0.9rem',
    color: 'var(--muted)'
  },
  sectionTitle: {
    margin: 0,
    fontSize: '1rem'
  },
  mutedText: {
    color: 'var(--muted)'
  },
  headerText: {
    margin: '8px 0 0',
    color: 'var(--muted)',
    maxWidth: 760
  },
  actionRow: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  listGrid: {
    display: 'grid',
    gap: 10
  },
  previewFilterGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: 12
  },
  previewCardsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
    gap: 20
  },
  inlineMetaRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12
  },
  card: {
    border: '1px solid var(--line)',
    borderRadius: 12,
    padding: 12
  },
  productCard: {
    border: '1px solid var(--line)',
    borderRadius: 12,
    padding: 12,
    display: 'grid',
    gap: 4
  },
  boostRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 12,
    border: '1px solid var(--line)',
    borderRadius: 12,
    padding: 12,
    flexWrap: 'wrap'
  },
  boostContent: {
    display: 'grid',
    gap: 4,
    minWidth: 0,
    flex: '1 1 260px'
  },
  dateGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: 12
  },
  checkboxRow: {
    display: 'flex',
    gap: 8,
    alignItems: 'center',
    color: 'var(--muted)'
  }
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function toFiniteNumber(value, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function toPositiveInteger(value, fallback = 1) {
  const parsed = Math.floor(Number(value))
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function toIsoStringOrNull(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

function isValidBoostWindow(startAt, endAt) {
  if (!startAt || !endAt) return true
  const startDate = new Date(startAt)
  const endDate = new Date(endAt)
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return false
  return endDate > startDate
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString()
}

function formatScore(value) {
  return toFiniteNumber(value, 0).toFixed(2)
}

function ProductList({ title, items }) {
  const safeItems = Array.isArray(items) ? items : []

  return (
    <section className="panel" style={{ ...styles.compactPanel }}>
      <div style={styles.inlineMetaRow}>
        <h3 style={styles.sectionTitle}>{title}</h3>
        <span style={styles.mutedText} aria-label={`${safeItems.length} products`}>
          {safeItems.length}
        </span>
      </div>

      {safeItems.length === 0 ? (
        <p style={{ margin: 0, ...styles.mutedText }}>No products ranked for this view.</p>
      ) : (
        <div style={styles.listGrid}>
          {safeItems.map((item) => (
            <div key={item.id} style={styles.productCard}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
                  {item?.name || 'Unnamed product'}
                </strong>
                <span style={styles.mutedText}>{formatScore(item?.merchandisingScore)}</span>
              </div>

              <small style={styles.mutedText}>{item?.categoryName || 'Uncategorized'}</small>

              <small style={styles.mutedText}>
                Stock {toFiniteNumber(item?.stockQty, 0)} · {item?.isTrending ? 'Trending' : 'Standard'} ·{' '}
                {item?.isNew ? 'New' : 'Established'}
              </small>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export function MerchandisingPage() {
  const confirm = useConfirmDialog()
  const [settings, setSettings] = useState(DEFAULT_SETTINGS)
  const [boosts, setBoosts] = useState([])
  const [preview, setPreview] = useState(null)
  const [boostForm, setBoostForm] = useState(createDefaultBoost)
  const [filters, setFilters] = useState({ query: '', categoryId: '', storeId: '', limit: 8 })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [creatingBoost, setCreatingBoost] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useToastFeedback({ successMessage: success, errorMessage: error })

  const isMountedRef = useRef(false)

  const maxPageSize = useMemo(() => {
    return Math.max(1, toPositiveInteger(settings.maxPageSize, DEFAULT_SETTINGS.maxPageSize))
  }, [settings.maxPageSize])

  const previewParams = useMemo(() => {
    const params = {
      limit: clamp(toPositiveInteger(filters.limit, 8), 1, maxPageSize)
    }

    if (filters.query.trim()) params.query = filters.query.trim()
    if (filters.categoryId.trim()) params.categoryId = filters.categoryId.trim()
    if (filters.storeId.trim()) params.storeId = filters.storeId.trim()

    return params
  }, [filters, maxPageSize])

  function clearMessages() {
    setError('')
    setSuccess('')
  }

  function setErrorMessage(message) {
    setError(message)
    setSuccess('')
  }

  function setSuccessMessage(message) {
    setSuccess(message)
    setError('')
  }

  useEffect(() => {
    isMountedRef.current = true

    async function load() {
      try {
        setLoading(true)

        const [settingsResponse, boostsResponse, previewResponse] = await Promise.all([
          getMerchandisingSettings(),
          listMerchandisingBoosts(),
          getMerchandisingPreview({ limit: 8 })
        ])

        if (!isMountedRef.current) return

        setSettings({ ...DEFAULT_SETTINGS, ...(settingsResponse || {}) })
        setBoosts(Array.isArray(boostsResponse) ? boostsResponse : [])
        setPreview(previewResponse || null)
      } catch (loadError) {
        if (!isMountedRef.current) return
        setErrorMessage(getErrorMessage(loadError, 'Failed to load merchandising controls.'))
      } finally {
        if (isMountedRef.current) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      isMountedRef.current = false
    }
  }, [])

  async function refreshPreview() {
    try {
      setRefreshing(true)
      setError('')

      const nextPreview = await getMerchandisingPreview(previewParams)

      if (!isMountedRef.current) return

      setPreview(nextPreview || null)
    } catch (previewError) {
      if (!isMountedRef.current) return
      setErrorMessage(getErrorMessage(previewError, 'Failed to refresh preview.'))
    } finally {
      if (isMountedRef.current) {
        setRefreshing(false)
      }
    }
  }

  async function handleSaveSettings(event) {
    event.preventDefault()

    try {
      setSaving(true)
      clearMessages()

      const payload = Object.fromEntries(
        Object.entries(settings).map(([key, value]) => [key, toFiniteNumber(value, DEFAULT_SETTINGS[key])])
      )

      const response = await updateMerchandisingSettings(payload)

      if (!isMountedRef.current) return

      setSettings({ ...DEFAULT_SETTINGS, ...(response || {}) })
      setSuccessMessage('Merchandising settings saved.')

      await refreshPreview()
    } catch (saveError) {
      if (!isMountedRef.current) return
      setErrorMessage(getErrorMessage(saveError, 'Failed to save merchandising settings.'))
    } finally {
      if (isMountedRef.current) {
        setSaving(false)
      }
    }
  }

  async function handleCreateBoost(event) {
    event.preventDefault()

    try {
      setCreatingBoost(true)
      clearMessages()

      const productId = boostForm.productId.trim()
      const label = boostForm.label.trim()

      if (!productId) {
        throw new Error('Product UUID is required.')
      }
      if (!label) {
        throw new Error('Campaign label is required.')
      }
      if (!isValidBoostWindow(boostForm.startAt, boostForm.endAt)) {
        throw new Error('Boost end date must be after start date.')
      }

      const payload = {
        productId,
        label,
        boostValue: toFiniteNumber(boostForm.boostValue, 0),
        active: Boolean(boostForm.active),
        startAt: toIsoStringOrNull(boostForm.startAt),
        endAt: toIsoStringOrNull(boostForm.endAt)
      }

      await createMerchandisingBoost(payload)

      if (!isMountedRef.current) return

      const nextBoosts = await listMerchandisingBoosts()

      if (!isMountedRef.current) return

      setBoostForm(createDefaultBoost())
      setBoosts(Array.isArray(nextBoosts) ? nextBoosts : [])
      setSuccessMessage('Merchandising boost created.')

      await refreshPreview()
    } catch (boostError) {
      if (!isMountedRef.current) return
      setErrorMessage(getErrorMessage(boostError, 'Failed to create merchandising boost.'))
    } finally {
      if (isMountedRef.current) {
        setCreatingBoost(false)
      }
    }
  }

  async function handleDeleteBoost(boostId) {
    try {
      clearMessages()
      const confirmed = await confirm({
        title: 'Move merchandising boost to trash?',
        message: 'This will move the manual boost to trash and remove it from the active merchandising strategy.',
        confirmLabel: 'Move to trash'
      })

      if (!confirmed) {
        return
      }

      await deleteMerchandisingBoost(boostId)

      if (!isMountedRef.current) return

      const nextBoosts = await listMerchandisingBoosts()

      if (!isMountedRef.current) return

      setBoosts(Array.isArray(nextBoosts) ? nextBoosts : [])
      setSuccessMessage('Merchandising boost moved to trash.')

      await refreshPreview()
    } catch (deleteError) {
      if (!isMountedRef.current) return
      setErrorMessage(getErrorMessage(deleteError, 'Failed to move merchandising boost to trash.'))
    }
  }

  if (loading) {
    return (
      <div className="page-shell">
        <div className="panel" style={styles.loadingPanel}>
          Loading merchandising engine...
        </div>
      </div>
    )
  }

  return (
    <div className="page-shell" style={styles.page}>
      <div>
        <h1 style={{ margin: 0 }}>Merchandising Engine</h1>
        <p style={styles.headerText}>
          Tune ranking signals, schedule manual boosts, and preview how the storefront catalog will rank
          products using direct impression and click behavior instead of sales-only proxies.
        </p>
      </div>

      <div style={styles.twoColumnLayout}>
        <div style={styles.stackedSection}>
          <form className="panel" style={styles.form} onSubmit={handleSaveSettings}>
            <h2 style={styles.sectionTitle}>Ranking weights</h2>

            {SETTING_FIELDS.map((field) => {
              const inputId = `setting-${field.key}`

              return (
                <label key={field.key} htmlFor={inputId} style={styles.field}>
                  <span style={styles.fieldLabel}>{field.label}</span>
                  <input
                    id={inputId}
                    type="number"
                    min="0"
                    step={field.step}
                    value={settings[field.key]}
                    onChange={(event) =>
                      setSettings((current) => ({
                        ...current,
                        [field.key]: event.target.value
                      }))
                    }
                    style={styles.input}
                  />
                </label>
              )
            })}

            <div style={styles.actionRow}>
              <button type="submit" className="btn btn-primary" disabled={saving} aria-busy={saving}>
                {saving ? 'Saving...' : 'Save settings'}
              </button>
            </div>
          </form>

          <form className="panel" style={styles.panel} onSubmit={handleCreateBoost}>
            <h2 style={styles.sectionTitle}>Manual boost</h2>

            <label htmlFor="boost-product-id" style={styles.field}>
              <span style={styles.fieldLabel}>Product UUID</span>
              <input
                id="boost-product-id"
                value={boostForm.productId}
                onChange={(event) =>
                  setBoostForm((current) => ({
                    ...current,
                    productId: event.target.value
                  }))
                }
                placeholder="Product UUID"
                style={styles.input}
              />
            </label>

            <label htmlFor="boost-label" style={styles.field}>
              <span style={styles.fieldLabel}>Campaign label</span>
              <input
                id="boost-label"
                value={boostForm.label}
                onChange={(event) =>
                  setBoostForm((current) => ({
                    ...current,
                    label: event.target.value
                  }))
                }
                placeholder="Campaign label"
                style={styles.input}
              />
            </label>

            <label htmlFor="boost-value" style={styles.field}>
              <span style={styles.fieldLabel}>Boost value</span>
              <input
                id="boost-value"
                type="number"
                min="0"
                step="0.1"
                value={boostForm.boostValue}
                onChange={(event) =>
                  setBoostForm((current) => ({
                    ...current,
                    boostValue: event.target.value
                  }))
                }
                placeholder="Boost value"
                style={styles.input}
              />
            </label>

            <div style={styles.dateGrid}>
              <label htmlFor="boost-start-at" style={styles.field}>
                <span style={styles.fieldLabel}>Start at</span>
                <input
                  id="boost-start-at"
                  type="datetime-local"
                  value={boostForm.startAt}
                  onChange={(event) =>
                    setBoostForm((current) => ({
                      ...current,
                      startAt: event.target.value
                    }))
                  }
                  style={styles.input}
                />
              </label>

              <label htmlFor="boost-end-at" style={styles.field}>
                <span style={styles.fieldLabel}>End at</span>
                <input
                  id="boost-end-at"
                  type="datetime-local"
                  value={boostForm.endAt}
                  onChange={(event) =>
                    setBoostForm((current) => ({
                      ...current,
                      endAt: event.target.value
                    }))
                  }
                  style={styles.input}
                />
              </label>
            </div>

            <label style={styles.checkboxRow}>
              <input
                type="checkbox"
                checked={boostForm.active}
                onChange={(event) =>
                  setBoostForm((current) => ({
                    ...current,
                    active: event.target.checked
                  }))
                }
              />
              Active immediately
            </label>

            <div style={styles.actionRow}>
              <button type="submit" className="btn" disabled={creatingBoost} aria-busy={creatingBoost}>
                {creatingBoost ? 'Creating...' : 'Create boost'}
              </button>
            </div>
          </form>
        </div>

        <div style={styles.stackedSection}>
          <section className="panel" style={styles.panel}>
            <div>
              <h2 style={styles.sectionTitle}>Preview filters</h2>
              <p style={{ margin: '6px 0 0', ...styles.mutedText }}>
                Use the same filters the storefront catalog would send.
              </p>
            </div>

            <div style={styles.previewFilterGrid}>
              <label htmlFor="preview-query" style={styles.field}>
                <span style={styles.fieldLabel}>Search query</span>
                <input
                  id="preview-query"
                  value={filters.query}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      query: event.target.value
                    }))
                  }
                  placeholder="Search query"
                  style={styles.input}
                />
              </label>

              <label htmlFor="preview-category-id" style={styles.field}>
                <span style={styles.fieldLabel}>Category UUID</span>
                <input
                  id="preview-category-id"
                  value={filters.categoryId}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      categoryId: event.target.value
                    }))
                  }
                  placeholder="Category UUID"
                  style={styles.input}
                />
              </label>

              <label htmlFor="preview-store-id" style={styles.field}>
                <span style={styles.fieldLabel}>Store UUID</span>
                <input
                  id="preview-store-id"
                  value={filters.storeId}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      storeId: event.target.value
                    }))
                  }
                  placeholder="Store UUID"
                  style={styles.input}
                />
              </label>

              <label htmlFor="preview-limit" style={styles.field}>
                <span style={styles.fieldLabel}>Limit</span>
                <input
                  id="preview-limit"
                  type="number"
                  min="1"
                  max={maxPageSize}
                  value={filters.limit}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      limit: event.target.value
                    }))
                  }
                  style={styles.input}
                />
              </label>
            </div>

            <div style={styles.actionRow}>
              <button
                type="button"
                className="btn"
                disabled={refreshing}
                aria-busy={refreshing}
                onClick={refreshPreview}
              >
                {refreshing ? 'Refreshing...' : 'Refresh preview'}
              </button>
            </div>
          </section>

          <section className="panel" style={styles.panel}>
            <h2 style={styles.sectionTitle}>Active boosts</h2>

            {boosts.length === 0 ? (
              <p style={{ margin: 0, ...styles.mutedText }}>No manual boosts configured.</p>
            ) : (
              boosts.map((boost) => (
                <div key={boost.id} style={styles.boostRow}>
                  <div style={styles.boostContent}>
                    <strong style={{ overflowWrap: 'anywhere' }}>{boost.label}</strong>
                    <small style={styles.mutedText}>
                      {boost.productName} · {boost.boostValue}
                    </small>
                    <small style={styles.mutedText}>
                      {boost.active ? 'Active' : 'Inactive'}
                      {boost.startAt ? ` · starts ${formatDateTime(boost.startAt)}` : ''}
                      {boost.endAt ? ` · ends ${formatDateTime(boost.endAt)}` : ''}
                    </small>
                  </div>

                  <button type="button" className="btn" onClick={() => handleDeleteBoost(boost.id)}>
                    Move to trash
                  </button>
                </div>
              ))
            )}
          </section>

          <div style={styles.previewCardsGrid}>
            <ProductList title="Featured" items={preview?.featured || []} />
            <ProductList title="Popularity" items={preview?.popularity || []} />
            <ProductList title="Trending" items={preview?.trending || []} />
            <ProductList title="Best selling" items={preview?.bestSelling || []} />
            <ProductList title="New arrivals" items={preview?.newest || []} />
          </div>
        </div>
      </div>
    </div>
  )
}
