import Skeleton from '@/components/ui/Skeleton'

export default function ProductDetailLoading() {
  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Skeleton variant="text" width={260} height={14} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container">
          <div className="pdp-layout">
            <div className="panel" style={{ minHeight: 480, padding: 0 }}>
              <Skeleton variant="rect" height={480} />
            </div>
            <div style={{ display: 'grid', gap: 12 }}>
              <Skeleton variant="text" width="30%" height={14} />
              <Skeleton variant="text" width="80%" height={36} />
              <Skeleton variant="text" width="25%" height={26} />
              <Skeleton variant="rect" height={120} />
              <Skeleton variant="rect" height={52} />
            </div>
          </div>
        </div>
      </section>
    </>
  )
}

