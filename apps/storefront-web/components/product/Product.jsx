'use client'

import { useState, useRef } from 'react'
import Link from 'next/link'

/**
 * ProductCard — Modern product card with image, badges, quick-view, wishlist.
 */
export function ProductCard({
  product,
  onQuickView,
  onWishlist,
  wishListed = false,
  showQuickView = true,
  className = ''
}) {
  const { id, name, slug, price, compareAtPrice, imageUrl, category, badges = [], rating, reviewCount, isNew, isTrending, isBestseller, lowStock, stockQty } = product || {}
  const href = `/products/${slug || id}`
  const hasDiscount = compareAtPrice && compareAtPrice > price
  const discountPercent = hasDiscount ? Math.round((1 - price / compareAtPrice) * 100) : 0
  const stockStatus = lowStock ? 'low-stock' : (stockQty == null || stockQty > 0) ? '' : 'out-of-stock'
  const stockLabel = lowStock ? 'Low stock' : (stockQty == null || stockQty > 0) ? 'In stock' : 'Out of stock'

  return (
    <div className={`product-card catalog-card ${className}`}>
      <Link href={href} className="product-visual" style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : {}}>
        {/* Badges */}
        <div className="product-badges-overlay">
          {badges.map((b, i) => (
            <span key={i} className={`product-badge ${b.type || ''}`}>{b.label}</span>
          ))}
          {hasDiscount && !badges.some(b => b.type === 'sale') && <span className="product-badge sale">Sale</span>}
          {isNew && <span className="product-badge new">New</span>}
          {isTrending && <span className="product-badge trending">Trending</span>}
          {isBestseller && <span className="product-badge bestseller">Bestseller</span>}
        </div>
        
        {/* Quick Actions */}
        <div className="product-card-actions">
          {showQuickView && onQuickView && (
            <button
              type="button"
              className="product-action-btn"
              onClick={(e) => { e.preventDefault(); onQuickView(product) }}
              aria-label="Quick view"
            >👁</button>
          )}
          {onWishlist && (
            <button
              type="button"
              className={`product-action-btn ${wishListed ? 'active' : ''}`}
              onClick={(e) => { e.preventDefault(); onWishlist(product) }}
              aria-label={wishListed ? 'Remove from wishlist' : 'Add to wishlist'}
            >
              {wishListed ? '♥' : '♡'}
            </button>
          )}
        </div>
      </Link>
      
      <div className="product-meta">
        {category && <span className="product-category">{category}</span>}
        <strong><Link href={href}>{name}</Link></strong>
        
        <div className="product-price-row">
          <p>${price?.toFixed(2)}</p>
          {hasDiscount && (
            <>
              <span className="original-price">${compareAtPrice.toFixed(2)}</span>
              <span className="discount-tag">-{discountPercent}%</span>
            </>
          )}
        </div>
        
        {rating != null && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span className="review-stars">{'★'.repeat(Math.round(rating))}{'☆'.repeat(5 - Math.round(rating))}</span>
            {reviewCount != null && <span style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>({reviewCount})</span>}
          </div>
        )}
        
        <span className={`product-stock-status ${stockStatus}`}>{stockLabel}</span>
      </div>
    </div>
  )
}

/**
 * ProductGallery — Image gallery with thumbnails and arrow navigation.
 */
export function ProductGallery({ images = [], className = '' }) {
  const [activeIdx, setActiveIdx] = useState(0)
  const mainImg = images[activeIdx]

  return (
    <div className={`product-gallery ${className}`}>
      <div className="gallery-main">
        {mainImg && (
          <img src={mainImg.url || mainImg} alt={mainImg.alt || `Product image ${activeIdx + 1}`} />
        )}
        {images.length > 1 && (
          <>
            <button
              type="button"
              className="gallery-nav-btn prev"
              onClick={() => setActiveIdx((i) => (i - 1 + images.length) % images.length)}
              aria-label="Previous image"
            >‹</button>
            <button
              type="button"
              className="gallery-nav-btn next"
              onClick={() => setActiveIdx((i) => (i + 1) % images.length)}
              aria-label="Next image"
            >›</button>
          </>
        )}
      </div>
      {images.length > 1 && (
        <div className="gallery-thumbs">
          {images.map((img, i) => (
            <button
              key={i}
              type="button"
              className={`gallery-thumb ${i === activeIdx ? 'active' : ''}`}
              onClick={() => setActiveIdx(i)}
              aria-label={`View image ${i + 1}`}
            >
              <img src={img.url || img} alt={img.alt || `Thumbnail ${i + 1}`} />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * VariantSelector — Size/material/style picker.
 */
export function VariantSelector({ label, options = [], value, onChange, className = '' }) {
  return (
    <div className={`variant-selector ${className}`}>
      {label && (
        <span className="variant-label">
          {label}{value && <span style={{ fontWeight: 400, color: 'var(--muted)', marginLeft: 8 }}>{value}</span>}
        </span>
      )}
      <div className="variant-options">
        {options.map(opt => {
          const val = typeof opt === 'string' ? opt : opt.value
          const lbl = typeof opt === 'string' ? opt : opt.label
          const disabled = typeof opt !== 'string' && opt.outOfStock

          return (
            <button
              key={val}
              type="button"
              className={`variant-option ${value === val ? 'selected' : ''} ${disabled ? 'out-of-stock' : ''}`}
              onClick={() => !disabled && onChange?.(val)}
              disabled={disabled}
            >
              {lbl}
            </button>
          )
        })}
      </div>
    </div>
  )
}

/**
 * ColorSwatches — Color option picker.
 */
export function ColorSwatches({ label = 'Color', colors = [], value, onChange, className = '' }) {
  const selectedColor = colors.find(c => c.value === value)

  return (
    <div className={`variant-selector ${className}`}>
      <span className="variant-label">
        {label}{selectedColor && <span style={{ fontWeight: 400, color: 'var(--muted)', marginLeft: 8 }}>{selectedColor.label}</span>}
      </span>
      <div className="color-swatches">
        {colors.map(color => (
          <button
            key={color.value}
            type="button"
            className={`color-swatch ${value === color.value ? 'selected' : ''}`}
            style={{ backgroundColor: color.hex }}
            onClick={() => onChange?.(color.value)}
            aria-label={color.label}
            title={color.label}
          />
        ))}
      </div>
    </div>
  )
}

/**
 * DeliveryEstimator — Shows shipping/delivery estimates.
 */
export function DeliveryEstimator({ estimates = [], className = '' }) {
  return (
    <div className={`delivery-estimator ${className}`}>
      {estimates.map((est, i) => (
        <div key={i} className="delivery-row">
          <span className="delivery-icon">{est.icon || '📦'}</span>
          <span className="delivery-label">{est.label}</span>
          <span className="delivery-value">{est.value}</span>
        </div>
      ))}
    </div>
  )
}

/**
 * WishlistButton — Standalone wishlist toggle button.
 */
export function WishlistButton({ active = false, onClick, size = 'md', className = '' }) {
  return (
    <button
      type="button"
      className={`action-icon-btn ${active ? 'active' : ''} ${className}`}
      onClick={onClick}
      aria-label={active ? 'Remove from wishlist' : 'Add to wishlist'}
      style={size === 'sm' ? { width: 32, height: 32, fontSize: '0.9rem' } : size === 'lg' ? { width: 48, height: 48, fontSize: '1.3rem' } : {}}
    >
      {active ? '♥' : '♡'}
    </button>
  )
}

/**
 * CompareButton — Add to compare button.
 */
export function CompareButton({ active = false, onClick, className = '' }) {
  return (
    <button
      type="button"
      className={`action-icon-btn ${active ? 'active' : ''} ${className}`}
      onClick={onClick}
      aria-label={active ? 'Remove from compare' : 'Add to compare'}
      style={active ? { background: 'var(--accent-blue)', borderColor: 'var(--accent-blue)', color: 'white' } : {}}
    >
      ⇔
    </button>
  )
}
