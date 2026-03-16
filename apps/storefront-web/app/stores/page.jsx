import Link from 'next/link'
import { getStores } from '@/lib/api'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'

export const revalidate = 60

export default async function StoresPage() {
  let stores = []
  try {
    const data = await getStores()
    stores = Array.isArray(data) ? data : data?.items || data?.content || []
  } catch { /* graceful */ }

  return (
    <>
      <section className="hero-compact">
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Stores' }]} />
          <h1 style={{ color: '#fff', margin: '0 0 8px', fontSize: 'clamp(1.8rem, 3vw, 2.5rem)' }}>Our Stores</h1>
          <p style={{ color: 'rgba(255,255,255,0.7)', margin: 0, maxWidth: 600 }}>
            Find a Noura store near you. Visit us for an in-person shopping experience.
          </p>
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container">
          {stores.length === 0 ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <h2 style={{ marginBottom: 8 }}>No Stores Available</h2>
              <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Store information is not available at this time.</p>
              <Link href="/products" className="button primary">Shop Online</Link>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: 20 }}>
              {stores.map((store) => (
                <div key={store.id} className="store-card panel" style={{ padding: 24 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                    <h3 style={{ margin: 0 }}>{store.name}</h3>
                    {store.status && (
                      <Badge variant={store.status === 'OPEN' || store.status === 'ACTIVE' ? 'success' : 'neutral'}>
                        {store.status}
                      </Badge>
                    )}
                  </div>

                  {store.description && <p style={{ color: 'var(--muted)', margin: '0 0 12px' }}>{store.description}</p>}

                  <div style={{ display: 'grid', gap: 8, fontSize: '0.9rem' }}>
                    {(store.address || store.line1) && (
                      <div style={{ display: 'flex', gap: 8 }}>
                        <span style={{ color: 'var(--muted)' }}>Address:</span>
                        <span>{store.address || [store.line1, store.city, store.stateProvince, store.countryCode].filter(Boolean).join(', ')}</span>
                      </div>
                    )}
                    {store.phone && (
                      <div style={{ display: 'flex', gap: 8 }}>
                        <span style={{ color: 'var(--muted)' }}>Phone:</span>
                        <span>{store.phone}</span>
                      </div>
                    )}
                    {store.email && (
                      <div style={{ display: 'flex', gap: 8 }}>
                        <span style={{ color: 'var(--muted)' }}>Email:</span>
                        <span>{store.email}</span>
                      </div>
                    )}
                    {store.operatingHours && (
                      <div style={{ display: 'flex', gap: 8 }}>
                        <span style={{ color: 'var(--muted)' }}>Hours:</span>
                        <span>{store.operatingHours}</span>
                      </div>
                    )}
                  </div>

                  {(store.latitude && store.longitude) && (
                    <div style={{ marginTop: 12 }}>
                      <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
                        Coordinates: {store.latitude}, {store.longitude}
                      </span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  )
}
