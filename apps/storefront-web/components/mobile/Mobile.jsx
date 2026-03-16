'use client'

import { useState, useEffect, useRef } from 'react'

/**
 * BottomSheet — Mobile bottom drawer.
 */
export function BottomSheet({ open, onClose, title, children, className = '' }) {
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden'
    }
    return () => { document.body.style.overflow = '' }
  }, [open])

  if (!open) return null

  return (
    <div className={`bottom-sheet-overlay ${className}`} onClick={onClose}>
      <div className="bottom-sheet" onClick={e => e.stopPropagation()}>
        <div className="bottom-sheet-handle" />
        {title && (
          <div className="bottom-sheet-header">
            <strong>{title}</strong>
            <button type="button" className="modal-close" onClick={onClose} aria-label="Close">✕</button>
          </div>
        )}
        <div className="bottom-sheet-body">{children}</div>
      </div>
    </div>
  )
}

/**
 * StickyAddToCart — Fixed bar at bottom for product pages.
 */
export function StickyAddToCart({ product, onAdd, visible = true, className = '' }) {
  if (!visible) return null

  return (
    <div className={`sticky-atc ${className}`}>
      <div className="sticky-atc-info">
        <strong style={{ fontSize: '0.9rem' }}>{product?.name}</strong>
        <span style={{ fontWeight: 700 }}>${product?.price?.toFixed(2)}</span>
      </div>
      <button type="button" className="button primary" onClick={onAdd}>
        Add to Cart
      </button>
    </div>
  )
}

/**
 * SkipToContent — Accessibility skip link.
 */
export function SkipToContent({ targetId = 'main-content', label = 'Skip to content' }) {
  return (
    <a href={`#${targetId}`} className="skip-to-content">
      {label}
    </a>
  )
}

/**
 * CookieConsent — Cookie consent banner.
 */
export function CookieConsent({ onAccept, onDecline, className = '' }) {
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    const stored = localStorage.getItem('noura_cookie_consent')
    if (stored) setDismissed(true)
  }, [])

  if (dismissed) return null

  const accept = () => {
    localStorage.setItem('noura_cookie_consent', 'accepted')
    setDismissed(true)
    onAccept?.()
  }

  const decline = () => {
    localStorage.setItem('noura_cookie_consent', 'declined')
    setDismissed(true)
    onDecline?.()
  }

  return (
    <div className={`cookie-banner ${className}`}>
      <p style={{ margin: 0, fontSize: '0.875rem' }}>
        We use cookies to enhance your experience. By continuing to visit this site you agree to our use of cookies.
      </p>
      <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
        <button type="button" className="button white sm" onClick={accept}>Accept</button>
        <button type="button" className="button sm" onClick={decline} style={{ background: 'transparent', borderColor: 'rgba(255,255,255,0.4)', color: 'white' }}>Decline</button>
      </div>
    </div>
  )
}

/**
 * FocusTrap — Traps keyboard focus within children (for modals/drawers).
 */
export function FocusTrap({ active = true, children }) {
  const ref = useRef(null)

  useEffect(() => {
    if (!active || !ref.current) return
    const el = ref.current
    const focusable = el.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    )
    if (focusable.length === 0) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    const handler = (e) => {
      if (e.key !== 'Tab') return
      if (e.shiftKey) {
        if (document.activeElement === first) { e.preventDefault(); last.focus() }
      } else {
        if (document.activeElement === last) { e.preventDefault(); first.focus() }
      }
    }

    el.addEventListener('keydown', handler)
    first.focus()
    return () => el.removeEventListener('keydown', handler)
  }, [active])

  return <div ref={ref}>{children}</div>
}
