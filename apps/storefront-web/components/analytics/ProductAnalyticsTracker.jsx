'use client'

import { useEffect } from 'react'
import { trackAnalyticsEvent } from '@/lib/analytics'

export default function ProductAnalyticsTracker({ productId }) {
  useEffect(() => {
    if (!productId) return
    trackAnalyticsEvent({
      eventType: 'PRODUCT_VIEW',
      productId: String(productId),
      pagePath: typeof window !== 'undefined' ? window.location.pathname : '/products'
    })
  }, [productId])

  return null
}
