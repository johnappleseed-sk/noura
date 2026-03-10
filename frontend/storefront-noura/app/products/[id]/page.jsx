import Link from 'next/link'
import { notFound } from 'next/navigation'
import { getAvailability, getProduct, getProductReviews, getRelatedProducts, getFrequentlyBoughtTogether } from '@/lib/api'
import { formatCurrency, formatMeasure } from '@/lib/format'
import StarRating from '@/components/ui/StarRating'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'
import AddToCartButton from '@/components/product/AddToCartButton'
import ProductAnalyticsTracker from '@/components/analytics/ProductAnalyticsTracker'
import TrackedProductGrid from '@/components/analytics/TrackedProductGrid'

export const revalidate = 60

export default async function ProductDetailPage({ params }) {
  const resolvedParams = (await params) || {}
  let product
  let availability
  let reviews = []
  let related = []
  let fbt = []

  try {
    ;[product, availability] = await Promise.all([
      getProduct(resolvedParams.id),
      getAvailability(resolvedParams.id)
    ])
  } catch {
    notFound()
  }

  try {
    const extras = await Promise.allSettled([
      getProductReviews(resolvedParams.id),
      getRelatedProducts(resolvedParams.id),
      getFrequentlyBoughtTogether(resolvedParams.id)
    ])
    reviews = extras[0].status === 'fulfilled' ? (Array.isArray(extras[0].value) ? extras[0].value : extras[0].value?.items || []) : []
    related = extras[1].status === 'fulfilled' ? (Array.isArray(extras[1].value) ? extras[1].value : []) : []
    fbt = extras[2].status === 'fulfilled' ? (Array.isArray(extras[2].value) ? extras[2].value : []) : []
  } catch { /* graceful */ }

  const avgRating = reviews.length > 0
    ? (reviews.reduce((sum, r) => sum + (r.rating || 0), 0) / reviews.length).toFixed(1)
    : null

  const inStock = availability.stockQty > 0 || availability.allowNegativeStock

  return (
    <>
      <ProductAnalyticsTracker productId={resolvedParams.id} />

      {/* ── Breadcrumbs ── */}
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs
            items={[
              { label: 'Home', href: '/' },
              { label: 'Shop', href: '/products' },
              ...(product.categoryName ? [{ label: product.categoryName, href: `/products?categoryId=${product.categoryId}` }] : []),
              { label: product.name },
            ]}
          />
        </div>
      </section>

      {/* ── Product Detail ── */}
      <section className="featured-section" style={{ paddingTop: 32 }}>
        <div className="container">
          <div className="pdp-layout">
            {/* Image */}
            <div className="pdp-image panel" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})`, backgroundSize: 'cover', backgroundPosition: 'center', minHeight: 480 } : { display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 480, background: 'var(--line-light)' }}>
              {!product.imageUrl && <span style={{ fontSize: '1.5rem', color: 'var(--muted)' }}>{product.categoryName || 'Product'}</span>}
            </div>

            {/* Info */}
            <div className="pdp-info">
              <span className="product-category">{product.categoryName || 'Product'}</span>
              <h1 style={{ margin: '8px 0 12px', fontSize: 'clamp(1.6rem, 2.5vw, 2.2rem)' }}>{product.name}</h1>

              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ fontSize: '1.8rem', fontWeight: 700 }}>{formatCurrency(product.price)}</span>
                {avgRating && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--warning)' }}>
                    <StarRating value={Number(avgRating)} size="md" />
                    <small style={{ color: 'var(--muted)' }}>({reviews.length} reviews)</small>
                  </span>
                )}
              </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
                <Badge variant={inStock ? 'success' : 'danger'}>
                  {inStock ? (availability.lowStock ? 'Low Stock' : 'In Stock') : 'Out of Stock'}
                </Badge>
                {availability.active && <Badge variant="info">Active Listing</Badge>}
                {availability.allowNegativeStock && <Badge>Backorder Available</Badge>}
              </div>

              {product.description && (
                <p style={{ color: 'var(--muted)', lineHeight: 1.7, marginBottom: 20 }}>{product.description}</p>
              )}

              {/* Add to Cart section */}
              <div className="panel" style={{ padding: 20, marginBottom: 24 }}>
                <AddToCartButton productId={resolvedParams.id} disabled={!inStock} />
                <p style={{ fontSize: '0.78rem', color: 'var(--muted)', marginTop: 8, marginBottom: 0 }}>
                  Free shipping on orders over $99. 30-day returns.
                </p>
              </div>

              {/* Specs */}
              <div style={{ borderTop: '1px solid var(--line)', paddingTop: 20 }}>
                <h3 style={{ fontSize: '0.95rem', marginBottom: 12 }}>Specifications</h3>
                <dl className="spec-grid">
                  <div><dt>SKU</dt><dd>{product.sku || 'N/A'}</dd></div>
                  <div><dt>Barcode</dt><dd>{product.barcode || 'N/A'}</dd></div>
                  <div><dt>Base Unit</dt><dd>{product.baseUnitName || 'piece'}</dd></div>
                  <div><dt>Weight</dt><dd>{formatMeasure(product.weightValue, product.weightUnit)}</dd></div>
                  <div><dt>Dimensions</dt><dd>{formatMeasure(product.lengthValue, product.lengthUnit)} × {formatMeasure(product.widthValue, product.widthUnit)} × {formatMeasure(product.heightValue, product.heightUnit)}</dd></div>
                  <div><dt>Low Stock Threshold</dt><dd>{product.lowStockThreshold ?? 'N/A'}</dd></div>
                </dl>
              </div>

              {product.boxSpecifications && (
                <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, marginTop: 16 }}>
                  <h3 style={{ fontSize: '0.95rem', marginBottom: 8 }}>Packaging</h3>
                  <p style={{ color: 'var(--muted)' }}>{product.boxSpecifications}</p>
                </div>
              )}

              {product.units && product.units.length > 0 && (
                <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, marginTop: 16 }}>
                  <h3 style={{ fontSize: '0.95rem', marginBottom: 8 }}>Available Sell Units</h3>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {product.units.map((unit) => (
                      <Badge key={unit.id}>
                        {unit.name} ({unit.abbreviation || 'unit'}) — {unit.conversionToBase}x base
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ── Frequently Bought Together ── */}
      {fbt.length > 0 && (
        <section className="featured-section">
          <div className="container">
            <h2 className="section-title">Frequently Bought Together</h2>
            <TrackedProductGrid
              products={fbt}
              listName="product-detail-frequently-bought-together-rail"
              pagePath={`/products/${resolvedParams.id}`}
              layoutClassName="scroll-row"
              variant="compact"
              cardClassName="product-card"
              cardStyle={{ minWidth: 220 }}
            />
          </div>
        </section>
      )}

      {/* ── Related Products ── */}
      {related.length > 0 && (
        <section className="featured-section">
          <div className="container">
            <h2 className="section-title">Related Products</h2>
            <TrackedProductGrid
              products={related.slice(0, 6)}
              listName="product-detail-related-grid"
              pagePath={`/products/${resolvedParams.id}`}
              layoutClassName="product-grid"
              variant="compact"
              cardClassName="product-card"
            />
          </div>
        </section>
      )}

      {/* ── Reviews ── */}
      <section className="featured-section" style={{ paddingBottom: 48 }}>
        <div className="container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
            <h2 className="section-title" style={{ marginBottom: 0 }}>Customer Reviews</h2>
            {avgRating && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <StarRating value={Number(avgRating)} size="md" />
                <strong>{avgRating}</strong>
                <span style={{ color: 'var(--muted)' }}>({reviews.length} reviews)</span>
              </div>
            )}
          </div>

          {reviews.length === 0 ? (
            <div className="panel" style={{ padding: 32, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)' }}>No reviews yet. Be the first to review this product.</p>
            </div>
          ) : (
            <div style={{ display: 'grid', gap: 16 }}>
              {reviews.slice(0, 10).map((review, i) => (
                <div key={review.id || i} className="panel review-card" style={{ padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <div>
                      <strong>{review.customerName || review.author || 'Customer'}</strong>
                      <StarRating value={review.rating} size="sm" />
                    </div>
                    {review.createdAt && (
                      <small style={{ color: 'var(--muted)' }}>{new Date(review.createdAt).toLocaleDateString()}</small>
                    )}
                  </div>
                  {review.title && <h4 style={{ margin: '4px 0 8px' }}>{review.title}</h4>}
                  {review.comment && <p style={{ color: 'var(--muted)', margin: 0 }}>{review.comment}</p>}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  )
}
