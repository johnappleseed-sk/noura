import Skeleton from '@/components/ui/Skeleton'

export default function ProductsLoading() {
  return (
    <>
      <section className="hero-compact">
        <div className="container">
          <div className="skeleton skeleton-text" style={{ width: 180, height: 14, marginBottom: 12 }} />
          <div className="skeleton skeleton-text" style={{ width: 320, height: 34 }} />
          <div className="skeleton skeleton-text" style={{ width: 280, height: 16, marginTop: 10 }} />
        </div>
      </section>

      <section className="featured-section catalog-section">
        <div className="catalog-layout">
          <aside className="catalog-sidebar">
            <div className="panel sidebar-panel">
              <Skeleton variant="text" width="40%" height={14} />
              <div style={{ display: 'grid', gap: 10, marginTop: 12 }}>
                <Skeleton variant="rect" height={42} />
                <Skeleton variant="rect" height={42} />
                <Skeleton variant="rect" height={42} />
              </div>
            </div>
          </aside>

          <div style={{ display: 'grid', gap: 24 }}>
            <div className="product-grid catalog-product-grid">
              <Skeleton variant="product-card" count={12} />
            </div>
          </div>
        </div>
      </section>
    </>
  )
}

