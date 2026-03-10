'use client'

import { useEffect, useMemo, useRef } from 'react'
import Link from 'next/link'
import Badge from '@/components/ui/Badge'
import { formatCurrency } from '@/lib/format'
import { trackAnalyticsEvent } from '@/lib/analytics'
import { setLastProductClickAttribution } from '@/lib/attribution'

export default function DealsProductGrid({
  deals = [],
  listName = 'deals-grid',
  pagePath = '/deals',
}) {
  const containerRef = useRef(null)
  const observedIds = useRef(new Set())
  const dealMap = useMemo(
    () =>
      new Map(
        deals.map((deal, index) => [String(deal.productId || deal.id), { deal, slot: index }])
      ),
    [deals]
  )

  useEffect(() => {
    if (!containerRef.current || deals.length === 0) {
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

          const trackedDeal = dealMap.get(productId)
          if (!trackedDeal) {
            return
          }

          observedIds.current.add(productId)
          const { deal, slot } = trackedDeal

          trackAnalyticsEvent({
            eventType: 'PRODUCT_IMPRESSION',
            productId,
            pagePath,
            metadata: {
              listName,
              slot,
              productName: deal.productName || deal.name,
              categoryName: deal.categoryName,
              discountPercent: deal.discountPercent ?? null,
              salePrice: deal.salePrice ?? deal.price ?? null,
              originalPrice: deal.originalPrice ?? null,
            },
          })
        })
      },
      { threshold: 0.35 }
    )

    const nodes = containerRef.current.querySelectorAll('[data-product-id]')
    nodes.forEach((node) => observer.observe(node))

    return () => observer.disconnect()
  }, [deals, dealMap, listName, pagePath])

  const handleClick = (deal, slot) => {
    const productId = deal.productId || deal.id
    if (!productId) {
      return
    }

    setLastProductClickAttribution({
      productId: String(productId),
      listName,
      slot,
      pagePath
    })

    trackAnalyticsEvent({
      eventType: 'PRODUCT_CLICK',
      productId: String(productId),
      pagePath,
      metadata: {
        listName,
        slot,
        productName: deal.productName || deal.name,
        categoryName: deal.categoryName,
        discountPercent: deal.discountPercent ?? null,
        salePrice: deal.salePrice ?? deal.price ?? null,
        originalPrice: deal.originalPrice ?? null,
      },
    })
  }

  return (
    <div ref={containerRef} className="deals-grid">
      {deals.map((deal, index) => {
        const productId = deal.productId || deal.id

        return (
          <Link
            key={productId}
            href={`/products/${productId}`}
            className="product-card"
            data-product-id={String(productId)}
            data-product-slot={index}
            onClick={() => handleClick(deal, index)}
          >
            <div className="product-visual" style={deal.imageUrl ? { backgroundImage: `url(${deal.imageUrl})` } : undefined}>
              {!deal.imageUrl && <span>{deal.categoryName || 'Deal'}</span>}
              {deal.discountPercent && (
                <Badge variant="success" style={{ position: 'absolute', top: 12, right: 12 }}>
                  {deal.discountPercent}% OFF
                </Badge>
              )}
            </div>
            <div className="product-meta">
              <span className="product-category">{deal.categoryName || 'Special Offer'}</span>
              <strong>{deal.productName || deal.name}</strong>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                {deal.originalPrice && (
                  <span style={{ textDecoration: 'line-through', color: 'var(--muted)', fontSize: '0.85rem' }}>
                    {formatCurrency(deal.originalPrice)}
                  </span>
                )}
                <span style={{ color: 'var(--danger)', fontWeight: 700 }}>
                  {formatCurrency(deal.salePrice || deal.price || 0)}
                </span>
              </div>
            </div>
          </Link>
        )
      })}
    </div>
  )
}
