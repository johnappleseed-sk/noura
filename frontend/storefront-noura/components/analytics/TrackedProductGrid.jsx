'use client'

import { useEffect, useMemo, useRef } from 'react'
import Link from 'next/link'
import { formatCurrency } from '@/lib/format'
import { trackAnalyticsEvent } from '@/lib/analytics'
import { setLastProductClickAttribution } from '@/lib/attribution'

function renderCatalogCard(product) {
  const hasDiscount = product.compareAtPrice && product.compareAtPrice > product.price
  const discountPercent = hasDiscount ? Math.round((1 - product.price / product.compareAtPrice) * 100) : 0
  const stockStatus = product.lowStock ? 'low-stock' : product.stockQty > 0 ? '' : 'out-of-stock'
  const stockLabel = product.lowStock ? 'Low stock' : product.stockQty > 0 ? 'In stock' : 'Out of stock'

  return (
    <>
      <div className="product-visual" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}>
        {!product.imageUrl && <span>{product.categoryName || 'New'}</span>}
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
          <p>{formatCurrency(product.price ?? 0)}</p>
          {hasDiscount && (
            <>
              <span className="original-price">{formatCurrency(product.compareAtPrice)}</span>
              <span className="discount-tag">-{discountPercent}%</span>
            </>
          )}
        </div>
        <span className={`product-stock-status ${stockStatus}`}>{stockLabel}</span>
      </div>
    </>
  )
}

function renderCompactCard(product) {
  const currentPrice = product.salePrice ?? product.price ?? 0
  const compareAtPrice = product.compareAtPrice ?? product.originalPrice ?? null
  const hasDiscount = compareAtPrice && compareAtPrice > currentPrice

  return (
    <>
      <div className="product-visual" style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}>
        {!product.imageUrl && <span>{product.categoryName || 'Product'}</span>}
        {(product.isTrending || product.isBestseller || hasDiscount) && (
          <div className="product-badges-overlay">
            {hasDiscount && <span className="product-badge sale">Sale</span>}
            {product.isTrending && <span className="product-badge trending">Trending</span>}
            {product.isBestseller && <span className="product-badge bestseller">Bestseller</span>}
          </div>
        )}
      </div>
      <div className="product-meta">
        <span className="product-category">{product.categoryName || 'Product'}</span>
        <strong>{product.name}</strong>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <span>{formatCurrency(currentPrice)}</span>
          {hasDiscount && (
            <span style={{ textDecoration: 'line-through', color: 'var(--muted)', fontSize: '0.85rem' }}>
              {formatCurrency(compareAtPrice)}
            </span>
          )}
        </div>
      </div>
    </>
  )
}

export default function TrackedProductGrid({
  products = [],
  listName,
  pagePath = '/',
  layoutClassName = 'product-grid',
  variant = 'catalog',
  cardClassName,
  cardStyle,
}) {
  const containerRef = useRef(null)
  const observedIds = useRef(new Set())
  const productMap = useMemo(
    () => new Map(products.map((product, index) => [String(product.id), { product, slot: index }])),
    [products]
  )

  useEffect(() => {
    if (!containerRef.current || products.length === 0) {
      return undefined
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) {
            return
          }

          const productId = entry.target.getAttribute('data-product-id')
          if (!productId || observedIds.current.has(productId)) {
            return
          }

          const trackedProduct = productMap.get(productId)
          if (!trackedProduct) {
            return
          }

          observedIds.current.add(productId)
          const { product, slot } = trackedProduct

          trackAnalyticsEvent({
            eventType: 'PRODUCT_IMPRESSION',
            productId,
            pagePath,
            metadata: {
              listName,
              slot,
              productName: product.name,
              categoryName: product.categoryName,
              merchandisingScore: product.merchandisingScore ?? null,
            },
          })
        })
      },
      { threshold: 0.35 }
    )

    const nodes = containerRef.current.querySelectorAll('[data-product-id]')
    nodes.forEach((node) => observer.observe(node))

    return () => observer.disconnect()
  }, [products, productMap, listName, pagePath])

  const handleClick = (product, slot) => {
    if (!product?.id) {
      return
    }

    setLastProductClickAttribution({
      productId: String(product.id),
      listName,
      slot,
      pagePath
    })

    trackAnalyticsEvent({
      eventType: 'PRODUCT_CLICK',
      productId: String(product.id),
      pagePath,
      metadata: {
        listName,
        slot,
        productName: product.name,
        categoryName: product.categoryName,
        merchandisingScore: product.merchandisingScore ?? null,
      },
    })
  }

  const resolvedCardClassName = cardClassName || (variant === 'catalog' ? 'product-card catalog-card' : 'product-card')

  return (
    <div ref={containerRef} className={layoutClassName}>
      {products.map((product, index) => (
        <Link
          key={product.id || index}
          href={`/products/${product.slug || product.id}`}
          className={resolvedCardClassName}
          style={cardStyle}
          data-product-id={String(product.id)}
          data-product-slot={index}
          onClick={() => handleClick(product, index)}
        >
          {variant === 'catalog' ? renderCatalogCard(product) : renderCompactCard(product)}
        </Link>
      ))}
    </div>
  )
}
