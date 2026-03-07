import Link from 'next/link'
import { notFound } from 'next/navigation'
import { getAvailability, getProduct } from '@/lib/api'
import { formatCurrency, formatMeasure } from '@/lib/format'

export default async function ProductDetailPage({ params }) {
  let product
  let availability

  try {
    ;[product, availability] = await Promise.all([
      getProduct(params.id),
      getAvailability(params.id)
    ])
  } catch (error) {
    notFound()
  }

  return (
    <section className="section stack-gap">
      <Link href="/products" className="inline-link">
        Back to catalog
      </Link>

      <article className="product-detail panel">
        <div
          className="detail-visual"
          style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
        >
          {!product.imageUrl && <span>{product.categoryName || 'Catalog product'}</span>}
        </div>

        <div className="detail-copy">
          <span className="eyebrow">{product.categoryName || 'Storefront product'}</span>
          <h1>{product.name}</h1>
          <p className="detail-price">{formatCurrency(product.price)}</p>

          <div className="detail-badges">
            <span className="badge">{availability.active ? 'Active listing' : 'Inactive'}</span>
            <span className="badge">
              {availability.allowNegativeStock
                ? 'Backorder-friendly'
                : availability.lowStock
                ? 'Low stock'
                : `Stock ${availability.stockQty ?? 0}`}
            </span>
          </div>

          <dl className="spec-grid">
            <div>
              <dt>SKU</dt>
              <dd>{product.sku || 'N/A'}</dd>
            </div>
            <div>
              <dt>Barcode</dt>
              <dd>{product.barcode || 'N/A'}</dd>
            </div>
            <div>
              <dt>Base unit</dt>
              <dd>{product.baseUnitName || 'piece'}</dd>
            </div>
            <div>
              <dt>Weight</dt>
              <dd>{formatMeasure(product.weightValue, product.weightUnit)}</dd>
            </div>
            <div>
              <dt>Length</dt>
              <dd>{formatMeasure(product.lengthValue, product.lengthUnit)}</dd>
            </div>
            <div>
              <dt>Width</dt>
              <dd>{formatMeasure(product.widthValue, product.widthUnit)}</dd>
            </div>
            <div>
              <dt>Height</dt>
              <dd>{formatMeasure(product.heightValue, product.heightUnit)}</dd>
            </div>
            <div>
              <dt>Stock threshold</dt>
              <dd>{product.lowStockThreshold ?? 'N/A'}</dd>
            </div>
          </dl>

          {product.boxSpecifications ? (
            <div className="detail-block">
              <h2>Packaging notes</h2>
              <p>{product.boxSpecifications}</p>
            </div>
          ) : null}

          <div className="detail-block">
            <h2>Available sell units</h2>
            {product.units.length === 0 ? (
              <p>No alternate sell units have been published yet.</p>
            ) : (
              <div className="unit-list">
                {product.units.map((unit) => (
                  <div key={unit.id} className="unit-card">
                    <strong>{unit.name}</strong>
                    <span>{unit.abbreviation || 'standard unit'}</span>
                    <small>Conversion to base: {unit.conversionToBase}</small>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </article>
    </section>
  )
}
