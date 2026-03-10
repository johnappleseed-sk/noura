import { useEffect, useMemo, useState } from 'react'
import {
  getRecommendationPreview,
  getRecommendationSettings,
  updateRecommendationSettings
} from '../shared/api/endpoints/recommendationAdminApi'

const DEFAULT_FORM = {
  productViewWeight: 1,
  addToCartWeight: 4,
  checkoutWeight: 8,
  trendingBoost: 30,
  bestSellerBoost: 20,
  ratingWeight: 5,
  categoryAffinityWeight: 6,
  brandAffinityWeight: 3,
  coPurchaseWeight: 5,
  dealBoost: 60,
  maxRecommendations: 12
}

const FIELD_GROUPS = [
  {
    title: 'Behavior signals',
    fields: [
      ['productViewWeight', 'Product view weight'],
      ['addToCartWeight', 'Add to cart weight'],
      ['checkoutWeight', 'Checkout completed weight']
    ]
  },
  {
    title: 'Ranking boosts',
    fields: [
      ['trendingBoost', 'Trending boost'],
      ['bestSellerBoost', 'Best seller boost'],
      ['ratingWeight', 'Rating weight'],
      ['dealBoost', 'Deal boost']
    ]
  },
  {
    title: 'Affinity tuning',
    fields: [
      ['categoryAffinityWeight', 'Category affinity'],
      ['brandAffinityWeight', 'Brand affinity'],
      ['coPurchaseWeight', 'Co-purchase weight'],
      ['maxRecommendations', 'Max recommendations']
    ]
  }
]

function RecommendationList({ title, items }) {
  return (
    <section className="panel" style={{ padding: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h3 style={{ margin: 0, fontSize: '1rem' }}>{title}</h3>
        <span style={{ color: 'var(--muted)' }}>{items.length} items</span>
      </div>
      {items.length === 0 ? (
        <p style={{ margin: 0, color: 'var(--muted)' }}>No recommendations available.</p>
      ) : (
        <div style={{ display: 'grid', gap: 10 }}>
          {items.map((item) => (
            <div
              key={item.id}
              style={{
                border: '1px solid var(--line)',
                borderRadius: 14,
                padding: 12,
                display: 'grid',
                gap: 4,
                background: 'var(--panel)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>{item.name}</strong>
                <span style={{ color: 'var(--muted)' }}>{Number(item.score || 0).toFixed(2)}</span>
              </div>
              <span style={{ color: 'var(--muted)' }}>{item.categoryName || 'Uncategorized'}</span>
              {item.reason && <small style={{ color: 'var(--muted)' }}>{item.reason}</small>}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export function RecommendationsPage() {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [previewFilters, setPreviewFilters] = useState({ customerRef: '', productId: '', limit: 6 })
  const [preview, setPreview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const previewParams = useMemo(() => {
    const params = { limit: Number(previewFilters.limit) || 6 }
    if (previewFilters.customerRef.trim()) params.customerRef = previewFilters.customerRef.trim()
    if (previewFilters.productId.trim()) params.productId = previewFilters.productId.trim()
    return params
  }, [previewFilters])

  useEffect(() => {
    let active = true

    async function load() {
      try {
        setLoading(true)
        setError('')
        const [settings, previewResponse] = await Promise.all([
          getRecommendationSettings(),
          getRecommendationPreview({ limit: 6 })
        ])
        if (!active) return
        setForm({ ...DEFAULT_FORM, ...settings })
        setPreview(previewResponse)
      } catch (loadError) {
        if (!active) return
        setError(loadError?.response?.data?.message || loadError?.message || 'Failed to load recommendation controls.')
      } finally {
        if (active) setLoading(false)
      }
    }

    load()
    return () => {
      active = false
    }
  }, [])

  async function handleSave(event) {
    event.preventDefault()
    try {
      setSaving(true)
      setError('')
      setSuccess('')
      const payload = Object.fromEntries(
        Object.entries(form).map(([key, value]) => [key, key === 'maxRecommendations' ? Number(value) : Number(value)])
      )
      const saved = await updateRecommendationSettings(payload)
      setForm({ ...DEFAULT_FORM, ...saved })
      setSuccess('Recommendation settings saved.')
      const refreshedPreview = await getRecommendationPreview(previewParams)
      setPreview(refreshedPreview)
    } catch (saveError) {
      setError(saveError?.response?.data?.message || saveError?.message || 'Failed to save recommendation settings.')
    } finally {
      setSaving(false)
    }
  }

  async function handleRefreshPreview() {
    try {
      setRefreshing(true)
      setError('')
      const response = await getRecommendationPreview(previewParams)
      setPreview(response)
    } catch (previewError) {
      setError(previewError?.response?.data?.message || previewError?.message || 'Failed to refresh preview.')
    } finally {
      setRefreshing(false)
    }
  }

  if (loading) {
    return <div className="page-shell"><div className="panel" style={{ padding: 24 }}>Loading recommendation controls...</div></div>
  }

  return (
    <div className="page-shell" style={{ display: 'grid', gap: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ margin: 0 }}>Recommendation Controls</h1>
          <p style={{ margin: '8px 0 0', color: 'var(--muted)', maxWidth: 760 }}>
            Tune ranking signals, preview recommendation outputs, and keep storefront recommendation behavior aligned with merchandizing intent.
          </p>
        </div>
      </div>

      {(error || success) && (
        <div
          className="panel"
          style={{
            padding: 16,
            borderColor: error ? 'var(--danger)' : 'var(--success)',
            color: error ? 'var(--danger)' : 'var(--success)'
          }}
        >
          {error || success}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 420px) minmax(0, 1fr)', gap: 24, alignItems: 'start' }}>
        <form className="panel" style={{ padding: 20, display: 'grid', gap: 20 }} onSubmit={handleSave}>
          {FIELD_GROUPS.map((group) => (
            <section key={group.title} style={{ display: 'grid', gap: 12 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: '1rem' }}>{group.title}</h2>
              </div>
              <div style={{ display: 'grid', gap: 12 }}>
                {group.fields.map(([field, label]) => (
                  <label key={field} style={{ display: 'grid', gap: 6 }}>
                    <span style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>{label}</span>
                    <input
                      type="number"
                      min={field === 'maxRecommendations' ? 1 : 0}
                      max={field === 'maxRecommendations' ? 24 : undefined}
                      step={field === 'maxRecommendations' ? 1 : 0.1}
                      value={form[field]}
                      onChange={(event) => setForm((current) => ({ ...current, [field]: event.target.value }))}
                      style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }}
                    />
                  </label>
                ))}
              </div>
            </section>
          ))}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save settings'}
            </button>
          </div>
        </form>

        <div style={{ display: 'grid', gap: 20 }}>
          <section className="panel" style={{ padding: 20, display: 'grid', gap: 16 }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem' }}>Preview filters</h2>
              <p style={{ margin: '6px 0 0', color: 'var(--muted)' }}>
                Preview generic, product-driven, or customer-driven recommendations without touching storefront traffic.
              </p>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
              <label style={{ display: 'grid', gap: 6 }}>
                <span style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>Customer ref</span>
                <input
                  value={previewFilters.customerRef}
                  onChange={(event) => setPreviewFilters((current) => ({ ...current, customerRef: event.target.value }))}
                  placeholder="customer@example.com"
                  style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }}
                />
              </label>
              <label style={{ display: 'grid', gap: 6 }}>
                <span style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>Product id</span>
                <input
                  value={previewFilters.productId}
                  onChange={(event) => setPreviewFilters((current) => ({ ...current, productId: event.target.value }))}
                  placeholder="UUID for bundle preview"
                  style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }}
                />
              </label>
              <label style={{ display: 'grid', gap: 6 }}>
                <span style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>Limit</span>
                <input
                  type="number"
                  min="1"
                  max={form.maxRecommendations || 24}
                  value={previewFilters.limit}
                  onChange={(event) => setPreviewFilters((current) => ({ ...current, limit: event.target.value }))}
                  style={{ padding: '10px 12px', borderRadius: 10, border: '1px solid var(--line)' }}
                />
              </label>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="button" className="btn" onClick={handleRefreshPreview} disabled={refreshing}>
                {refreshing ? 'Refreshing...' : 'Refresh preview'}
              </button>
            </div>
          </section>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 20 }}>
            <RecommendationList title="Trending" items={preview?.trending || []} />
            <RecommendationList title="Best sellers" items={preview?.bestSellers || []} />
            <RecommendationList title="Deals" items={preview?.deals || []} />
            <RecommendationList title="Personalized" items={preview?.personalized || []} />
            <RecommendationList title="Cross-sell" items={preview?.crossSell || []} />
            <RecommendationList title="Related products" items={preview?.productPreview?.relatedProducts || []} />
            <RecommendationList title="Frequently bought together" items={preview?.productPreview?.frequentlyBoughtTogether || []} />
          </div>
        </div>
      </div>
    </div>
  )
}
