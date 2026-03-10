import Link from 'next/link'
import { getCategories, getProducts, getBestSellers, getTrendingProducts, getDeals, getActivePromotions, getTrendTags, getHeroSlides } from '@/lib/api'
import { PromoBanner } from '@/components/promotion'
import Badge from '@/components/ui/Badge'
import { HeroCarousel, ProductCarousel } from '@/components/carousel'
import TrackedProductGrid from '@/components/analytics/TrackedProductGrid'
import TrackedCategoryGrid from '@/components/analytics/TrackedCategoryGrid'

export const revalidate = 60

const apiBaseUrl = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default async function HomePage() {
  const heroLocale = process.env.NEXT_PUBLIC_STOREFRONT_LOCALE || process.env.STOREFRONT_LOCALE || 'en-US'
  const heroChannelId = process.env.NEXT_PUBLIC_STOREFRONT_CHANNEL_ID || process.env.STOREFRONT_CHANNEL_ID || undefined
  const heroStoreId = process.env.NEXT_PUBLIC_STOREFRONT_STORE_ID || process.env.STOREFRONT_STORE_ID || undefined

  let heroSlides = []
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
      getHeroSlides({ storeId: heroStoreId, channelId: heroChannelId, locale: heroLocale }),
      getCategories(),
      getProducts({ size: 8 }),
      getBestSellers(),
      getTrendingProducts(),
      getDeals(),
      getActivePromotions(),
      getTrendTags()
    ])

    heroSlides = results[0].status === 'fulfilled' ? results[0].value : []
    categories = results[1].status === 'fulfilled' ? results[1].value : []
    products = results[2].status === 'fulfilled' ? results[2].value : { items: [] }
    bestSellers = results[3].status === 'fulfilled' ? results[3].value : []
    trending = results[4].status === 'fulfilled' ? results[4].value : []
    deals = results[5].status === 'fulfilled' ? results[5].value : []
    promotions = results[6].status === 'fulfilled' ? results[6].value : []
    trendTags = results[7].status === 'fulfilled' ? results[7].value : []

    if (results[1].status === 'rejected' && results[2].status === 'rejected') {
      apiUnavailable = true
    }
  } catch {
    apiUnavailable = true
  }

  return (
    <>
      {/* ── Hero Carousel ── */}
      {heroSlides.length ? (
        <HeroCarousel
          slides={heroSlides}
          variant="default"
          autoPlay
          autoPlayInterval={6000}
          showProgress
        />
      ) : (
        <section
          style={{
            background: 'linear-gradient(135deg, #091224, #12395a)',
            color: '#ffffff',
            padding: '72px 0'
          }}
        >
          <div className="container">
            <div style={{ maxWidth: 640 }}>
              <span className="eyebrow" style={{ color: 'rgba(255,255,255,0.7)' }}>Storefront</span>
              <h1 style={{ fontSize: 'clamp(2.6rem, 5vw, 4.5rem)', lineHeight: 1.05, margin: '12px 0 18px' }}>
                Commerce content updates are loading.
              </h1>
              <p style={{ fontSize: '1.05rem', lineHeight: 1.7, color: 'rgba(255,255,255,0.82)' }}>
                No active hero slides are currently available for this store or locale. The storefront continues to render safely without carousel data.
              </p>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 28 }}>
                <Link href="/products" className="button primary lg">Browse products</Link>
                <Link href="/deals" className="button ghost lg">View deals</Link>
              </div>
            </div>
          </div>
        </section>
      )}

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
              <TrackedCategoryGrid
                categories={categories.slice(0, 8)}
                listName="home-category-grid"
                pagePath="/"
                className="category-grid"
                style={{ marginTop: 24 }}
              />
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
              analyticsListName="home-best-sellers-carousel"
              analyticsPagePath="/"
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
              <TrackedProductGrid
                products={products.items}
                listName="home-featured-products-grid"
                pagePath="/"
                layoutClassName="product-grid catalog-product-grid"
                variant="catalog"
              />
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
              analyticsListName="home-trending-carousel"
              analyticsPagePath="/"
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
              analyticsListName="home-deals-carousel"
              analyticsPagePath="/"
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
