'use client'

import { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import Link from 'next/link'
import { Carousel } from './Carousel'
import { formatCurrency } from '@/lib/format'
import { trackAnalyticsEvent } from '@/lib/analytics'
import { setLastProductClickAttribution } from '@/lib/attribution'

/**
 * ProductSlide - Individual product card for carousel
 */
function ProductSlide({
  product,
  onQuickView,
  onAddToCart,
  onWishlist,
  isWishlisted = false,
  variant = 'default',
  isLoaded = true,
  listName,
  pagePath = '/',
  slot,
}) {
  const {
    id,
    name,
    slug,
    price,
    compareAtPrice,
    imageUrl,
    categoryName,
    rating,
    reviewCount,
    stockQty,
    lowStock,
    isNew,
    isTrending,
    isBestseller,
    badges = [],
  } = product || {}

  const href = `/products/${slug || id}`
  const cardRef = useRef(null)
  const hasTrackedImpression = useRef(false)
  const hasDiscount = compareAtPrice && compareAtPrice > price
  const discountPercent = hasDiscount ? Math.round((1 - price / compareAtPrice) * 100) : 0
  const stockStatus = lowStock ? 'low-stock' : (stockQty == null || stockQty > 0) ? '' : 'out-of-stock'
  const stockLabel = lowStock ? 'Low stock' : (stockQty == null || stockQty > 0) ? 'In stock' : 'Out of stock'

  useEffect(() => {
    if (!id || !listName || !cardRef.current || hasTrackedImpression.current) {
      return undefined
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting || hasTrackedImpression.current) {
            return
          }

          hasTrackedImpression.current = true
          observer.disconnect()

          trackAnalyticsEvent({
            eventType: 'PRODUCT_IMPRESSION',
            productId: String(id),
            pagePath,
            metadata: {
              listName,
              slot,
              productName: name,
              categoryName,
              merchandisingScore: product?.merchandisingScore ?? null,
            },
          })
        })
      },
      { threshold: 0.35 }
    )

    observer.observe(cardRef.current)
    return () => observer.disconnect()
  }, [id, listName, pagePath, slot, name, categoryName, product])

  const handleProductClick = useCallback(() => {
    if (!id || !listName) {
      return
    }

    setLastProductClickAttribution({
      productId: String(id),
      listName,
      slot,
      pagePath
    })

    trackAnalyticsEvent({
      eventType: 'PRODUCT_CLICK',
      productId: String(id),
      pagePath,
      metadata: {
        listName,
        slot,
        productName: name,
        categoryName,
        merchandisingScore: product?.merchandisingScore ?? null,
      },
    })
  }, [id, listName, pagePath, slot, name, categoryName, product])

  if (!isLoaded) {
    return (
      <div className="product-carousel-card product-carousel-card--skeleton">
        <div className="product-carousel-card__image-skeleton" />
        <div className="product-carousel-card__content-skeleton">
          <div className="skeleton-line skeleton-line--sm" />
          <div className="skeleton-line skeleton-line--lg" />
          <div className="skeleton-line skeleton-line--md" />
        </div>
      </div>
    )
  }

  return (
    <div
      ref={cardRef}
      className={`product-carousel-card product-carousel-card--${variant}`}
      data-product-id={id ? String(id) : undefined}
      data-product-slot={slot}
      data-product-list={listName}
    >
      <Link href={href} className="product-carousel-card__link" onClick={handleProductClick}>
        {/* Image */}
        <div
          className="product-carousel-card__image"
          style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined}
        >
          {!imageUrl && (
            <span className="product-carousel-card__placeholder">{categoryName || 'Product'}</span>
          )}

          {/* Badges */}
          <div className="product-carousel-card__badges">
            {badges.map((badge, i) => (
              <span key={i} className={`product-badge ${badge.type || ''}`}>{badge.label}</span>
            ))}
            {hasDiscount && !badges.some(b => b.type === 'sale') && (
              <span className="product-badge sale">-{discountPercent}%</span>
            )}
            {isNew && <span className="product-badge new">New</span>}
            {isTrending && <span className="product-badge trending">Trending</span>}
            {isBestseller && <span className="product-badge bestseller">Bestseller</span>}
          </div>

          {/* Quick Actions */}
          <div className="product-carousel-card__actions">
            {onQuickView && (
              <button
                type="button"
                className="product-carousel-card__action"
                onClick={(e) => { e.preventDefault(); onQuickView(product) }}
                aria-label="Quick view"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            )}
            {onWishlist && (
              <button
                type="button"
                className={`product-carousel-card__action ${isWishlisted ? 'product-carousel-card__action--active' : ''}`}
                onClick={(e) => { e.preventDefault(); onWishlist(product) }}
                aria-label={isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
              >
                <svg viewBox="0 0 24 24" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
              </button>
            )}
          </div>
        </div>

        {/* Content */}
        <div className="product-carousel-card__content">
          <span className="product-carousel-card__category">{categoryName || 'Uncategorized'}</span>
          <h3 className="product-carousel-card__title">{name}</h3>

          {/* Rating */}
          {rating != null && (
            <div className="product-carousel-card__rating">
              <span className="product-carousel-card__stars">
                {'★'.repeat(Math.round(rating))}{'☆'.repeat(5 - Math.round(rating))}
              </span>
              {reviewCount != null && (
                <span className="product-carousel-card__review-count">({reviewCount})</span>
              )}
            </div>
          )}

          {/* Price */}
          <div className="product-carousel-card__price-row">
            <span className="product-carousel-card__price">
              {typeof formatCurrency === 'function' ? formatCurrency(price) : `$${price?.toFixed(2)}`}
            </span>
            {hasDiscount && (
              <span className="product-carousel-card__original-price">
                {typeof formatCurrency === 'function' ? formatCurrency(compareAtPrice) : `$${compareAtPrice?.toFixed(2)}`}
              </span>
            )}
          </div>

          {/* Stock Status */}
          <span className={`product-carousel-card__stock ${stockStatus}`}>
            {stockLabel}
          </span>
        </div>
      </Link>

      {/* Add to Cart */}
      {onAddToCart && (
        <button
          type="button"
          className="product-carousel-card__add-to-cart"
          onClick={() => onAddToCart(product)}
          disabled={stockQty === 0}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
          Add to Cart
        </button>
      )}
    </div>
  )
}

/**
 * ProductCarousel - Carousel for product listings
 * 
 * @param {Object} props
 * @param {string} props.title - Section title
 * @param {string} props.subtitle - Section subtitle (eyebrow)
 * @param {string} props.description - Section description
 * @param {Array} props.products - Array of product objects
 * @param {string} props.viewAllLink - Link for "View All" button
 * @param {string} props.viewAllText - Text for "View All" button
 * @param {Function} props.onQuickView - Quick view callback
 * @param {Function} props.onAddToCart - Add to cart callback
 * @param {Function} props.onWishlist - Wishlist callback
 * @param {Set} props.wishlistedIds - Set of wishlisted product IDs
 * @param {number} props.slidesToShow - Number of products visible
 * @param {boolean} props.autoplay - Enable autoplay
 * @param {string} props.variant - A/B test variant
 * @param {string} props.cardVariant - Product card variant
 * @param {boolean} props.loading - Loading state
 * @param {string} props.className - Additional CSS classes
 */
export function ProductCarousel({
  title,
  subtitle,
  description,
  products = [],
  viewAllLink,
  viewAllText = 'View All',
  onQuickView,
  onAddToCart,
  onWishlist,
  wishlistedIds = new Set(),
  slidesToShow = 4,
  slidesToShowTablet = 2,
  slidesToShowMobile = 1,
  autoplay = false,
  autoplayInterval = 5000,
  variant = 'default',
  cardVariant = 'default',
  loading = false,
  emptyMessage = 'No products available',
  analyticsListName,
  analyticsPagePath = '/',
  className = '',
}) {
  const [responsiveSlidesToShow, setResponsiveSlidesToShow] = useState(slidesToShow)
  const containerRef = useRef(null)

  // Responsive slides calculation
  useEffect(() => {
    const updateSlidesToShow = () => {
      const width = window.innerWidth
      if (width < 640) {
        setResponsiveSlidesToShow(slidesToShowMobile)
      } else if (width < 1024) {
        setResponsiveSlidesToShow(slidesToShowTablet)
      } else {
        setResponsiveSlidesToShow(slidesToShow)
      }
    }

    updateSlidesToShow()

    // Debounced resize handler
    let timeoutId
    const handleResize = () => {
      clearTimeout(timeoutId)
      timeoutId = setTimeout(updateSlidesToShow, 150)
    }

    window.addEventListener('resize', handleResize)
    return () => {
      window.removeEventListener('resize', handleResize)
      clearTimeout(timeoutId)
    }
  }, [slidesToShow, slidesToShowTablet, slidesToShowMobile])

  // A/B test variants
  const variantStyles = {
    default: '',
    compact: 'product-carousel--compact',
    featured: 'product-carousel--featured',
    minimal: 'product-carousel--minimal',
  }

  // Loading skeleton
  if (loading) {
    return (
      <section className={`product-carousel ${variantStyles[variant] || ''} product-carousel--loading ${className}`}>
        <div className="product-carousel__header">
          <div className="skeleton-line skeleton-line--eyebrow" />
          <div className="skeleton-line skeleton-line--title" />
        </div>
        <div className="product-carousel__skeleton-grid">
          {Array.from({ length: responsiveSlidesToShow }).map((_, i) => (
            <ProductSlide key={i} product={{}} isLoaded={false} />
          ))}
        </div>
      </section>
    )
  }

  // Empty state
  if (!products.length) {
    return (
      <section className={`product-carousel ${variantStyles[variant] || ''} product-carousel--empty ${className}`}>
        {(title || subtitle) && (
          <div className="product-carousel__header">
            {subtitle && <span className="product-carousel__subtitle">{subtitle}</span>}
            {title && <h2 className="product-carousel__title">{title}</h2>}
          </div>
        )}
        <div className="product-carousel__empty">
          <p>{emptyMessage}</p>
        </div>
      </section>
    )
  }

  return (
    <section
      ref={containerRef}
      className={`product-carousel ${variantStyles[variant] || ''} ${className}`}
    >
      {/* Header */}
      {(title || subtitle || viewAllLink) && (
        <div className="product-carousel__header">
          <div className="product-carousel__header-text">
            {subtitle && <span className="product-carousel__subtitle">{subtitle}</span>}
            {title && <h2 className="product-carousel__title">{title}</h2>}
            {description && <p className="product-carousel__description">{description}</p>}
          </div>
          {viewAllLink && (
            <Link href={viewAllLink} className="product-carousel__view-all">
              {viewAllText}
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </Link>
          )}
        </div>
      )}

      {/* Carousel */}
      <div className="product-carousel__container">
        <Carousel
          autoplay={autoplay}
          autoplayInterval={autoplayInterval}
          loop={products.length > responsiveSlidesToShow}
          showArrows={products.length > responsiveSlidesToShow}
          showDots={false}
          arrowStyle="product"
          slidesToShow={responsiveSlidesToShow}
          slidesToScroll={1}
          gap={20}
          pauseOnHover={true}
          ariaLabel={`${title || 'Product'} carousel`}
        >
          {products.map((product, index) => (
            <ProductSlide
              key={product.id || index}
              product={product}
              onQuickView={onQuickView}
              onAddToCart={onAddToCart}
              onWishlist={onWishlist}
              isWishlisted={wishlistedIds.has(product.id)}
              variant={cardVariant}
              listName={analyticsListName}
              pagePath={analyticsPagePath}
              slot={index}
            />
          ))}
        </Carousel>
      </div>
    </section>
  )
}

/**
 * PersonalizedCarousel - Product carousel with personalization support
 */
export function PersonalizedCarousel({
  userId,
  type = 'browsing', // 'browsing' | 'purchased' | 'recommended'
  fallbackProducts = [],
  ...props
}) {
  const [products, setProducts] = useState(fallbackProducts)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // In a real implementation, fetch personalized products from API
    // For now, use fallback products
    const fetchPersonalized = async () => {
      try {
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // Check localStorage for browsing history
        const browsingHistory = localStorage.getItem('noura_browsing_history')
        if (browsingHistory) {
          // In real app, send these IDs to backend for personalized recommendations
          const history = JSON.parse(browsingHistory)
          console.log('Browsing history:', history)
        }
        
        // Use fallback products
        setProducts(fallbackProducts)
      } catch (error) {
        console.error('Failed to fetch personalized products:', error)
        setProducts(fallbackProducts)
      } finally {
        setLoading(false)
      }
    }

    fetchPersonalized()
  }, [userId, type, fallbackProducts])

  const titleMap = {
    browsing: 'Based on Your Browsing',
    purchased: 'Buy Again',
    recommended: 'Recommended for You',
  }

  return (
    <ProductCarousel
      title={props.title || titleMap[type]}
      subtitle={props.subtitle || 'Personalized'}
      products={products}
      loading={loading}
      {...props}
    />
  )
}

export default ProductCarousel
