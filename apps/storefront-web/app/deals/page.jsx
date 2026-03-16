import Link from 'next/link'
import { getDeals, getActivePromotions } from '@/lib/api'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'
import DealsProductGrid from '@/components/analytics/DealsProductGrid'

export const revalidate = 60

export default async function DealsPage() {
  let deals = []
  let promotions = []

  try {
    const results = await Promise.allSettled([getDeals(), getActivePromotions()])
    deals = results[0].status === 'fulfilled' ? (Array.isArray(results[0].value) ? results[0].value : results[0].value?.items || []) : []
    promotions = results[1].status === 'fulfilled' ? (Array.isArray(results[1].value) ? results[1].value : []) : []
  } catch { /* graceful */ }

  return (
    <>
      <section className="hero-compact">
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Deals' }]} />
          <h1 style={{ color: '#fff', margin: '0 0 8px', fontSize: 'clamp(1.8rem, 3vw, 2.5rem)' }}>Deals &amp; Promotions</h1>
          <p style={{ color: 'rgba(255,255,255,0.7)', margin: 0, maxWidth: 600 }}>
            Save big with our latest offers, discounts, and exclusive promotions.
          </p>
        </div>
      </section>

      {/* Active Promotions */}
      {promotions.length > 0 && (
        <section className="featured-section" style={{ paddingTop: 32 }}>
          <div className="container">
            <h2 className="section-title">Active Promotions</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
              {promotions.map((promo, i) => (
                <div key={promo.id || i} className="promo-card panel" style={{ padding: 24 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                    <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{promo.name || promo.title || 'Special Offer'}</h3>
                    {promo.discountPercent && <Badge variant="success">{promo.discountPercent}% OFF</Badge>}
                    {promo.discountAmount && !promo.discountPercent && <Badge variant="success">${promo.discountAmount} OFF</Badge>}
                  </div>
                  {promo.description && <p style={{ color: 'var(--muted)', margin: '0 0 12px' }}>{promo.description}</p>}
                  <div style={{ display: 'flex', gap: 12, fontSize: '0.85rem', color: 'var(--muted)' }}>
                    {promo.startDate && <span>From: {new Date(promo.startDate).toLocaleDateString()}</span>}
                    {promo.endDate && <span>Until: {new Date(promo.endDate).toLocaleDateString()}</span>}
                  </div>
                  {promo.code && (
                    <div style={{ marginTop: 12, padding: '8px 14px', background: 'var(--line-light)', borderRadius: 'var(--radius-sm)', display: 'inline-block' }}>
                      <small style={{ color: 'var(--muted)' }}>Code: </small>
                      <strong style={{ letterSpacing: '0.1em' }}>{promo.code}</strong>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Deals Products */}
      <section className="featured-section" style={{ paddingBottom: 48 }}>
        <div className="container">
          <h2 className="section-title">Products on Sale</h2>
          {deals.length === 0 ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)' }}>No deals available right now. Check back soon!</p>
              <Link href="/products" className="button primary" style={{ marginTop: 12 }}>Browse All Products</Link>
            </div>
          ) : (
            <DealsProductGrid deals={deals} listName="deals-page-grid" pagePath="/deals" />
          )}
        </div>
      </section>
    </>
  )
}
