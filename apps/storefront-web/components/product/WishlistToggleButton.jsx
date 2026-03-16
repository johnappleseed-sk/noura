'use client'

import { useEffect, useState } from 'react'
import { isWishlisted, subscribeWishlist, toggleWishlistItem } from '@/lib/wishlist'

/**
 * Product wishlist toggle button that keeps state in sync across all tabs/components.
 */
export default function WishlistToggleButton({ product, className = '' }) {
  const productId = String(product?.id || '')
  const [active, setActive] = useState(() => isWishlisted(productId))

  useEffect(() => {
    setActive(isWishlisted(productId))
    if (!productId) {
      return undefined
    }
    return subscribeWishlist(() => setActive(isWishlisted(productId)))
  }, [productId])

  const handleClick = (event) => {
    event.preventDefault()
    event.stopPropagation()
    if (!productId) {
      return
    }

    const next = toggleWishlistItem({
      id: productId,
      name: product?.name || '',
      price: product?.price || 0,
      imageUrl: product?.imageUrl || null,
      categoryName: product?.categoryName || product?.category || null
    })
    setActive(next)
  }

  return (
    <button
      type="button"
      className={`product-action-btn ${active ? 'active' : ''} ${className}`.trim()}
      onClick={handleClick}
      aria-label={active ? 'Remove from wishlist' : 'Add to wishlist'}
      title={active ? 'Remove from wishlist' : 'Add to wishlist'}
    >
      {active ? '♥' : '♡'}
    </button>
  )
}

