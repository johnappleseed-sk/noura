import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { recommendationsApi } from '@/api/recommendationsApi'
import { RecommendationRail } from '@/components/ai/RecommendationRail'
import { Seo } from '@/components/common/Seo'
import { ProductCard } from '@/components/product/ProductCard'
import { addBulkItems } from '@/features/cart/cartSlice'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'
import { setCategoryFilters } from '@/features/products/productsSlice'
import {
  selectRecommendationReason,
  selectRecommendationSource,
  selectRecommendedProducts,
} from '@/features/recommendations/recommendationsSlice'
import { setChatbotOpen } from '@/features/ui/uiSlice'
import { Product } from '@/types'
import { formatCurrency } from '@/utils/currency'
import { rankRecommendations } from '@/utils/recommendationEngine'

const flashSaleStartSeconds = 3 * 60 * 60 + 22 * 60

/**
 * Executes format countdown.
 *
 * @param seconds The seconds value.
 * @returns The result of format countdown.
 */
const formatCountdown = (seconds: number): string => {
  const safe = Math.max(0, seconds)
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  const secs = safe % 60
  return `${String(hours).padStart(2, '0')}h ${String(minutes).padStart(2, '0')}m ${String(secs).padStart(2, '0')}s`
}

const categoryDescriptions: Record<string, string> = {
  Computing: 'Laptops, monitors, and workstations for modern teams.',
  Wearables: 'Health and wellness products for connected lifestyles.',
  Audio: 'Headphones, ANC gear, and professional sound tools.',
  Home: 'Smart home essentials and automated living upgrades.',
  'Home Office': 'Workspace solutions tuned for productivity.',
  Fashion: 'Functional style, travel gear, and accessories.',
  Sports: 'Performance equipment and fitness picks.',
  Lifestyle: 'Everyday essentials with premium quality.',
}

const faqItems = [
  {
    id: 'faq-ship',
    question: 'How long does shipping take?',
    answer: 'Standard delivery takes 3-5 business days. Enterprise expedited options are available at checkout.',
  },
  {
    id: 'faq-returns',
    question: 'What is your return policy?',
    answer: 'You can return eligible products within 30 days. B2B managed accounts have custom SLA-based return windows.',
  },
  {
    id: 'faq-pricing',
    question: 'Do you support tiered business pricing?',
    answer: 'Yes. B2B accounts receive volume-based tier pricing and contract-specific terms.',
  },
  {
    id: 'faq-support',
    question: 'Where can I get support?',
    answer: 'Use live chat, open a support ticket from your account, or call enterprise support from the header.',
  },
]

/**
 * Renders the HomePage component.
 *
 * @returns The rendered component tree.
 */
export const HomePage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  const status = useAppSelector((state) => state.products.status)
  const products = useAppSelector((state) => state.products.items)
  const browsingHistory = useAppSelector((state) => state.products.browsingHistory)
  const recommendations = useAppSelector(selectRecommendedProducts)
  const recommendationSource = useAppSelector(selectRecommendationSource)
  const recommendationReason = useAppSelector(selectRecommendationReason)
  const personalization = useAppSelector((state) => state.personalization)
  const geoContext = useAppSelector((state) => state.geo.context)
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const user = useAppSelector((state) => state.auth.user)
  const userId = user?.id ?? null
  const userRole = user?.role ?? null
  const orders = useAppSelector((state) => state.orders.items)
  const ordersStatus = useAppSelector((state) => state.orders.status)
  const ordersError = useAppSelector((state) => state.orders.error)

  const [flashSeconds, setFlashSeconds] = useState(flashSaleStartSeconds)
  const [newsletterEmail, setNewsletterEmail] = useState('')
  const [newsletterStatus, setNewsletterStatus] = useState('')
  const [quickOrderSku, setQuickOrderSku] = useState('')
  const [quickOrderTemplate, setQuickOrderTemplate] = useState('')
  const [quickOrderFileName, setQuickOrderFileName] = useState('')
  const [quickOrderStatus, setQuickOrderStatus] = useState('')
  const [openFaqId, setOpenFaqId] = useState<string | null>(faqItems[0]?.id ?? null)
  const [backendPersonalized, setBackendPersonalized] = useState<Product[]>([])
  const [backendCrossSell, setBackendCrossSell] = useState<Product[]>([])
  const [backendBestSellers, setBackendBestSellers] = useState<Product[]>([])
  const [backendTrending, setBackendTrending] = useState<Product[]>([])
  const [backendDeals, setBackendDeals] = useState<Product[]>([])

  useEffect(() => {
    const timer = window.setInterval(() => {
      setFlashSeconds((current) => {
        if (current <= 0) {
          return flashSaleStartSeconds
        }
        return current - 1
      })
    }, 1000)

    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!userId) {
      return
    }
    if (userRole === 'admin') {
      void dispatch(fetchAdminOrders())
      return
    }
    void dispatch(fetchMyOrders())
  }, [dispatch, userId, userRole])

  useEffect(() => {
    if (products.length === 0) {
      return
    }

    let cancelled = false

    void (async () => {
      const [personalizedResult, crossSellResult, bestSellersResult, trendingResult, dealsResult] =
        await Promise.allSettled([
          recommendationsApi.personalized(),
          recommendationsApi.crossSell(),
          recommendationsApi.bestSellers(),
          recommendationsApi.trending(),
          recommendationsApi.deals(),
        ])

      if (cancelled) {
        return
      }

      if (personalizedResult.status === 'fulfilled') {
        setBackendPersonalized(personalizedResult.value.slice(0, 6))
      }
      if (crossSellResult.status === 'fulfilled') {
        setBackendCrossSell(crossSellResult.value.slice(0, 6))
      }
      if (bestSellersResult.status === 'fulfilled') {
        setBackendBestSellers(bestSellersResult.value.slice(0, 6))
      }
      if (trendingResult.status === 'fulfilled') {
        setBackendTrending(trendingResult.value.slice(0, 6))
      }
      if (dealsResult.status === 'fulfilled') {
        setBackendDeals(dealsResult.value.slice(0, 6))
      }
    })()

    return () => {
      cancelled = true
    }
  }, [products.length])

  const productMap = useMemo(() => new Map<string, Product>(products.map((product) => [product.id, product])), [products])

  const categories = useMemo(
    () => Array.from(new Set(products.map((product) => product.category))),
    [products],
  )

  const historyCategories = useMemo(
    () =>
      Array.from(
        new Set(
          browsingHistory
            .map((id) => productMap.get(id)?.category)
            .filter((item): item is string => Boolean(item)),
        ),
      ),
    [browsingHistory, productMap],
  )

  const departmentShowcase = useMemo(() => {
    const b2bPriority = ['Computing', 'Home Office', 'Audio', 'Home']
    const prioritySeed = user?.role === 'b2b' ? b2bPriority : historyCategories
    const seeded = [...prioritySeed, ...categories]
    const unique = Array.from(new Set(seeded)).slice(0, 8)
    return unique.map((category) => ({
      category,
      description: categoryDescriptions[category] ?? 'Explore top products in this department.',
      hero: products.find((product) => product.category === category)?.images[0] ?? '',
    }))
  }, [categories, historyCategories, products, user?.role])

  const recommendedForYou = useMemo(() => {
    if (backendPersonalized.length > 0) {
      return backendPersonalized.slice(0, 6)
    }
    if (recommendations.length > 0) {
      return recommendations.slice(0, 6)
    }
    return rankRecommendations(products, browsingHistory, { limit: 6 })
  }, [backendPersonalized, recommendations, products, browsingHistory])

  const frequentlyBoughtTogether = useMemo(() => {
    if (backendCrossSell.length > 0) {
      return backendCrossSell.slice(0, 6)
    }
    const latestOrder = orders[0]
    if (!latestOrder) {
      return []
    }
    return latestOrder.items
      .map((item) => productMap.get(item.productId))
      .filter((item): item is Product => Boolean(item))
      .slice(0, 6)
  }, [backendCrossSell, orders, productMap])

  const customersAlsoViewed = useMemo(() => {
    const ranked = rankRecommendations(products, browsingHistory, { limit: 8 })
    const selected = ranked.filter((item) => !recommendedForYou.some((rec) => rec.id === item.id))
    return selected.slice(0, 6)
  }, [products, browsingHistory, recommendedForYou])

  const bestSellers = useMemo(() => {
    if (backendBestSellers.length > 0) {
      return backendBestSellers.slice(0, 6)
    }
    const scored = [...products].sort((left, right) => {
      const leftScore = left.popularity + left.rating * 10
      const rightScore = right.popularity + right.rating * 10
      return rightScore - leftScore
    })
    return scored.slice(0, 6)
  }, [backendBestSellers, products])

  const trendingNow = useMemo(() => {
    if (backendTrending.length > 0) {
      return backendTrending.slice(0, 6)
    }
    return [...products]
      .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
      .slice(0, 6)
  }, [backendTrending, products])

  const dealProducts = useMemo(() => {
    if (backendDeals.length > 0) {
      return backendDeals.slice(0, 6)
    }
    return [...products]
      .filter((product) => product.price <= 500)
      .sort((left, right) => {
        const leftDiscount = (left.originalPrice ?? left.price) - left.price
        const rightDiscount = (right.originalPrice ?? right.price) - right.price
        return rightDiscount - leftDiscount
      })
      .slice(0, 6)
  }, [backendDeals, products])

  const brandHighlights = useMemo(() => {
    const counts = new Map<string, number>()
    for (const product of products) {
      counts.set(product.brand, (counts.get(product.brand) ?? 0) + 1)
    }
    return Array.from(counts.entries())
      .sort((left, right) => right[1] - left[1])
      .slice(0, 8)
  }, [products])

  const newArrivals = useMemo(
    () =>
      [...products]
        .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
        .slice(0, 6),
    [products],
  )

  const reviewCarousel = useMemo(() => {
    const snippets: Array<{ id: string; quote: string; user: string; productName: string; rating: number }> = []
    for (const product of products) {
      for (const review of product.reviews.slice(0, 1)) {
        snippets.push({
          id: review.id,
          quote: review.comment,
          user: review.user,
          productName: product.name,
          rating: review.rating,
        })
      }
    }
    return snippets.slice(0, 8)
  }, [products])

  /**
   * Executes handle department open.
   *
   * @param category The category value.
   * @returns No value.
   */
  const handleDepartmentOpen = (category: string): void => {
    dispatch(setCategoryFilters([category]))
    navigate('/products')
  }

  /**
   * Executes handle quick order submit.
   *
   * @param event The event value.
   * @returns No value.
   */
  const handleQuickOrderSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()

    const tokens = quickOrderSku
      .split(/[\n,;\s]+/)
      .map((token) => token.trim().toLowerCase())
      .filter(Boolean)

    const lines = new Map<
      string,
      { productId: string; name: string; image: string; price: number; quantity: number; storeId: string | null; storeName?: string }
    >()

    for (const token of tokens) {
      const match = products.find(
        (product) => product.id.toLowerCase() === token || product.name.toLowerCase().includes(token),
      )
      if (!match) {
        continue
      }
      const existing = lines.get(match.id)
      if (existing) {
        existing.quantity += 1
      } else {
        lines.set(match.id, {
          productId: match.id,
          name: match.name,
          image: match.images[0],
          price: match.price,
          quantity: 1,
          storeId: selectedStore?.id ?? null,
          storeName: selectedStore?.name,
        })
      }
    }

    if (quickOrderTemplate) {
      const templateOrder = orders.find((order) => order.id === quickOrderTemplate)
      if (templateOrder) {
        for (const item of templateOrder.items) {
          const product = productMap.get(item.productId)
          if (!product) {
            continue
          }
          const existing = lines.get(product.id)
          if (existing) {
            existing.quantity += item.quantity
          } else {
            lines.set(product.id, {
              productId: product.id,
              name: product.name,
              image: product.images[0],
              price: product.price,
              quantity: item.quantity,
              storeId: selectedStore?.id ?? null,
              storeName: selectedStore?.name,
            })
          }
        }
      }
    }

    if (lines.size === 0) {
      setQuickOrderStatus(
        quickOrderFileName
          ? `Uploaded ${quickOrderFileName}. Procurement team will review the file.`
          : 'No valid SKU matched. Try product IDs or product keywords.',
      )
      return
    }

    dispatch(addBulkItems(Array.from(lines.values())))
    setQuickOrderStatus(`Added ${lines.size} items to cart.`)
    navigate('/cart')
  }

  /**
   * Executes handle newsletter submit.
   *
   * @param event The event value.
   * @returns No value.
   */
  const handleNewsletterSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    if (!newsletterEmail.includes('@')) {
      setNewsletterStatus('Please enter a valid email address.')
      return
    }
    setNewsletterStatus('Subscribed successfully. You will receive enterprise commerce updates.')
    setNewsletterEmail('')
  }

  /**
   * Executes open store locator.
   *
   * @returns No value.
   */
  const openStoreLocator = (): void => {
    dispatch(
      pushNotification({
        id: `store-locator-${Date.now()}`,
        title: 'Store locator',
        description: `Nearest ${geoContext.region.toUpperCase()} outlet lookup is running.`,
        category: 'system',
        createdAt: new Date().toISOString(),
        read: false,
      }),
    )
  }

  const recNames = recommendedForYou.slice(0, 3).map((product) => product.name).join(', ')
  const viewedNames = browsingHistory
    .map((id) => productMap.get(id)?.name)
    .filter((item): item is string => Boolean(item))
    .slice(0, 3)
    .join(', ')
  const responsiveCardGridClassName = 'grid auto-rows-fr grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6'

  return (
    <div className="space-y-10">
      <Seo
        description="Enterprise commerce homepage with role-based modules, AI recommendations, promotions, and B2B quick order."
        title="Home"
      />

      <section className="panel-high relative overflow-hidden px-6 py-10 md:px-10">
        <div
          className="absolute inset-0 -z-10 opacity-95"
          style={{
            background:
              'linear-gradient(120deg, color-mix(in oklab, var(--m3-primary) 90%, #0b1114), color-mix(in oklab, var(--m3-secondary) 70%, #111b20))',
          }}
        />
        <div className="absolute -left-12 top-0 h-56 w-56 rounded-full bg-white/25 blur-3xl" />
        <div className="absolute bottom-0 right-0 h-56 w-56 rounded-full bg-cyan-200/25 blur-3xl" />

        <div className="max-w-3xl text-white">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-100">
            Enterprise AI Commerce
          </p>
          <h1 className="mt-4 text-3xl font-black leading-tight sm:text-4xl">
            Scalable storefront experience for retail and B2B buyers.
          </h1>
          <p className="mt-3 text-sm text-brand-50/90 sm:text-base">
            Personalized discovery, live recommendations, dynamic promotions, and quick ordering workflows from one
            modular frontend.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link className="m3-btn m3-btn-tonal !px-5 !py-2 text-sm font-semibold" to="/products">
              Explore products
            </Link>
            <button
              className="m3-btn m3-btn-outlined !border-white/70 !bg-white/10 !px-5 !py-2 text-sm !text-white"
              onClick={() => dispatch(setChatbotOpen(true))}
              type="button"
            >
              Talk to AI assistant
            </button>
          </div>
        </div>
      </section>

      <section aria-label="Personalization status" className="panel p-6">
        <h2 className="m3-title">{user ? `Welcome back, ${user.fullName}` : personalization.headline}</h2>
        <p className="m3-subtitle mt-2">{personalization.subheadline}</p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="m3-chip">Session segment: {personalization.segment}</span>
          <span className="m3-chip">
            Region: {geoContext.region.toUpperCase()} / {geoContext.currency}
          </span>
          <span className="m3-chip">Role: {user?.role ?? 'guest'}</span>
          <span className="m3-chip">Recommendation source: {recommendationSource}</span>
        </div>
        {recommendationReason ? <p className="m3-subtitle mt-2 text-xs">{recommendationReason}</p> : null}
      </section>

      <section aria-label="Shop by department" className="space-y-4">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="m3-title">Shop by Department</h2>
            <p className="m3-subtitle">
              {user?.role === 'b2b'
                ? 'B2B-relevant departments are prioritized for your account.'
                : 'Departments are ranked by your recent browsing behavior.'}
            </p>
          </div>
          <Link className="m3-link" to="/products">
            Browse all
          </Link>
        </div>
        <div className={responsiveCardGridClassName}>
          {departmentShowcase.map((entry) => (
            <article className="panel relative flex h-full flex-col overflow-hidden p-4" key={entry.category}>
              {entry.hero ? (
                <img
                  alt={`${entry.category} category`}
                  className="mb-3 h-28 w-full rounded-2xl object-cover"
                  loading="lazy"
                  src={entry.hero}
                />
              ) : null}
              <h3 className="text-lg font-semibold">{entry.category}</h3>
              <p className="m3-subtitle mt-2 line-clamp-2">{entry.description}</p>
              <button
                className="m3-btn m3-btn-outlined mt-auto !h-10 !rounded-2xl !px-4 !py-2"
                onClick={() => handleDepartmentOpen(entry.category)}
                type="button"
              >
                Open department
              </button>
            </article>
          ))}
        </div>
      </section>

      <RecommendationRail
        products={recommendedForYou}
        subtitle={
          selectedStore
            ? `Customers at ${selectedStore.name} also bought these products.`
            : 'AI-ranked products based on behavior, affinity, and product quality.'
        }
        title={selectedStore ? `Trending near ${selectedStore.city}` : 'Recommended for You'}
      />

      <section aria-label="Cross sell recommendations" className="space-y-4">
        <h2 className="m3-title">Frequently Bought Together</h2>
        {frequentlyBoughtTogether.length === 0 ? (
          <div className="panel p-5 text-sm">Purchase history signals are building for cross-sell recommendations.</div>
        ) : (
          <div className={responsiveCardGridClassName}>
            {frequentlyBoughtTogether.map((product) => (
              <ProductCard key={`fbt-${product.id}`} product={product} />
            ))}
          </div>
        )}
      </section>

      <section aria-label="Social proof recommendations" className="space-y-4">
        <h2 className="m3-title">Customers Also Viewed</h2>
        <div className={responsiveCardGridClassName}>
          {customersAlsoViewed.map((product) => (
            <ProductCard key={`cav-${product.id}`} product={product} />
          ))}
        </div>
      </section>

      <section aria-label="Best sellers and trending products" className="space-y-4">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="m3-title">Best Sellers / Trending Now</h2>
            <p className="m3-subtitle">
              Region-aware demand signal: {geoContext.region.toUpperCase()} storefront index.
            </p>
          </div>
        </div>
        {status === 'loading' ? (
          <div className="panel p-6 text-sm">Loading real-time trend data...</div>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="space-y-3">
              <h3 className="text-lg font-semibold">Top Sellers</h3>
              <div className="grid gap-4 sm:grid-cols-2">
                {bestSellers.slice(0, 4).map((product) => (
                  <ProductCard key={`best-${product.id}`} product={product} />
                ))}
              </div>
            </div>
            <div className="space-y-3">
              <h3 className="text-lg font-semibold">Trending Now</h3>
              <div className="grid gap-4 sm:grid-cols-2">
                {trendingNow.slice(0, 4).map((product) => (
                  <ProductCard key={`trend-${product.id}`} product={product} />
                ))}
              </div>
            </div>
          </div>
        )}
      </section>

      <section aria-label="Deals and promotions" className="space-y-4">
        <div className="panel-high p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Flash Sale</h2>
              <p className="m3-subtitle">Ends in {formatCountdown(flashSeconds)}</p>
            </div>
            <button
              className="m3-btn m3-btn-filled !h-10 !rounded-2xl !px-4"
              onClick={() => navigate('/products')}
              type="button"
            >
              View flash products
            </button>
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <article className="panel p-3">
              <h3 className="font-semibold">Buy 2, Get 20% Off</h3>
              <p className="m3-subtitle mt-1 text-xs">Applies to accessories and lifestyle categories.</p>
            </article>
            <article className="panel p-3">
              <h3 className="font-semibold">Free Gift with Purchase</h3>
              <p className="m3-subtitle mt-1 text-xs">Orders above {formatCurrency(500)} receive a premium gift.</p>
            </article>
            <article className="panel p-3">
              <h3 className="font-semibold">Electronics under $500</h3>
              <p className="m3-subtitle mt-1 text-xs">Curated high-value picks for budget-conscious shoppers.</p>
            </article>
          </div>
        </div>
        <div className={responsiveCardGridClassName}>
          {dealProducts.map((product) => (
            <ProductCard key={`deal-${product.id}`} product={product} />
          ))}
        </div>
      </section>

      <section aria-label="Shop by brand" className="space-y-4">
        <h2 className="m3-title">Brand Highlights</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {brandHighlights.map(([brand, count]) => (
            <article className="panel flex items-center justify-between p-4" key={brand}>
              <div>
                <h3 className="text-base font-semibold">{brand}</h3>
                <p className="m3-subtitle text-xs">{count} products</p>
              </div>
              <button
                className="m3-btn m3-btn-outlined !h-9 !rounded-xl !px-3"
                onClick={() => navigate('/products')}
                type="button"
              >
                Shop
              </button>
            </article>
          ))}
        </div>
      </section>

      <section aria-label="New arrivals" className="space-y-4">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="m3-title">New Arrivals</h2>
            <p className="m3-subtitle">Latest inventory releases across categories.</p>
          </div>
          <Link className="m3-link" to="/products">
            View collection
          </Link>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {newArrivals.map((product) => (
            <article className="panel overflow-hidden" key={`new-${product.id}`}>
              <div className="relative">
                <img
                  alt={product.name}
                  className="h-48 w-full object-cover"
                  loading="lazy"
                  src={product.images[0]}
                />
                <span className="m3-chip absolute left-3 top-3 border-0 bg-emerald-500 px-3 py-1 font-semibold text-white">
                  New
                </span>
              </div>
              <div className="space-y-2 p-4">
                <Link className="block text-base font-semibold hover:opacity-80" to={`/products/${product.id}`}>
                  {product.name}
                </Link>
                <p className="m3-subtitle text-xs">{new Date(product.createdAt).toLocaleDateString()}</p>
                <p className="text-sm font-semibold">{formatCurrency(product.price)}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section aria-label="Editorial content blocks" className="space-y-4">
        <h2 className="m3-title">Guides, Insights, and Customer Stories</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <article className="panel p-5">
            <p className="m3-chip">Buying Guide</p>
            <h3 className="mt-3 text-lg font-semibold">How to choose a laptop for enterprise teams</h3>
            <p className="m3-subtitle mt-2 text-sm">
              Procurement checklist for performance tiers, security, and lifecycle planning.
            </p>
          </article>
          <article className="panel p-5">
            <p className="m3-chip">Trend Report</p>
            <h3 className="mt-3 text-lg font-semibold">2026 regional demand and pricing trends</h3>
            <p className="m3-subtitle mt-2 text-sm">
              Understand seasonal patterns and category growth by market.
            </p>
          </article>
          <article className="panel p-5">
            <p className="m3-chip">B2B Story</p>
            <h3 className="mt-3 text-lg font-semibold">How an enterprise buyer reduced reorder time by 48%</h3>
            <p className="m3-subtitle mt-2 text-sm">
              Using quick-order templates, approval queue automation, and AI recommendations.
            </p>
          </article>
        </div>
      </section>

      <section aria-label="B2B quick order module" className="panel space-y-4 p-6">
        <h2 className="m3-title">B2B Quick Order / Reorder</h2>
        {user?.role === 'b2b' ? (
          <form className="grid gap-4 lg:grid-cols-[1.3fr_1fr]" onSubmit={handleQuickOrderSubmit}>
            <div className="space-y-3">
              <label className="text-sm font-semibold" htmlFor="quick-order-sku">
                SKU Entry
              </label>
              <textarea
                className="m3-input !h-28 !rounded-2xl !py-3"
                id="quick-order-sku"
                onChange={(event) => setQuickOrderSku(event.target.value)}
                placeholder="Enter SKU IDs or keywords, separated by comma or line breaks"
                value={quickOrderSku}
              />
              <div>
                <label className="text-sm font-semibold" htmlFor="bulk-order-file">
                  Bulk order file
                </label>
                <input
                  className="m3-input mt-1 !h-11 !rounded-2xl !py-2"
                  id="bulk-order-file"
                  onChange={(event) => setQuickOrderFileName(event.target.files?.[0]?.name ?? '')}
                  type="file"
                />
                {quickOrderFileName ? <p className="m3-subtitle mt-1 text-xs">Uploaded: {quickOrderFileName}</p> : null}
              </div>
            </div>
            <div className="space-y-3">
              <label className="text-sm font-semibold" htmlFor="reorder-template">
                Reorder from past purchases
              </label>
              <select
                className="m3-select !h-11 !rounded-2xl"
                id="reorder-template"
                onChange={(event) => setQuickOrderTemplate(event.target.value)}
                value={quickOrderTemplate}
              >
                <option value="">Select an order template</option>
                {orders.map((order) => (
                  <option key={order.id} value={order.id}>
                    {order.id} - {order.status}
                  </option>
                ))}
              </select>
              {ordersStatus === 'loading' ? <p className="m3-subtitle text-xs">Loading order templates...</p> : null}
              {ordersError ? <p className="text-xs text-rose-600">{ordersError}</p> : null}
              <button className="m3-btn m3-btn-filled w-full !h-11 !rounded-2xl" type="submit">
                Add to cart
              </button>
              {quickOrderStatus ? <p className="m3-subtitle text-xs">{quickOrderStatus}</p> : null}
            </div>
          </form>
        ) : (
          <div className="panel p-5">
            <p className="text-sm">
              This module is enabled for B2B accounts. Register or contact sales to unlock quick order, file upload,
              and approval workflow features.
            </p>
            <div className="mt-3 flex gap-2">
              <Link className="m3-btn m3-btn-filled !h-10 !rounded-2xl !px-4" to="/register">
                Create account
              </Link>
              <button
                className="m3-btn m3-btn-outlined !h-10 !rounded-2xl !px-4"
                onClick={() => dispatch(setChatbotOpen(true))}
                type="button"
              >
                Contact sales
              </button>
            </div>
          </div>
        )}
      </section>

      <section aria-label="Social proof and trust signals" className="space-y-4">
        <h2 className="m3-title">Social Proof & Trust Signals</h2>
        <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
          <div className="panel p-4">
            <h3 className="text-lg font-semibold">Customer Reviews</h3>
            <div className="mt-3 flex gap-3 overflow-x-auto pb-1">
              {reviewCarousel.map((review) => (
                <article className="panel min-w-[240px] p-3" key={review.id}>
                  <p className="text-xs font-semibold">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</p>
                  <p className="mt-2 text-sm">&quot;{review.quote}&quot;</p>
                  <p className="m3-subtitle mt-2 text-xs">
                    {review.user} on {review.productName}
                  </p>
                </article>
              ))}
            </div>
          </div>
          <div className="space-y-4">
            <div className="panel p-4">
              <h3 className="text-sm font-semibold uppercase tracking-wide">Trust Badges</h3>
              <div className="mt-3 flex flex-wrap gap-2">
                <span className="m3-chip">Secure checkout</span>
                <span className="m3-chip">Money-back guarantee</span>
                <span className="m3-chip">Free returns</span>
              </div>
            </div>
            <div className="panel p-4">
              <h3 className="text-sm font-semibold uppercase tracking-wide">As Featured In</h3>
              <div className="mt-3 grid grid-cols-2 gap-2 text-center text-xs font-semibold">
                <span className="panel p-2">TechDaily</span>
                <span className="panel p-2">Commerce Weekly</span>
                <span className="panel p-2">B2B Insider</span>
                <span className="panel p-2">Retail Vision</span>
              </div>
            </div>
          </div>
        </div>
        <div className="panel p-4">
          <h3 className="text-lg font-semibold">Shoppable Community Feed</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            {products.slice(0, 3).map((product) => (
              <Link className="group relative overflow-hidden rounded-2xl" key={`ugc-${product.id}`} to={`/products/${product.id}`}>
                <img
                  alt={`Community post featuring ${product.name}`}
                  className="h-40 w-full object-cover transition group-hover:scale-105"
                  loading="lazy"
                  src={product.images[0]}
                />
                <span className="m3-chip absolute left-2 top-2 border-0 bg-black/60 text-white">
                  Shop this look
                </span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section aria-label="Service and information modules" className="space-y-4">
        <h2 className="m3-title">Service & Information</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <article className="panel p-4">
            <h3 className="text-base font-semibold">Store Locator</h3>
            <p className="m3-subtitle mt-2 text-sm">Locate nearby stores and dealer partners based on your region.</p>
            <button className="m3-btn m3-btn-outlined mt-3 !h-10 !rounded-2xl !px-4" onClick={openStoreLocator} type="button">
              Find nearest stores
            </button>
          </article>
          <article className="panel p-4">
            <h3 className="text-base font-semibold">Live Support</h3>
            <p className="m3-subtitle mt-2 text-sm">Get instant support from the AI assistant or customer success team.</p>
            <button
              className="m3-btn m3-btn-outlined mt-3 !h-10 !rounded-2xl !px-4"
              onClick={() => dispatch(setChatbotOpen(true))}
              type="button"
            >
              Open live chat
            </button>
          </article>
          <article className="panel p-4">
            <h3 className="text-base font-semibold">Shipping & Returns</h3>
            <ul className="m3-subtitle mt-2 space-y-1 text-sm">
              <li>Free shipping above {formatCurrency(300)}</li>
              <li>30-day returns for eligible products</li>
              <li>Dedicated B2B logistics support</li>
            </ul>
          </article>
        </div>

        <div className="panel p-4">
          <h3 className="text-lg font-semibold">FAQ</h3>
          <div className="mt-3 space-y-2">
            {faqItems.map((item) => {
              const isOpen = openFaqId === item.id
              return (
                <article className="rounded-2xl border p-3" key={item.id} style={{ borderColor: 'var(--m3-outline-variant)' }}>
                  <button
                    aria-controls={`${item.id}-panel`}
                    aria-expanded={isOpen}
                    className="flex w-full items-center justify-between text-left text-sm font-semibold"
                    onClick={() => setOpenFaqId((current) => (current === item.id ? null : item.id))}
                    type="button"
                  >
                    {item.question}
                    <span aria-hidden="true">{isOpen ? '−' : '+'}</span>
                  </button>
                  {isOpen ? (
                    <p className="m3-subtitle mt-2 text-sm" id={`${item.id}-panel`}>
                      {item.answer}
                    </p>
                  ) : null}
                </article>
              )
            })}
          </div>
        </div>
      </section>

      <section aria-label="Newsletter signup" className="panel-high p-6">
        <h2 className="m3-title">Stay Updated</h2>
        <p className="m3-subtitle mt-2">
          Subscribe and get 10% off your first order plus weekly trend intelligence.
        </p>
        <form className="mt-4 flex flex-col gap-2 sm:flex-row" onSubmit={handleNewsletterSubmit}>
          <label className="sr-only" htmlFor="newsletter-email">
            Email address
          </label>
          <input
            className="m3-input flex-1"
            id="newsletter-email"
            onChange={(event) => setNewsletterEmail(event.target.value)}
            placeholder="name@company.com"
            type="email"
            value={newsletterEmail}
          />
          <button className="m3-btn m3-btn-filled !h-11 !rounded-2xl !px-5" type="submit">
            Subscribe
          </button>
        </form>
        {newsletterStatus ? <p className="m3-subtitle mt-2 text-xs">{newsletterStatus}</p> : null}
      </section>

      <section aria-label="Enterprise enhancements summary" className="panel p-6">
        <h2 className="m3-title">Enterprise Adaptation Layer</h2>
        <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <article className="panel p-3">
            <h3 className="text-sm font-semibold">Role-based Content</h3>
            <p className="m3-subtitle mt-1 text-xs">
              Current role: {user?.role ?? 'guest'}. B2B modules auto-enable when role is `b2b`.
            </p>
          </article>
          <article className="panel p-3">
            <h3 className="text-sm font-semibold">Dynamic Pricing & Inventory</h3>
            <p className="m3-subtitle mt-1 text-xs">
              Live stock and pricing cards are rendered from product state and region currency.
            </p>
          </article>
          <article className="panel p-3">
            <h3 className="text-sm font-semibold">Multi-region Adaptation</h3>
            <p className="m3-subtitle mt-1 text-xs">
              Region {geoContext.region.toUpperCase()} / {geoContext.language} / {geoContext.currency}.
            </p>
          </article>
          <article className="panel p-3">
            <h3 className="text-sm font-semibold">Performance Baseline</h3>
            <p className="m3-subtitle mt-1 text-xs">
              Lazy-loaded routes, optimized images, and lightweight recommendation computation.
            </p>
          </article>
          <article className="panel p-3 md:col-span-2 xl:col-span-4">
            <h3 className="text-sm font-semibold">Personalization Snapshot</h3>
            <p className="m3-subtitle mt-1 text-xs">
              Recently viewed: {viewedNames || 'No recent history yet.'}
            </p>
            <p className="m3-subtitle text-xs">
              Recommendation sample: {recNames || 'Recommendations are warming up.'}
            </p>
            {recommendationReason ? <p className="m3-subtitle text-xs">Signal: {recommendationReason}</p> : null}
          </article>
        </div>
      </section>
    </div>
  )
}
