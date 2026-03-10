import { useEffect, useState } from 'react'
import { getCommerceAnalyticsOverview } from '../../shared/api/endpoints/analyticsApi'
import { formatCurrency } from '../../shared/ui/formatters'

export function CommerceAnalyticsOverviewPanel() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [overview, setOverview] = useState(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const data = await getCommerceAnalyticsOverview()
      setOverview(data)
    } catch (err) {
      setError(err.message || 'Unable to load commerce analytics overview.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <section className="panel" style={{ marginBottom: 24 }}>
      <div className="section-head">
        <div>
          <h3>Commerce event overview</h3>
          <p>Live metrics derived from the new analytics event pipeline.</p>
        </div>
        <button className="btn btn-outline btn-sm" type="button" onClick={load} disabled={loading}>Refresh</button>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}
      {loading ? <p className="muted-text">Loading event overview…</p> : null}

      {overview ? (
        <>
          <div className="card-grid">
            <article className="metric-card"><h3>Total events</h3><strong>{overview.totalEvents}</strong></article>
            <article className="metric-card"><h3>Product views</h3><strong>{overview.productViews}</strong></article>
            <article className="metric-card"><h3>Add to cart</h3><strong>{overview.addToCartCount}</strong></article>
            <article className="metric-card"><h3>Checkout started</h3><strong>{overview.checkoutStartedCount}</strong></article>
            <article className="metric-card"><h3>Checkout completed</h3><strong>{overview.checkoutCompletedCount}</strong></article>
            <article className="metric-card"><h3>Conversion rate</h3><strong>{overview.conversionRate}%</strong></article>
            <article className="metric-card"><h3>Cart abandonment</h3><strong>{overview.cartAbandonmentRate}%</strong></article>
            <article className="metric-card"><h3>Average order value</h3><strong>{formatCurrency(overview.averageOrderValue || 0)}</strong></article>
          </div>

          <div className="table-card" style={{ marginTop: 20 }}>
            <table className="data-table compact">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Source</th>
                  <th>Product</th>
                  <th>Promotion</th>
                  <th>When</th>
                </tr>
              </thead>
              <tbody>
                {(overview.recentEvents || []).slice(0, 12).map((event) => (
                  <tr key={event.id}>
                    <td>{event.eventType}</td>
                    <td>{event.source || '—'}</td>
                    <td>{event.productId || '—'}</td>
                    <td>{event.promotionCode || '—'}</td>
                    <td>{event.occurredAt ? new Date(event.occurredAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </section>
  )
}
