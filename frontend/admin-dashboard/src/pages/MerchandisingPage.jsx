import { useEffect, useMemo, useState } from 'react'
import {
  createMerchandisingBoost,
  deleteMerchandisingBoost,
  getMerchandisingPreview,
  getMerchandisingSettings,
  listMerchandisingBoosts,
  updateMerchandisingSettings
} from '../shared/api/endpoints/merchandisingAdminApi'

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

const DEFAULT_BOOST = {
  productId: '',
  label: '',
  boostValue: 10,
  active: true,
  startAt: '',
  endAt: ''
}

function ProductList({ title, items }) {
  return (
    <section className="panel" style={{ padding: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h3 style={{ margin: 0, fontSize: '1rem' }}>{title}</h3>
        <span style={{ color: 'var(--muted)' }}>{items.length}</span>
      </div>
      {items.length === 0 ? (
        <p style={{ margin: 0, color: 'var(--muted)' }}>No products ranked for this view.</p>
      ) : (
        <div style={{ display: 'grid', gap: 10 }}>
          {items.map((item) => (
            <div key={item.id} style={{ border: '1px solid var(--line)', borderRadius: 12, padding: 12, display: 'grid', gap: 4 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>{item.name}</strong>
                <span style={{ color: 'var(--muted)' }}>{Number(item.merchandisingScore || 0).toFixed(2)}</span>
              </div>
              <small style={{ color: 'var(--muted)' }}>{item.categoryName || 'Uncategorized'}</small>
              <small style={{ color: 'var(--muted)' }}>
                Stock {item.stockQty} · {item.isTrending ? 'Trending' : 'Standard'} · {item.isNew ? 'New' : 'Established'}
              </small>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export function MerchandisingPage() {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS)
  const [boosts, setBoosts] = useState([])
  const [preview, setPreview] = useState(null)
  const [boostForm, setBoostForm] = useState(DEFAULT_BOOST)
  const [filters, setFilters] = useState({ query: '', categoryId: '', storeId: '', limit: 8 })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [creatingBoost, setCreatingBoost] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const previewParams = useMemo(() => {
    const params = { limit: Number(filters.limit) || 8 }
    if (filters.query.trim()) params.query = filters.query.trim()
    if (filters.categoryId.trim()) params.categoryId = filters.categoryId.trim()
    if (filters.storeId.trim()) params.storeId = filters.storeId.trim()
    return params
  }, [filters])

  useEffect(() => {
    let active = true

    async function load() {
      try {
        setLoading(true)
        const [settingsResponse, boostsResponse, previewResponse] = await Promise.all([
          getMerchandisingSettings(),
          listMerchandisingBoosts(),
          getMerchandisingPreview({ limit: 8 })
        ])
        if (!active) return
        setSettings({ ...DEFAULT_SETTINGS, ...settingsResponse })
        setBoosts(boostsResponse || [])
        setPreview(previewResponse)
      } catch (loadError) {
        if (!active) return
        setError(loadError?.response?.data?.message || loadError?.message || 'Failed to load merchandising controls.')
      } finally {
        if (active) setLoading(false)
      }
    }

    load()
    return () => {
      active = false
    }
  }, [])

  async function refreshPreview() {
    try {
      setRefreshing(true)
      setError('')
      setPreview(await getMerchandisingPreview(previewParams))
    } catch (previewError) {
      setError(previewError?.response?.data?.message || previewError?.message || 'Failed to refresh preview.')
    } finally {
      setRefreshing(false)
    }
  }

  async function handleSaveSettings(event) {
    event.preventDefault()
    try {
      setSaving(true)
      setError('')
      setSuccess('')
      const payload = Object.fromEntries(Object.entries(settings).map(([key, value]) => [key, Number(value)]))
      const response = await updateMerchandisingSettings(payload)
      setSettings({ ...DEFAULT_SETTINGS, ...response })
      setSuccess('Merchandising settings saved.')
      await refreshPreview()
    } catch (saveError) {
      setError(saveError?.response?.data?.message || saveError?.message || 'Failed to save merchandising settings.')
    } finally {
      setSaving(false)
    }
  }

  async function handleCreateBoost(event) {
    event.preventDefault()
    try {
      setCreatingBoost(true)
      setError('')
      setSuccess('')
      const payload = {
        productId: boostForm.productId.trim(),
        label: boostForm.label.trim(),
        boostValue: Number(boostForm.boostValue),
        active: Boolean(boostForm.active),
        startAt: boostForm.startAt ? new Date(boostForm.startAt).toISOString() : null,
        endAt: boostForm.endAt ? new Date(boostForm.endAt).toISOString() : null
      }
      await createMerchandisingBoost(payload)
      setBoostForm(DEFAULT_BOOST)
      setBoosts(await listMerchandisingBoosts())
      setSuccess('Merchandising boost created.')
      await refreshPreview()
    } catch (boostError) {
      setError(boostError?.response?.data?.message || boostError?.message || 'Failed to create merchandising boost.')
    } finally {
      setCreatingBoost(false)
    }
  }

  async function handleDeleteBoost(boostId) {
    try {
      setError('')
      setSuccess('')
      await deleteMerchandisingBoost(boostId)
      setBoosts(await listMerchandisingBoosts())
      setSuccess('Merchandising boost deleted.')
      await refreshPreview()
    } catch (deleteError) {
      setError(deleteError?.response?.data?.message || deleteError?.message || 'Failed to delete merchandising boost.')
    }
  }

  if (loading) {
    return <div className="page-shell"><div className="panel" style={{ padding: 24 }}>Loading merchandising engine...</div></div>
  }

  return (
    <div className="page-shell" style={{ display: 'grid', gap: 24 }}>
      <div>
        <h1 style={{ margin: 0 }}>Merchandising Engine</h1>
        <p style={{ margin: '8px 0 0', color: 'var(--muted)', maxWidth: 760 }}>
          Tune ranking signals, schedule manual boosts, and preview how the storefront catalog will rank products using direct impression and click behavior instead of sales-only proxies.
        </p>
      </div>

      {(error || success) && (
        <div className="panel" style={{ padding: 16, color: error ? 'var(--danger)' : 'var(--success)', borderColor: error ? 'var(--danger)' : 'var(--success)' }}>
          {error || success}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 420px) minmax(0, 1fr)', gap: 24, alignItems: 'start' }}>
        <div style={{ display: 'grid', gap: 20 }}>
          <form className="panel" style={{ padding: 20, display: 'grid', gap: 14 }} onSubmit={handleSaveSettings}>
            <h2 style={{ margin: 0, fontSize: '1rem' }}>Ranking weights</h2>
            {SETTING_FIELDS.map((field) => (
              <label key={field.key} style={{ display: 'grid', gap: 6 }}>
                <span style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>{field.label}</span>
                <input
                  type="number"
                  min="0"
                  step={field.step}
                  value={settings[field.key]}
                  onChange={(event) => setSettings((current) => ({ ...current, [field.key]: event.target.value }))}
                  style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }}
                />
              </label>
            ))}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save settings'}</button>
            </div>
          </form>

          <form className="panel" style={{ padding: 20, display: 'grid', gap: 12 }} onSubmit={handleCreateBoost}>
            <h2 style={{ margin: 0, fontSize: '1rem' }}>Manual boost</h2>
            <input value={boostForm.productId} onChange={(event) => setBoostForm((current) => ({ ...current, productId: event.target.value }))} placeholder="Product UUID" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
            <input value={boostForm.label} onChange={(event) => setBoostForm((current) => ({ ...current, label: event.target.value }))} placeholder="Campaign label" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
            <input type="number" min="0" step="0.1" value={boostForm.boostValue} onChange={(event) => setBoostForm((current) => ({ ...current, boostValue: event.target.value }))} placeholder="Boost value" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <input type="datetime-local" value={boostForm.startAt} onChange={(event) => setBoostForm((current) => ({ ...current, startAt: event.target.value }))} style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
              <input type="datetime-local" value={boostForm.endAt} onChange={(event) => setBoostForm((current) => ({ ...current, endAt: event.target.value }))} style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
            </div>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'var(--muted)' }}>
              <input type="checkbox" checked={boostForm.active} onChange={(event) => setBoostForm((current) => ({ ...current, active: event.target.checked }))} />
              Active immediately
            </label>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="submit" className="btn" disabled={creatingBoost}>{creatingBoost ? 'Creating...' : 'Create boost'}</button>
            </div>
          </form>
        </div>

        <div style={{ display: 'grid', gap: 20 }}>
          <section className="panel" style={{ padding: 20, display: 'grid', gap: 12 }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem' }}>Preview filters</h2>
              <p style={{ margin: '6px 0 0', color: 'var(--muted)' }}>Use the same filters the storefront catalog would send.</p>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
              <input value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))} placeholder="Search query" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
              <input value={filters.categoryId} onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))} placeholder="Category UUID" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
              <input value={filters.storeId} onChange={(event) => setFilters((current) => ({ ...current, storeId: event.target.value }))} placeholder="Store UUID" style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
              <input type="number" min="1" max={settings.maxPageSize || 48} value={filters.limit} onChange={(event) => setFilters((current) => ({ ...current, limit: event.target.value }))} style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="button" className="btn" disabled={refreshing} onClick={refreshPreview}>{refreshing ? 'Refreshing...' : 'Refresh preview'}</button>
            </div>
          </section>

          <section className="panel" style={{ padding: 20, display: 'grid', gap: 10 }}>
            <h2 style={{ margin: 0, fontSize: '1rem' }}>Active boosts</h2>
            {boosts.length === 0 ? (
              <p style={{ margin: 0, color: 'var(--muted)' }}>No manual boosts configured.</p>
            ) : (
              boosts.map((boost) => (
                <div key={boost.id} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, border: '1px solid var(--line)', borderRadius: 12, padding: 12 }}>
                  <div style={{ display: 'grid', gap: 4 }}>
                    <strong>{boost.label}</strong>
                    <small style={{ color: 'var(--muted)' }}>{boost.productName} · {boost.boostValue}</small>
                    <small style={{ color: 'var(--muted)' }}>
                      {boost.active ? 'Active' : 'Inactive'}
                      {boost.startAt ? ` · starts ${new Date(boost.startAt).toLocaleString()}` : ''}
                      {boost.endAt ? ` · ends ${new Date(boost.endAt).toLocaleString()}` : ''}
                    </small>
                  </div>
                  <button type="button" className="btn" onClick={() => handleDeleteBoost(boost.id)}>Delete</button>
                </div>
              ))
            )}
          </section>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 20 }}>
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
