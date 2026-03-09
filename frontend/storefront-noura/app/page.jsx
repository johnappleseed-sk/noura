import Link from 'next/link'
import { getCategories, getProducts, getBestSellers, getTrendingProducts, getDeals, getActivePromotions, getTrendTags } from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import { PromoBanner } from '@/components/promotion'
import Badge from '@/components/ui/Badge'
import { HeroCarousel, ProductCarousel } from '@/components/carousel'

export const revalidate = 60

const apiBaseUrl = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

// Hero slides configuration
const heroSlides = [
  {
    id: 'slide-1',
    title: 'Discover products built for the way you work.',
    description: 'Premium quality, transparent pricing, and fast fulfillment — all powered by the Noura enterprise commerce engine.',
    eyebrow: 'New Season Collection',
    primaryCta: { text: 'Shop All Products', href: '/products' },
    secondaryCta: { text: 'View Deals', href: '/deals' },
    backgroundImage: '/images/hero-1.jpg',
    overlayType: 'gradient',
    contentPosition: 'left',
    showSearch: true
  },
  {
    id: 'slide-2',
    title: 'Summer Sale: Up to 50% Off',
    description: 'Limited time offers on premium products. Free shipping on orders over $99.',
    eyebrow: 'Limited Time Offer',
    primaryCta: { text: 'Shop the Sale', href: '/deals' },
    secondaryCta: { text: 'Learn More', href: '/products' },
    backgroundImage: '/images/hero-2.jpg',
    overlayType: 'gradient',
    contentPosition: 'center'
  },
  {
    id: 'slide-3',
    title: 'Enterprise Solutions Made Simple',
    description: 'Streamline your workflow with our integrated commerce platform. Built for scale.',
    eyebrow: 'For Business',
    primaryCta: { text: 'Get Started', href: '/auth/register' },
    backgroundImage: '/images/hero-3.jpg',
    overlayType: 'dark',
    contentPosition: 'right'
  }
]

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
      {/* ── Hero Carousel ── */}
      <HeroCarousel
        slides={heroSlides}
        variant="default"
        autoPlay
        autoPlayInterval={6000}
        showProgress
      />

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
            <ProductCarousel
              title="Best Sellers"
              subtitle="Most Popular"
              description="Our customers' top picks this season"
              products={bestSellers.map(p => ({ ...p, isBestseller: true }))}
              viewAllLink="/products?sort=bestselling"
            />
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
              <div className="product-grid catalog-product-grid" style={{ marginTop: 24 }}>
                {products.items.map((product) => {
                  const hasDiscount = product.compareAtPrice && product.compareAtPrice > product.price
                  const discountPercent = hasDiscount ? Math.round((1 - product.price / product.compareAtPrice) * 100) : 0
                  const stockStatus = product.lowStock ? 'low-stock' : product.stockQty > 0 ? '' : 'out-of-stock'
                  const stockLabel = product.lowStock ? 'Low stock' : product.stockQty > 0 ? 'In stock' : 'Out of stock'
                  return (
                    <Link key={product.id} href={`/products/${product.id}`} className="product-card catalog-card">
                      <div className="product-visual" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}>
                        {!product.imageUrl && <span>{product.categoryName || 'New'}</span>}
                        <div className="product-badges-overlay">
                          {hasDiscount && <span className="product-badge sale">Sale</span>}
                          {product.isNew && <span className="product-badge new">New</span>}
                        </div>
                        <div className="product-card-actions">
                          <button type="button" className="product-action-btn" aria-label="Quick view" onClick={(e) => e.preventDefault()}>👁</button>
                          <button type="button" className="product-action-btn" aria-label="Add to wishlist" onClick={(e) => e.preventDefault()}>♡</button>
                        </div>
                      </div>
                      <div className="product-meta">
                        <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                        <strong>{product.name}</strong>
                        <div className="product-price-row">
                          <p>{formatCurrency(product.price)}</p>
                          {hasDiscount && (
                            <>
                              <span className="original-price">{formatCurrency(product.compareAtPrice)}</span>
                              <span className="discount-tag">-{discountPercent}%</span>
                            </>
                          )}
                        </div>
                        <span className={`product-stock-status ${stockStatus}`}>{stockLabel}</span>
                      </div>
                    </Link>
                  )
                })}
              </div>
            </div>
          </section>

          {/* ── Trending ── */}
          {trending.length > 0 && (
            <ProductCarousel
              title="What's Hot"
              subtitle="Trending Now"
              description="Products gaining momentum right now"
              products={trending.map(p => ({ ...p, isTrending: true }))}
              viewAllLink="/products?sort=trending"
            />
          )}

          {/* ── Deals ── */}
          {deals.length > 0 && (
            <ProductCarousel
              title="Today's Deals"
              subtitle="Limited Time"
              description="Exclusive offers ending soon"
              products={deals}
              viewAllLink="/deals"
              variant="featured"
            />
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
