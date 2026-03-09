import Link from 'next/link'
import { getCategories, getProducts, getTrendTags } from '@/lib/api'
import { formatCurrency } from '@/lib/format'
import { Breadcrumbs } from '@/components/navigation'

export const revalidate = 60

function pickFirst(value, fallback = '') {
  if (Array.isArray(value)) return value[0] || fallback
  return value || fallback
}

function buildPageHref(sp, nextPage) {
  const params = new URLSearchParams()
  const q = pickFirst(sp?.q)
  const categoryId = pickFirst(sp?.categoryId)
  const sort = pickFirst(sp?.sort, 'featured')
  if (q) params.set('q', q)
  if (categoryId) params.set('categoryId', categoryId)
  if (sort) params.set('sort', sort)
  params.set('page', String(nextPage))
  return `/products?${params.toString()}`
}

function buildCategoryHref(sp, nextCategoryId) {
  const params = new URLSearchParams()
  const q = pickFirst(sp?.q)
  const sort = pickFirst(sp?.sort, 'featured')
  if (q) params.set('q', q)
  if (nextCategoryId) params.set('categoryId', String(nextCategoryId))
  if (sort) params.set('sort', sort)
  return `/products?${params.toString()}`
}

function sortLabel(sort) {
  switch (sort) {
    case 'name': return 'Name'
    case 'priceAsc': return 'Price: Low → High'
    case 'priceDesc': return 'Price: High → Low'
    default: return 'Featured'
  }
}

export default async function ProductsPage({ searchParams }) {
  const sp = (await searchParams) || {}
  const q = pickFirst(sp.q)
  const categoryId = pickFirst(sp.categoryId)
  const sort = pickFirst(sp.sort, 'featured')
  const page = Number.parseInt(pickFirst(sp.page, '0'), 10) || 0

  let categories = []
  let products = { items: [], page: 0, hasNext: false, hasPrevious: false }
  let trendTags = []
  let apiUnavailable = false

  try {
    const results = await Promise.allSettled([
      getCategories(),
      getProducts({ q, categoryId, sort, page, size: 12 }),
      getTrendTags()
    ])
    categories = results[0].status === 'fulfilled' ? results[0].value : []
    products = results[1].status === 'fulfilled' ? results[1].value : products
    trendTags = results[2].status === 'fulfilled' ? results[2].value : []
    if (results[0].status === 'rejected' && results[1].status === 'rejected') apiUnavailable = true
  } catch {
    apiUnavailable = true
  }

  const activeCategory = categories.find((c) => String(c.id) === String(categoryId))
  const resultCount = products.items.length

  return (
    <>
      {/* ── Compact Hero ── */}
      <section className="hero-compact">
        <div className="container">
          <Breadcrumbs items={[
            { label: 'Home', href: '/' },
            { label: 'Shop' },
            ...(activeCategory ? [{ label: activeCategory.name }] : [])
          ]} />
          <h1 style={{ color: '#fff', margin: '8px 0 0', fontSize: 'clamp(1.8rem,3vw,2.8rem)' }}>
            {q ? `Results for "${q}"` : activeCategory ? activeCategory.name : 'All Products'}
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.6)', marginTop: 8 }}>
            {resultCount} products found &middot; Page {page + 1} &middot; Sorted by {sortLabel(sort)}
          </p>
        </div>
      </section>

      <section className="featured-section catalog-section">
        <div className="catalog-layout">
            {/* ── Sidebar ── */}
            <aside className="catalog-sidebar">
              <div className="panel sidebar-panel">
                <h3>Search</h3>
                <form action="/products" style={{ display: 'grid', gap: 8 }}>
                  <input type="text" name="q" className="form-input" placeholder="Search products..." defaultValue={q} style={{ width: '100%' }} />
                  {categoryId && <input type="hidden" name="categoryId" value={categoryId} />}
                  <input type="hidden" name="sort" value={sort} />
                  <button type="submit" className="button primary sm" style={{ width: '100%' }}>Search</button>
                </form>
              </div>

              <div className="panel sidebar-panel">
                <h3>Categories</h3>
                <div style={{ display: 'grid', gap: 4 }}>
                  <Link
                    href={buildCategoryHref(sp, null)}
                    className={`chip-link${!categoryId ? ' active' : ''}`}
                    style={{ minWidth: 0, padding: '8px 12px' }}
                  >
                    <span>All Categories</span>
                  </Link>
                  {categories.map((cat) => (
                    <Link
                      key={cat.id}
                      href={buildCategoryHref(sp, cat.id)}
                      className={`chip-link${String(cat.id) === String(categoryId) ? ' active' : ''}`}
                      style={{ minWidth: 0, padding: '8px 12px' }}
                    >
                      <span>{cat.name}</span>
                      <small>{cat.productCount || 0}</small>
                    </Link>
                  ))}
                </div>
              </div>

              <div className="panel sidebar-panel">
                <h3>Sort By</h3>
                <div style={{ display: 'grid', gap: 4 }}>
                  {[
                    { v: 'featured', l: 'Featured' },
                    { v: 'name', l: 'Name' },
                    { v: 'priceAsc', l: 'Price: Low → High' },
                    { v: 'priceDesc', l: 'Price: High → Low' }
                  ].map((s) => (
                    <Link
                      key={s.v}
                      href={`/products?${new URLSearchParams({ ...(q ? { q } : {}), ...(categoryId ? { categoryId } : {}), sort: s.v }).toString()}`}
                      className={`chip-link${sort === s.v ? ' active' : ''}`}
                      style={{ minWidth: 0, padding: '8px 12px' }}
                    >
                      <span>{s.l}</span>
                    </Link>
                  ))}
                </div>
              </div>

              {trendTags.length > 0 && (
                <div className="panel sidebar-panel">
                  <h3>Trending</h3>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {trendTags.slice(0, 10).map((tag, i) => (
                      <Link
                        key={i}
                        href={`/products?q=${encodeURIComponent(tag.name || tag.tag || tag)}`}
                        className="filter-pill"
                        style={{ cursor: 'pointer', fontSize: '0.72rem' }}
                      >
                        {tag.name || tag.tag || tag}
                      </Link>
                    ))}
                  </div>
                </div>
              )}

              {(q || categoryId) && (
                <Link href="/products" className="button ghost sm" style={{ width: '100%' }}>
                  Clear All Filters
                </Link>
              )}
            </aside>

            {/* ── Product Grid ── */}
            <div style={{ display: 'grid', gap: 24, alignContent: 'start' }}>
              {apiUnavailable ? (
                <div className="panel empty-state">
                  <div className="empty-copy-block">
                    <span className="eyebrow">Catalog offline</span>
                    <h2>The storefront is ready, but the catalog feed is offline.</h2>
                    <p>Start the backend on <code>http://localhost:8080</code> to load products.</p>
                  </div>
                </div>
              ) : products.items.length === 0 ? (
                <div className="panel empty-state">
                  <div className="empty-copy-block">
                    <span className="eyebrow">No results</span>
                    <h2>No products matched your filters.</h2>
                    <p>Try adjusting your search or browse all categories.</p>
                  </div>
                  <div className="empty-actions">
                    <Link href="/products" className="button primary">View All Products</Link>
                  </div>
                </div>
              ) : (
                <>
                  <div className="product-grid catalog-product-grid">
                    {products.items.map((product) => (
                      <Link key={product.id} href={`/products/${product.id}`} className="product-card catalog-card">
                        <div className="product-visual" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}>
                          {!product.imageUrl && <span>{product.categoryName || 'Product'}</span>}
                        </div>
                        <div className="product-meta">
                          <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                          <strong>{product.name}</strong>
                          <p>{formatCurrency(product.price)}</p>
                          <small>{product.lowStock ? 'Low stock' : product.stockQty > 0 ? 'In stock' : 'Check availability'}</small>
                        </div>
                      </Link>
                    ))}
                  </div>
                  <div className="pager pager-panel" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    {products.hasPrevious ? (
                      <Link href={buildPageHref(sp, products.page - 1)} className="button ghost sm">&larr; Previous</Link>
                    ) : <span />}
                    <span className="pager-label">Page {products.page + 1}</span>
                    {products.hasNext ? (
                      <Link href={buildPageHref(sp, products.page + 1)} className="button ghost sm">Next &rarr;</Link>
                    ) : <span />}
                  </div>
                </>
              )}
            </div>
          </div>
      </section>
    </>
  )
}
