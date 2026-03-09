'use client'

import { useState } from 'react'
import { CountdownTimer } from '@/components/ui/Countdown'

/**
 * PromoBanner — Full-width promotional banner.
 */
export function PromoBanner({ title, subtitle, cta, href, bgColor = 'var(--accent)', textColor = '#fff', className = '' }) {
  return (
    <div className={`promo-banner ${className}`} style={{ background: bgColor, color: textColor }}>
      <div className="promo-banner-content">
        {title && <h3 className="promo-banner-title">{title}</h3>}
        {subtitle && <p className="promo-banner-sub">{subtitle}</p>}
        {cta && href && (
          <a href={href} className="button white" style={{ marginTop: 10 }}>{cta}</a>
        )}
      </div>
    </div>
  )
}

/**
 * PromoCard — Card-style promotion/deal display.
 */
export function PromoCard({ title, description, imageUrl, badge, cta, href, className = '' }) {
  return (
    <div className={`promo-card ${className}`}>
      {imageUrl && (
        <div className="promo-card-image">
          <img src={imageUrl} alt={title || ''} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          {badge && <span className="badge accent" style={{ position: 'absolute', top: 10, left: 10 }}>{badge}</span>}
        </div>
      )}
      <div className="promo-card-body">
        {title && <h4 style={{ margin: '0 0 6px' }}>{title}</h4>}
        {description && <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--muted)' }}>{description}</p>}
        {cta && href && (
          <a href={href} className="button ghost sm" style={{ marginTop: 10 }}>{cta}</a>
        )}
      </div>
    </div>
  )
}

/**
 * FlashDeal — Time-limited deal card with countdown.
 */
export function FlashDeal({ title, originalPrice, salePrice, imageUrl, endTime, href, className = '' }) {
  const pct = originalPrice && salePrice
    ? Math.round(((originalPrice - salePrice) / originalPrice) * 100)
    : null

  return (
    <div className={`flash-deal ${className}`}>
      {imageUrl && (
        <div className="flash-deal-image">
          <img src={imageUrl} alt={title || ''} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          {pct && <span className="badge accent" style={{ position: 'absolute', top: 8, left: 8 }}>-{pct}%</span>}
        </div>
      )}
      <div className="flash-deal-body">
        {title && <h4 style={{ margin: '0 0 6px', fontSize: '0.95rem' }}>{title}</h4>}
        <div style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
          <span style={{ fontWeight: 700, fontSize: '1.1rem' }}>${salePrice?.toFixed(2)}</span>
          {originalPrice && (
            <span style={{ textDecoration: 'line-through', color: 'var(--muted)', fontSize: '0.85rem' }}>
              ${originalPrice.toFixed(2)}
            </span>
          )}
        </div>
        {endTime && (
          <div style={{ marginTop: 10 }}>
            <CountdownTimer targetDate={endTime} />
          </div>
        )}
        {href && <a href={href} className="button primary sm" style={{ marginTop: 10, textAlign: 'center', display: 'block' }}>Shop Now</a>}
      </div>
    </div>
  )
}

/**
 * CouponBanner — Inline coupon highlight.
 */
export function CouponBanner({ code, description, className = '' }) {
  const [copied, setCopied] = useState(false)

  const copy = () => {
    navigator.clipboard?.writeText(code).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  return (
    <div className={`coupon-banner ${className}`}>
      <span>{description || `Use code ${code}`}</span>
      <button type="button" className="coupon-banner-code" onClick={copy}>
        {code} {copied ? '✓' : '📋'}
      </button>
    </div>
  )
}
