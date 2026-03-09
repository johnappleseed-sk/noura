import Link from 'next/link'
import { getCategories, getProducts, getBestSellers, getTrendingProducts, getDeals, getActivePromotions, getTrendTags } from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import { PromoBanner } from '@/components/promotion'
import Badge from '@/components/ui/Badge'

export const revalidate = 60

const apiBaseUrl = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default async function HomePage() {
  let categories = []
  let products = { items: [] }
  let bestSellers = []
  let trending = []
  let deals = []
  let promotions = []
  let trendTags = []
  let apiUnavailable = false

  try {
    const results = await Promise.allSettled([
      getCategories(),
      getProducts({ size: 8 }),
      getBestSellers(),
      getTrendingProducts(),
      getDeals(),
      getActivePromotions(),
      getTrendTags()
    ])

    categories = results[0].status === 'fulfilled' ? results[0].value : []
    products = results[1].status === 'fulfilled' ? results[1].value : { items: [] }
    bestSellers = results[2].status === 'fulfilled' ? results[2].value : []
    trending = results[3].status === 'fulfilled' ? results[3].value : []
    deals = results[4].status === 'fulfilled' ? results[4].value : []
    promotions = results[5].status === 'fulfilled' ? results[5].value : []
    trendTags = results[6].status === 'fulfilled' ? results[6].value : []

    if (results[0].status === 'rejected' && results[1].status === 'rejected') {
      apiUnavailable = true
    }
  } catch {
    apiUnavailable = true
  }

  return (
    <>
      {/* ── Hero ── */}
      <section className="hero-full">
        <div className="container" style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 48, alignItems: 'center' }}>
          <div>
            <span className="eyebrow">New Season Collection</span>
            <h1>Discover products built for the way you work.</h1>
            <p className="lede">
              Premium quality, transparent pricing, and fast fulfillment — all powered by the Noura enterprise commerce engine.
            </p>
            <div className="hero-actions" style={{ marginTop: 24 }}>
              <Link href="/products" className="button white lg">
                Shop All Products
              </Link>
              <Link href="/deals" className="button ghost lg" style={{ borderColor: 'rgba(255,255,255,0.2)', color: 'white' }}>
                View Deals
              </Link>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div className="metric" style={{ background: 'rgba(255,255,255,0.08)', borderColor: 'rgba(255,255,255,0.1)', color: 'white' }}>
              <strong style={{ color: 'white' }}>{categories.length}</strong>
              <span style={{ color: 'rgba(255,255,255,0.6)' }}>Categories</span>
            </div>
            <div className="metric" style={{ background: 'rgba(255,255,255,0.08)', borderColor: 'rgba(255,255,255,0.1)', color: 'white' }}>
              <strong style={{ color: 'white' }}>{products.items.length}+</strong>
              <span style={{ color: 'rgba(255,255,255,0.6)' }}>Products</span>
            </div>
            <div className="metric" style={{ background: 'rgba(255,255,255,0.08)', borderColor: 'rgba(255,255,255,0.1)', color: 'white' }}>
              <strong style={{ color: 'white' }}>{promotions.length}</strong>
              <span style={{ color: 'rgba(255,255,255,0.6)' }}>Active Promos</span>
            </div>
            <div className="metric" style={{ background: 'rgba(255,255,255,0.08)', borderColor: 'rgba(255,255,255,0.1)', color: 'white' }}>
              <strong style={{ color: 'white' }}>{trendTags.length}</strong>
              <span style={{ color: 'rgba(255,255,255,0.6)' }}>Trending Tags</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── Trust Bar ── */}
      <div className="trust-bar">
        <div className="trust-item">
          <span className="trust-icon">🚚</span>
          <span>Free Shipping Over $99</span>
        </div>
        <div className="trust-item">
          <span className="trust-icon">🔒</span>
          <span>Secure Checkout</span>
        </div>
        <div className="trust-item">
          <span className="trust-icon">↩️</span>
          <span>Easy Returns</span>
        </div>
        <div className="trust-item">
          <span className="trust-icon">💬</span>
          <span>24/7 Support</span>
        </div>
      </div>

      {apiUnavailable ? (
        <section className="featured-section">
          <div className="container">
            <div className="panel notice">
              <h2>Backend unavailable</h2>
              <p>Start the Spring Boot app on <code>{apiBaseUrl}</code> to load live data.</p>
            </div>
          </div>
        </section>
      ) : (
        <>
          {/* ── Trending Tags ── */}
          {trendTags.length > 0 && (
            <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 32 }}>
              <div className="container">
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                  <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--muted)' }}>Trending:</span>
                  {trendTags.map((tag, i) => (
                    <Link key={i} href={`/products?q=${encodeURIComponent(tag.name || tag.tag || tag)}`}>
                      <Badge variant="trending">{tag.name || tag.tag || tag}</Badge>
                    </Link>
                  ))}
                </div>
              </div>
            </section>
          )}

          {/* ── Categories ── */}
          <section className="featured-section">
            <div className="container">
              <div className="section-head">
                <div>
                  <span className="eyebrow">Shop by Category</span>
                  <h2>Browse our collections</h2>
                </div>
                <Link href="/products" className="inline-link">
                  View all &rarr;
                </Link>
              </div>
              <div className="category-grid" style={{ marginTop: 24 }}>
                {categories.slice(0, 8).map((category) => (
                  <Link
                    key={category.id}
                    href={`/products?categoryId=${category.id}`}
                    className="category-card"
                  >
                    <span className="category-count">{category.productCount} products</span>
                    <strong>{category.name}</strong>
                    <p>{category.description || 'Explore this collection'}</p>
                  </Link>
                ))}
              </div>
            </div>
          </section>

          {/* ── Best Sellers ── */}
          {bestSellers.length > 0 && (
            <section className="featured-section">
              <div className="container">
                <div className="section-head">
                  <div>
                    <span className="eyebrow">Most Popular</span>
                    <h2>Best Sellers</h2>
                  </div>
                </div>
                <p className="section-subtitle">Our customers&apos; top picks this season</p>
                <div className="product-scroll-row">
                  {bestSellers.map((product) => (
                    <Link key={product.id} href={`/products/${product.id}`} className="product-card">
                      <div
                        className="product-visual"
                        style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
                      >
                        {!product.imageUrl && <span>{product.categoryName || 'Best Seller'}</span>}
                      </div>
                      <div className="product-meta">
                        <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                        <strong>{product.name}</strong>
                        <p>{formatCurrency(product.price)}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            </section>
          )}

          {/* ── Promo Banner ── */}
          {promotions.length > 0 && (
            <PromoBanner
              title={promotions[0].name || 'Special Offer'}
              subtitle={promotions[0].description || 'Limited time deals across selected categories'}
              cta="View All Deals"
              href="/deals"
            />
          )}

          {/* ── Featured Products ── */}
          <section className="featured-section">
            <div className="container">
              <div className="section-head">
                <div>
                  <span className="eyebrow">Just In</span>
                  <h2>Featured Products</h2>
                </div>
                <Link href="/products" className="inline-link">
                  Shop all &rarr;
                </Link>
              </div>
              <div className="product-grid" style={{ marginTop: 24 }}>
                {products.items.map((product) => (
                  <Link key={product.id} href={`/products/${product.id}`} className="product-card">
                    <div
                      className="product-visual"
                      style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
                    >
                      {!product.imageUrl && <span>{product.categoryName || 'New'}</span>}
                    </div>
                    <div className="product-meta">
                      <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                      <strong>{product.name}</strong>
                      <p>{formatCurrency(product.price)}</p>
                      <small>
                        {product.lowStock ? 'Low stock — order soon' : product.stockQty > 0 ? 'In stock' : 'Check availability'}
                      </small>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          </section>

          {/* ── Trending ── */}
          {trending.length > 0 && (
            <section className="featured-section">
              <div className="container">
                <div className="section-head">
                  <div>
                    <span className="eyebrow">Trending Now</span>
                    <h2>What&apos;s Hot</h2>
                  </div>
                </div>
                <p className="section-subtitle">Products gaining momentum right now</p>
                <div className="product-scroll-row">
                  {trending.map((product) => (
                    <Link key={product.id} href={`/products/${product.id}`} className="product-card">
                      <div
                        className="product-visual"
                        style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
                      >
                        {!product.imageUrl && <span>{product.categoryName || 'Trending'}</span>}
                      </div>
                      <div className="product-meta">
                        <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                        <strong>{product.name}</strong>
                        <p>{formatCurrency(product.price)}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            </section>
          )}

          {/* ── Deals ── */}
          {deals.length > 0 && (
            <section className="featured-section">
              <div className="container">
                <div className="section-head">
                  <div>
                    <span className="eyebrow">Limited Time</span>
                    <h2>Today&apos;s Deals</h2>
                  </div>
                  <Link href="/deals" className="inline-link">
                    See all deals &rarr;
                  </Link>
                </div>
                <div className="product-grid" style={{ marginTop: 24 }}>
                  {deals.slice(0, 4).map((product) => (
                    <Link key={product.id} href={`/products/${product.id}`} className="product-card">
                      <div
                        className="product-visual"
                        style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
                      >
                        {!product.imageUrl && <span>Deal</span>}
                      </div>
                      <div className="product-meta">
                        <span className="product-category">{product.categoryName || 'Deal'}</span>
                        <strong>{product.name}</strong>
                        <p>{formatCurrency(product.price)}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            </section>
          )}
        </>
      )}

      {/* ── CTA Banner ── */}
      <PromoBanner
        title="Join Noura Today"
        subtitle="Create an account to unlock personalized recommendations, order tracking, and exclusive deals."
        cta="Create Free Account"
        href="/auth/register"
        bgColor="linear-gradient(135deg, #059669, #0d9488)"
      />
    </>
  )
}
