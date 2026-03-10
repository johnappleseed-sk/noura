'use client'

import Link from 'next/link'
import { useEffect, useMemo, useRef } from 'react'
import { formatCurrency } from '@/lib/format'
import { trackAnalyticsEvent } from '@/lib/analytics'
import { setLastProductClickAttribution } from '@/lib/attribution'

export default function MerchandisingProductGrid({
  products,
  query,
  categoryId,
  sort,
  page,
  listName = 'catalog-grid'
}) {
  const containerRef = useRef(null)
  const impressedIdsRef = useRef(new Set())
  const productMap = useMemo(
    () => new Map(products.map((product, index) => [String(product.id), { product, slot: index }])),
    [products]
  )

  useEffect(() => {
    impressedIdsRef.current = new Set()
  }, [products, query, categoryId, sort, page, listName])

  useEffect(() => {
    if (!containerRef.current || products.length === 0) {
      return undefined
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting || entry.intersectionRatio < 0.35) {
            return
          }

          const productId = entry.target.getAttribute('data-product-id')
          if (!productId || impressedIdsRef.current.has(productId)) {
            return
          }

          impressedIdsRef.current.add(productId)
          const payload = productMap.get(productId)
          if (payload) {
            trackAnalyticsEvent({
              eventType: 'PRODUCT_IMPRESSION',
              productId,
              metadata: {
                listName,
                sort,
                query: query || null,
                categoryId: categoryId || null,
                page,
                slot: payload.slot,
                merchandisingScore: payload.product.merchandisingScore || 0
              }
            })
          }

          observer.unobserve(entry.target)
        })
      },
      { threshold: [0.35] }
    )

    const cards = containerRef.current.querySelectorAll('[data-product-id]')
    cards.forEach((card) => observer.observe(card))

    return () => observer.disconnect()
  }, [products, productMap, query, categoryId, sort, page, listName])

  function trackProductClick(product, slot) {
    setLastProductClickAttribution({
      productId: String(product.id),
      listName,
      slot,
      pagePath: typeof window !== 'undefined' ? window.location.pathname : null
    })

    trackAnalyticsEvent({
      eventType: 'PRODUCT_CLICK',
      productId: String(product.id),
      metadata: {
        listName,
        sort,
        query: query || null,
        categoryId: categoryId || null,
        page,
        slot,
        merchandisingScore: product.merchandisingScore || 0
      }
    })
  }

  return (
    <div ref={containerRef} className="product-grid catalog-product-grid">
      {products.map((product, index) => {
        const hasDiscount = product.compareAtPrice && product.compareAtPrice > product.price
        const discountPercent = hasDiscount ? Math.round((1 - product.price / product.compareAtPrice) * 100) : 0
        const stockStatus = product.lowStock ? 'low-stock' : product.stockQty > 0 ? '' : 'out-of-stock'
        const stockLabel = product.lowStock ? 'Low stock' : product.stockQty > 0 ? 'In stock' : 'Out of stock'

        return (
          <Link
            key={product.id}
            href={`/products/${product.id}`}
            className="product-card catalog-card"
            data-product-id={product.id}
            onClick={() => trackProductClick(product, index)}
          >
            <div className="product-visual" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}>
              {!product.imageUrl && <span>{product.categoryName || 'Product'}</span>}

              <div className="product-badges-overlay">
                {hasDiscount && <span className="product-badge sale">Sale</span>}
                {product.isNew && <span className="product-badge new">New</span>}
                {product.isTrending && <span className="product-badge trending">Trending</span>}
                {product.isBestseller && <span className="product-badge bestseller">Bestseller</span>}
              </div>

              <div className="product-card-actions">
                <span className="product-action-btn" aria-hidden="true">👁</span>
                <span className="product-action-btn" aria-hidden="true">♡</span>
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
  )
}
