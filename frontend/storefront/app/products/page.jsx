import Link from 'next/link'
import { getCategories, getProducts } from '@/lib/api'
import { formatCurrency } from '@/lib/format'

function pickFirst(value, fallback = '') {
  if (Array.isArray(value)) {
    return value[0] || fallback
  }
  return value || fallback
}

function buildPageHref(searchParams, nextPage) {
  const params = new URLSearchParams()
  const q = pickFirst(searchParams?.q)
  const categoryId = pickFirst(searchParams?.categoryId)
  const sort = pickFirst(searchParams?.sort, 'featured')

  if (q) params.set('q', q)
  if (categoryId) params.set('categoryId', categoryId)
  if (sort) params.set('sort', sort)
  params.set('page', String(nextPage))

  return `/products?${params.toString()}`
}

export default async function ProductsPage({ searchParams }) {
  const q = pickFirst(searchParams?.q)
  const categoryId = pickFirst(searchParams?.categoryId)
  const sort = pickFirst(searchParams?.sort, 'featured')
  const page = Number.parseInt(pickFirst(searchParams?.page, '0'), 10) || 0

  let categories = []
  let products = { items: [], page: 0, hasNext: false, hasPrevious: false }
  let apiUnavailable = false

  try {
    ;[categories, products] = await Promise.all([
      getCategories(),
      getProducts({
        q,
        categoryId,
        sort,
        page,
        size: 12
      })
    ])
  } catch (error) {
    apiUnavailable = true
  }

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Catalog</span>
          <h1>Customer-facing product browse</h1>
        </div>
      </div>

      <form action="/products" className="panel filter-bar">
        <input type="text" name="q" placeholder="Search by name, SKU, or barcode" defaultValue={q} />
        <select name="categoryId" defaultValue={categoryId || ''}>
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={String(category.id)}>
              {category.name}
            </option>
          ))}
        </select>
        <select name="sort" defaultValue={sort}>
          <option value="featured">Featured</option>
          <option value="name">Name</option>
          <option value="priceAsc">Price low to high</option>
          <option value="priceDesc">Price high to low</option>
        </select>
        <button type="submit" className="button primary">
          Apply
        </button>
      </form>

      {apiUnavailable ? (
        <div className="panel notice">
          <h2>Catalog API unavailable</h2>
          <p>Run the backend on <code>http://localhost:8080</code> to populate this page.</p>
        </div>
      ) : (
        <>
          <div className="product-grid">
            {products.items.map((product) => (
              <Link key={product.id} href={`/products/${product.id}`} className="product-card">
                <div
                  className="product-visual"
                  style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
                >
                  {!product.imageUrl && <span>{product.categoryName || 'Catalog'}</span>}
                </div>
                <div className="product-meta">
                  <span className="product-category">{product.categoryName || 'Uncategorized'}</span>
                  <strong>{product.name}</strong>
                  <p>{formatCurrency(product.price)}</p>
                  <small>
                    {product.allowNegativeStock
                      ? 'Flexible fulfillment'
                      : product.lowStock
                      ? 'Low stock'
                      : `Stock ${product.stockQty ?? 0}`}
                  </small>
                </div>
              </Link>
            ))}
          </div>

          <div className="pager">
            {products.hasPrevious ? (
              <Link href={buildPageHref(searchParams, products.page - 1)} className="button ghost">
                Previous
              </Link>
            ) : (
              <span />
            )}
            <span>Page {products.page + 1}</span>
            {products.hasNext ? (
              <Link href={buildPageHref(searchParams, products.page + 1)} className="button ghost">
                Next
              </Link>
            ) : (
              <span />
            )}
          </div>
        </>
      )}
    </section>
  )
}
