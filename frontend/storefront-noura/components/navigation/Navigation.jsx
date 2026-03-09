'use client'

import { useState, useRef, useEffect } from 'react'
import Link from 'next/link'

/**
 * MegaMenu — Desktop dropdown mega menu for category navigation.
 */
export function MegaMenu({ items = [], className = '' }) {
  const [openId, setOpenId] = useState(null)
  const menuRef = useRef(null)

  useEffect(() => {
    const handleOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setOpenId(null)
    }
    document.addEventListener('mousedown', handleOutside)
    return () => document.removeEventListener('mousedown', handleOutside)
  }, [])

  return (
    <nav ref={menuRef} className={`mega-menu ${className}`} role="navigation">
      <ul className="mega-menu-bar">
        {items.map((item) => (
          <li
            key={item.id}
            className={`mega-menu-item ${openId === item.id ? 'is-open' : ''}`}
            onMouseEnter={() => setOpenId(item.id)}
            onMouseLeave={() => setOpenId(null)}
          >
            {item.href ? (
              <Link href={item.href} className="mega-menu-trigger">
                {item.label}
                {item.children?.length > 0 && <span className="mega-chevron">▾</span>}
              </Link>
            ) : (
              <button
                type="button"
                className="mega-menu-trigger"
                aria-expanded={openId === item.id}
                onClick={() => setOpenId(openId === item.id ? null : item.id)}
              >
                {item.label}
                {item.children?.length > 0 && <span className="mega-chevron">▾</span>}
              </button>
            )}

            {item.children?.length > 0 && openId === item.id && (
              <div className="mega-panel">
                <div className="mega-panel-inner">
                  {item.children.map((group) => (
                    <div key={group.id} className="mega-group">
                      {group.label && <span className="mega-group-title">{group.label}</span>}
                      <ul className="mega-group-links">
                        {group.links?.map((link) => (
                          <li key={link.href}>
                            <Link href={link.href} onClick={() => setOpenId(null)}>
                              {link.icon && <span className="mega-link-icon">{link.icon}</span>}
                              <span>{link.label}</span>
                              {link.badge && <span className="badge badge-info sm">{link.badge}</span>}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                  {item.featured && (
                    <div className="mega-featured">
                      {item.featured}
                    </div>
                  )}
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </nav>
  )
}

/**
 * MobileNav — Slide-out mobile navigation drawer.
 */
export function MobileNav({ open, onClose, children, className = '' }) {
  useEffect(() => {
    if (open) document.body.style.overflow = 'hidden'
    else document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  if (!open) return null

  return (
    <>
      <div className="mobile-nav-overlay" onClick={onClose} />
      <nav className={`mobile-nav ${className}`} role="navigation" aria-label="Mobile navigation">
        <div className="mobile-nav-header">
          <button type="button" className="mobile-nav-close" onClick={onClose} aria-label="Close menu">✕</button>
        </div>
        <div className="mobile-nav-body">
          {children}
        </div>
      </nav>
    </>
  )
}

/**
 * MobileNavItem — Collapsible nav item for mobile menu.
 */
export function MobileNavItem({ label, href, children, icon }) {
  const [expanded, setExpanded] = useState(false)
  const hasChildren = children && children.length > 0

  if (!hasChildren) {
    return (
      <Link href={href || '#'} className="mobile-nav-link">
        {icon && <span className="mobile-nav-icon">{icon}</span>}
        <span>{label}</span>
      </Link>
    )
  }

  return (
    <div className={`mobile-nav-group ${expanded ? 'is-open' : ''}`}>
      <button type="button" className="mobile-nav-link" onClick={() => setExpanded(!expanded)}>
        {icon && <span className="mobile-nav-icon">{icon}</span>}
        <span>{label}</span>
        <span className="mobile-nav-chevron">{expanded ? '−' : '+'}</span>
      </button>
      {expanded && (
        <div className="mobile-nav-sub">
          {children}
        </div>
      )}
    </div>
  )
}

/**
 * BackToTop — Floating button to scroll back to top.
 */
export function BackToTop({ threshold = 400 }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > threshold)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold])

  if (!visible) return null

  return (
    <button
      type="button"
      className="back-to-top"
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
      aria-label="Back to top"
    >
      ↑
    </button>
  )
}

/**
 * Breadcrumbs — Component version of breadcrumb navigation.
 */
export function Breadcrumbs({ items = [], className = '' }) {
  return (
    <nav className={`breadcrumbs ${className}`} aria-label="Breadcrumb">
      {items.map((item, i) => (
        <span key={i}>
          {i > 0 && <span className="sep">/</span>}
          {item.href ? (
            <Link href={item.href}>{item.label}</Link>
          ) : (
            <span aria-current="page">{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  )
}
