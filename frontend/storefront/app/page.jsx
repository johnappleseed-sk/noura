import Link from 'next/link'
import { getCategories, getProducts } from '@/lib/api'
import { formatCurrency } from '@/lib/format'

export default async function HomePage() {
  let categories = []
  let products = { items: [] }
  let apiUnavailable = false

  try {
    ;[categories, products] = await Promise.all([
      getCategories(),
      getProducts({ size: 8 })
    ])
  } catch (error) {
    apiUnavailable = true
  }

  return (
    <>
      <section className="hero panel">
        <div className="hero-copy">
          <span className="eyebrow">Fullstack commerce kickoff</span>
          <h1>Turn the existing POS engine into a customer-ready commerce surface.</h1>
          <p className="lede">
            This storefront already reads from the shared product, category, pricing, and stock models.
            It is intentionally separate from the internal admin and cashier workflows.
          </p>
          <div className="hero-actions">
            <Link href="/products" className="button primary">
              Browse catalog
            </Link>
            <a className="button ghost" href="http://localhost:8080/api/storefront/v1/catalog/products" target="_blank" rel="noreferrer">
              Inspect API
            </a>
          </div>
        </div>
        <div className="hero-metrics">
          <div className="metric">
            <strong>{categories.length}</strong>
            <span>live categories</span>
          </div>
          <div className="metric">
            <strong>{products.items.length}</strong>
            <span>featured products</span>
          </div>
          <div className="metric">
            <strong>1</strong>
            <span>shared catalog core</span>
          </div>
        </div>
      </section>

      {apiUnavailable ? (
        <section className="panel notice">
          <h2>Backend unavailable</h2>
          <p>
            Start the Spring Boot app on <code>http://localhost:8080</code> to load live categories and products.
          </p>
        </section>
      ) : (
        <>
          <section className="section">
            <div className="section-head">
              <div>
                <span className="eyebrow">Navigation</span>
                <h2>Browse by category</h2>
              </div>
              <Link href="/products" className="inline-link">
                View full catalog
              </Link>
            </div>
            <div className="category-grid">
              {categories.map((category) => (
                <Link
                  key={category.id}
                  href={`/products?categoryId=${category.id}`}
                  className="category-card"
                >
                  <span className="category-count">{category.productCount} items</span>
                  <strong>{category.name}</strong>
                  <p>{category.description || 'Shared catalog category from the POS core.'}</p>
                </Link>
              ))}
            </div>
          </section>

          <section className="section">
            <div className="section-head">
              <div>
                <span className="eyebrow">Catalog</span>
                <h2>Featured products</h2>
              </div>
            </div>
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
                        ? 'Made to order or oversell allowed'
                        : product.lowStock
                        ? 'Low stock'
                        : `Stock ${product.stockQty ?? 0}`}
                    </small>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        </>
      )}
    </>
  )
}
