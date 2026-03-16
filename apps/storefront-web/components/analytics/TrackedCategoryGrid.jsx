'use client'

import { useEffect, useMemo, useRef } from 'react'
import Link from 'next/link'
import { trackAnalyticsEvent } from '@/lib/analytics'

export default function TrackedCategoryGrid({
  categories = [],
  listName = 'home-category-grid',
  pagePath = '/',
  className = 'category-grid',
  style,
}) {
  const containerRef = useRef(null)
  const impressedIdsRef = useRef(new Set())
  const categoryMap = useMemo(
    () => new Map(categories.map((category, index) => [String(category.id), { category, slot: index }])),
    [categories]
  )

  useEffect(() => {
    impressedIdsRef.current = new Set()
  }, [categories, listName, pagePath])

  useEffect(() => {
    if (!containerRef.current || categories.length === 0) {
      return undefined
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting || entry.intersectionRatio < 0.35) {
            return
          }

          const categoryId = entry.target.getAttribute('data-category-id')
          if (!categoryId || impressedIdsRef.current.has(categoryId)) {
            return
          }

          impressedIdsRef.current.add(categoryId)
          const payload = categoryMap.get(categoryId)
          if (payload) {
            trackAnalyticsEvent({
              eventType: 'CATEGORY_IMPRESSION',
              pagePath,
              metadata: {
                listName,
                slot: payload.slot,
                categoryId: String(payload.category.id),
                categoryName: payload.category.name,
                productCount: payload.category.productCount ?? null
              }
            })
          }

          observer.unobserve(entry.target)
        })
      },
      { threshold: [0.35] }
    )

    const cards = containerRef.current.querySelectorAll('[data-category-id]')
    cards.forEach((card) => observer.observe(card))

    return () => observer.disconnect()
  }, [categories, categoryMap, listName, pagePath])

  function trackCategoryClick(category, slot) {
    trackAnalyticsEvent({
      eventType: 'CATEGORY_CLICK',
      pagePath,
      metadata: {
        listName,
        slot,
        categoryId: String(category.id),
        categoryName: category.name,
        productCount: category.productCount ?? null
      }
    })
  }

  return (
    <div ref={containerRef} className={className} style={style}>
      {categories.map((category, index) => (
        <Link
          key={category.id}
          href={`/products?categoryId=${category.id}`}
          className="category-card"
          data-category-id={category.id}
          onClick={() => trackCategoryClick(category, index)}
        >
          <span className="category-count">{category.productCount} products</span>
          <strong>{category.name}</strong>
          <p>{category.description || 'Explore this collection'}</p>
        </Link>
      ))}
    </div>
  )
}
