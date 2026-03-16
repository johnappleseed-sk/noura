import Link from 'next/link'
import { getCategories, getProducts, getTrendTags } from '@/lib/api'
import { Breadcrumbs } from '@/components/navigation'
import MerchandisingProductGrid from '@/components/analytics/MerchandisingProductGrid'
import Pagination from '@/components/ui/Pagination'

export const revalidate = 60

const PAGE_SIZE_OPTIONS = [12, 24, 48]
const SORT_OPTIONS = [
  { value: 'featured', label: 'Featured' },
  { value: 'popularity', label: 'Popularity' },
  { value: 'trending', label: 'Trending' },
  { value: 'bestselling', label: 'Best Selling' },
  { value: 'new', label: 'New Arrivals' },
  { value: 'name', label: 'Name' },
  { value: 'priceAsc', label: 'Price: Low → High' },
  { value: 'priceDesc', label: 'Price: High → Low' }
]

function pickFirst(value, fallback = '') {
  if (Array.isArray(value)) return value[0] || fallback
  return value || fallback
}

function normalizePage(value) {
  const parsed = Number.parseInt(String(value || '0'), 10)
  if (Number.isNaN(parsed) || parsed < 0) return 0
  return parsed
}

function normalizePageSize(value) {
  const parsed = Number.parseInt(String(value || '12'), 10)
  if (!PAGE_SIZE_OPTIONS.includes(parsed)) {
    return 12
  }
  return parsed
}

function sortLabel(sort) {
  return SORT_OPTIONS.find((entry) => entry.value === sort)?.label || 'Featured'
}

function buildCatalogHref(sp, overrides = {}) {
  const params = new URLSearchParams()
  const next = {
    q: pickFirst(sp?.q),
    categoryId: pickFirst(sp?.categoryId),
    storeId: pickFirst(sp?.storeId),
    sort: pickFirst(sp?.sort, 'featured'),
    size: pickFirst(sp?.size, '12'),
    page: pickFirst(sp?.page, '0'),
    ...overrides
  }

  if (next.q) params.set('q', next.q)
  if (next.categoryId) params.set('categoryId', String(next.categoryId))
  if (next.storeId) params.set('storeId', String(next.storeId))
  if (next.sort) params.set('sort', String(next.sort))
  if (next.size) params.set('size', String(next.size))
  if (next.page != null) params.set('page', String(next.page))

  return `/products?${params.toString()}`
}

function buildCategoryHref(sp, nextCategoryId) {
  return buildCatalogHref(sp, { categoryId: nextCategoryId || null, page: 0 })
}

function buildPageHref(sp, nextPage) {
  return buildCatalogHref(sp, { page: nextPage })
}

export default async function ProductsPage({ searchParams }) {
  const sp = (await searchParams) || {}
  const q = pickFirst(sp.q).trim()
  const categoryId = pickFirst(sp.categoryId).trim()
  const storeId = pickFirst(sp.storeId).trim()
  const sort = pickFirst(sp.sort, 'featured').trim()
  const page = normalizePage(pickFirst(sp.page, '0'))
  const size = normalizePageSize(pickFirst(sp.size, '12'))
  const analyticsListName = q ? 'search-results-grid' : categoryId ? 'category-results-grid' : 'catalog-grid'

  let categories = []
  let products = {
    items: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false
  }
  let trendTags = []
  let apiUnavailable = false

  try {
    const results = await Promise.allSettled([
      getCategories(),
      getProducts({ q, categoryId, storeId, sort, page, size }),
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
  const resultCount = Number(products.totalElements || products.items.length || 0)
  const activeFilters = [
    q ? { label: `Search: ${q}`, href: buildCatalogHref(sp, { q: null, page: 0 }) } : null,
    categoryId && activeCategory
      ? { label: `Category: ${activeCategory.name}`, href: buildCatalogHref(sp, { categoryId: null, page: 0 }) }
      : null,
    storeId ? { label: `Store: ${storeId}`, href: buildCatalogHref(sp, { storeId: null, page: 0 }) } : null,
    sort && sort !== 'featured' ? { label: `Sort: ${sortLabel(sort)}`, href: buildCatalogHref(sp, { sort: 'featured', page: 0 }) } : null,
    size !== 12 ? { label: `Page size: ${size}`, href: buildCatalogHref(sp, { size: 12, page: 0 }) } : null
  ].filter(Boolean)

  return (
    <>
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
            {resultCount} products found · Page {products.page + 1} · Sorted by {sortLabel(sort)}
          </p>
        </div>
      </section>

      <section className="featured-section catalog-section">
        <div className="catalog-layout">
          <aside className="catalog-sidebar">
            <div className="panel sidebar-panel">
              <h3>Search</h3>
              <form action="/products" style={{ display: 'grid', gap: 8 }}>
                <input
                  type="text"
                  name="q"
                  className="form-input"
                  placeholder="Search products..."
                  defaultValue={q}
                  style={{ width: '100%' }}
                />
                <input
                  type="text"
                  name="storeId"
                  className="form-input"
                  placeholder="Store ID (optional)"
                  defaultValue={storeId}
                  style={{ width: '100%' }}
                />
                {categoryId && <input type="hidden" name="categoryId" value={categoryId} />}
                <input type="hidden" name="sort" value={sort} />
                <input type="hidden" name="size" value={String(size)} />
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
                {SORT_OPTIONS.map((entry) => (
                  <Link
                    key={entry.value}
                    href={buildCatalogHref(sp, { sort: entry.value, page: 0 })}
                    className={`chip-link${sort === entry.value ? ' active' : ''}`}
                    style={{ minWidth: 0, padding: '8px 12px' }}
                  >
                    <span>{entry.label}</span>
                  </Link>
                ))}
              </div>
            </div>

            <div className="panel sidebar-panel">
              <h3>Page Size</h3>
              <div style={{ display: 'grid', gap: 4 }}>
                {PAGE_SIZE_OPTIONS.map((option) => (
                  <Link
                    key={option}
                    href={buildCatalogHref(sp, { size: option, page: 0 })}
                    className={`chip-link${Number(size) === Number(option) ? ' active' : ''}`}
                    style={{ minWidth: 0, padding: '8px 12px' }}
                  >
                    <span>{option} per page</span>
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
                      href={buildCatalogHref(sp, { q: tag.name || tag.tag || tag, page: 0 })}
                      className="filter-pill"
                      style={{ cursor: 'pointer', fontSize: '0.72rem' }}
                    >
                      {tag.name || tag.tag || tag}
                    </Link>
                  ))}
                </div>
              </div>
            )}

            {(q || categoryId || storeId || sort !== 'featured' || size !== 12) && (
              <Link href="/products" className="button ghost sm" style={{ width: '100%' }}>
                Clear All Filters
              </Link>
            )}
          </aside>

          <div style={{ display: 'grid', gap: 24, alignContent: 'start' }}>
            {activeFilters.length > 0 ? (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {activeFilters.map((filter) => (
                  <Link key={filter.label} href={filter.href} className="filter-pill">
                    {filter.label} ✕
                  </Link>
                ))}
              </div>
            ) : null}

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
                <MerchandisingProductGrid
                  products={products.items}
                  query={q}
                  categoryId={categoryId}
                  sort={sort}
                  page={products.page}
                  listName={analyticsListName}
                />

                {(products.totalPages > 1 || products.hasNext || products.hasPrevious) ? (
                  <div className="pager pager-panel">
                    <Pagination
                      page={products.page}
                      totalPages={products.totalPages || undefined}
                      hasNext={products.hasNext}
                      hasPrevious={products.hasPrevious}
                      buildHref={(nextPage) => buildPageHref(sp, nextPage)}
                    />
                  </div>
                ) : null}
              </>
            )}
          </div>
        </div>
      </section>
    </>
  )
}
