'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { getCart, resolveCustomerToken } from '@/lib/api'

function AccountIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M12 12.8a4.4 4.4 0 1 0 0-8.8 4.4 4.4 0 0 0 0 8.8Zm0 2.2c-4.4 0-8 2.4-8 5.4 0 .4.3.6.6.6h14.8c.4 0 .6-.3.6-.6 0-3-3.6-5.4-8-5.4Z" />
    </svg>
  )
}

function CartIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M7.2 5h13.2c.5 0 .8.4.7.9l-1.3 7.4a2 2 0 0 1-2 1.7H10a2 2 0 0 1-2-1.6L6.8 6.5H4.2a.8.8 0 0 1 0-1.5h2.4c.3 0 .6.2.7.5Zm2.5 13.8a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4Zm7.2 0a1.7 1.7 0 1 0 0 3.4 1.7 1.7 0 0 0 0-3.4Z" />
    </svg>
  )
}

function OrdersIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M6 3.8h12A2.2 2.2 0 0 1 20.2 6v12A2.2 2.2 0 0 1 18 20.2H6A2.2 2.2 0 0 1 3.8 18V6A2.2 2.2 0 0 1 6 3.8Zm2.2 4.1c-.5 0-.8.3-.8.8s.3.8.8.8h7.6c.5 0 .8-.3.8-.8s-.3-.8-.8-.8H8.2Zm0 3.6c-.5 0-.8.3-.8.8s.3.8.8.8h7.6c.5 0 .8-.3.8-.8s-.3-.8-.8-.8H8.2Zm0 3.6c-.5 0-.8.3-.8.8s.3.8.8.8h4.4c.5 0 .8-.3.8-.8s-.3-.8-.8-.8H8.2Z" />
    </svg>
  )
}

const iconByKey = {
  account: AccountIcon,
  cart: CartIcon,
  orders: OrdersIcon
}

export function AnnouncementBar({ message, ctaLabel, ctaHref }) {
  return (
    <div className="enterprise-announcement" role="note" aria-label="Store announcement">
      <div className="enterprise-announcement-inner">
        <p>{message}</p>
        <Link href={ctaHref} className="enterprise-announcement-cta">
          {ctaLabel}
          <span aria-hidden="true">&rarr;</span>
        </Link>
      </div>
    </div>
  )
}

export function HeaderBrand({ brandName = 'Noura', href = '/' }) {
  return (
    <Link href={href} className="enterprise-brand" aria-label={`${brandName} home`}>
      <span className="enterprise-brand-mark" aria-hidden="true">
        <img
          src="/logo/noura-enterprise-icon.svg"
          alt=""
          className="enterprise-brand-mark-image"
          loading="eager"
          decoding="async"
        />
      </span>
      <span className="enterprise-brand-copy">
        <strong>{brandName}</strong>
        <small>Commerce Platform</small>
      </span>
    </Link>
  )
}

export function HeaderNav({ items = [] }) {
  const pathname = usePathname()
  const navRef = useRef(null)
  const linkRefs = useRef({})
  const megaRefs = useRef({})
  const [openMenuKey, setOpenMenuKey] = useState(null)
  const [indicator, setIndicator] = useState({ left: 0, width: 0, visible: false })

  const navOrder = useMemo(() => items.map((item) => item.href), [items])

  const focusNavLinkByIndex = (index) => {
    const href = navOrder[index]
    if (!href) return
    linkRefs.current[href]?.focus()
  }

  const handleMegaLinkKeyDown = (item, entryIndex, event) => {
    const entries = megaRefs.current[item.href] || []

    if (event.key === 'Tab' && entries.length > 0) {
      const firstIndex = 0
      const lastIndex = entries.length - 1

      if (!event.shiftKey && entryIndex === lastIndex) {
        event.preventDefault()
        entries[firstIndex]?.focus()
        return
      }

      if (event.shiftKey && entryIndex === firstIndex) {
        event.preventDefault()
        entries[lastIndex]?.focus()
        return
      }
    }

    if (event.key === 'Escape') {
      event.preventDefault()
      setOpenMenuKey(null)
      linkRefs.current[item.href]?.focus()
      return
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      const next = (entryIndex + 1) % entries.length
      entries[next]?.focus()
      return
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      const prev = (entryIndex - 1 + entries.length) % entries.length
      entries[prev]?.focus()
    }
  }

  const handleNavLinkKeyDown = (item, itemIndex, hasChildren, event) => {
    if (event.key === 'ArrowRight') {
      event.preventDefault()
      focusNavLinkByIndex((itemIndex + 1) % navOrder.length)
      return
    }

    if (event.key === 'ArrowLeft') {
      event.preventDefault()
      focusNavLinkByIndex((itemIndex - 1 + navOrder.length) % navOrder.length)
      return
    }

    if (event.key === 'Home') {
      event.preventDefault()
      focusNavLinkByIndex(0)
      return
    }

    if (event.key === 'End') {
      event.preventDefault()
      focusNavLinkByIndex(navOrder.length - 1)
      return
    }

    if (event.key === 'Escape') {
      event.preventDefault()
      setOpenMenuKey(null)
      return
    }

    if (event.key === 'ArrowDown' && hasChildren) {
      event.preventDefault()
      setOpenMenuKey(item.href)

      requestAnimationFrame(() => {
        const firstItem = megaRefs.current[item.href]?.[0]
        firstItem?.focus()
      })
    }
  }

  useEffect(() => {
    const syncIndicator = () => {
      const activeItem = items.find((item) => (item.matchPrefix ? pathname.startsWith(item.href) : pathname === item.href))
      if (!activeItem) {
        setIndicator((prev) => ({ ...prev, visible: false }))
        return
      }

      const linkEl = linkRefs.current[activeItem.href]
      const navEl = navRef.current
      if (!linkEl || !navEl) {
        setIndicator((prev) => ({ ...prev, visible: false }))
        return
      }

      setIndicator({
        left: linkEl.offsetLeft,
        width: linkEl.offsetWidth,
        visible: true
      })
    }

    syncIndicator()
    window.addEventListener('resize', syncIndicator)
    return () => window.removeEventListener('resize', syncIndicator)
  }, [items, pathname])

  return (
    <nav
      ref={navRef}
      className="enterprise-nav"
      aria-label="Primary"
      onMouseLeave={() => setOpenMenuKey(null)}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          setOpenMenuKey(null)
        }
      }}
    >
      {items.map((item, itemIndex) => {
        const active = item.matchPrefix ? pathname.startsWith(item.href) : pathname === item.href
        const hasChildren = Array.isArray(item.children) && item.children.length > 0
        const isMegaOpen = openMenuKey === item.href

        return (
          <div
            key={item.href}
            className="enterprise-nav-item"
            onMouseEnter={() => hasChildren && setOpenMenuKey(item.href)}
            onFocus={() => hasChildren && setOpenMenuKey(item.href)}
          >
            <Link
              ref={(el) => { linkRefs.current[item.href] = el }}
              href={item.href}
              className={`enterprise-nav-link${active ? ' is-active' : ''}`}
              aria-current={active ? 'page' : undefined}
              aria-haspopup={hasChildren ? 'menu' : undefined}
              aria-expanded={hasChildren ? isMegaOpen : undefined}
              onKeyDown={(event) => handleNavLinkKeyDown(item, itemIndex, hasChildren, event)}
            >
              <span>{item.label}</span>
              {hasChildren ? <span className="enterprise-nav-caret" aria-hidden="true">▾</span> : null}
            </Link>

            {hasChildren ? (
              <div
                className={`enterprise-mega${isMegaOpen ? ' is-open' : ''}`}
                role="menu"
                aria-label={`${item.label} menu`}
              >
                <div className="enterprise-mega-inner">
                  {item.children.map((entry, entryIndex) => (
                    <Link
                      key={entry.href}
                      ref={(el) => {
                        if (!megaRefs.current[item.href]) megaRefs.current[item.href] = []
                        megaRefs.current[item.href][entryIndex] = el
                      }}
                      href={entry.href}
                      role="menuitem"
                      className="enterprise-mega-link"
                      onClick={() => setOpenMenuKey(null)}
                      onKeyDown={(event) => handleMegaLinkKeyDown(item, entryIndex, event)}
                    >
                      <span className="enterprise-mega-title">{entry.label}</span>
                      {entry.description ? <small>{entry.description}</small> : null}
                    </Link>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        )
      })}

      <span
        className={`enterprise-nav-indicator${indicator.visible ? ' is-visible' : ''}`}
        style={{
          '--indicator-left': `${indicator.left}px`,
          '--indicator-width': `${indicator.width}px`
        }}
        aria-hidden="true"
      />
    </nav>
  )
}

export function HeaderActions({ items = [] }) {
  const pathname = usePathname()

  return (
    <nav className="enterprise-actions" aria-label="Utility">
      {items.map((item) => {
        const active = pathname === item.href || (item.matchPrefix && pathname.startsWith(item.href))
        const Icon = iconByKey[item.icon] || AccountIcon
        return (
          <Link
            key={item.href}
            href={item.href}
            className={`enterprise-action${item.emphasis ? ' is-emphasis' : ''}${active ? ' is-active' : ''}`}
            aria-current={active ? 'page' : undefined}
          >
            <span className="enterprise-action-icon">
              <Icon />
            </span>
            <span>{item.label}</span>
            {item.badge != null ? <span className="enterprise-action-badge">{item.badge}</span> : null}
          </Link>
        )
      })}
    </nav>
  )
}

export function Header({
  brandName = 'Noura',
  announcement = {
    message: 'Free shipping on orders over $99',
    ctaLabel: 'Shop now',
    ctaHref: '/products'
  },
  navItems = [],
  actionItems = [],
  cartCount = null,
  enableCartBadge = true
}) {
  const pathname = usePathname()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [cartBadgeCount, setCartBadgeCount] = useState(cartCount)

  useEffect(() => {
    if (!enableCartBadge || cartCount != null) {
      setCartBadgeCount(cartCount)
      return
    }

    let cancelled = false

    async function hydrateCartCount() {
      const token = resolveCustomerToken()
      if (!token) {
        if (!cancelled) setCartBadgeCount(null)
        return
      }

      try {
        const cart = await getCart(token)
        const count = Array.isArray(cart?.items)
          ? cart.items.reduce((total, item) => total + Number(item?.quantity || 0), 0)
          : null

        if (!cancelled) {
          setCartBadgeCount(count && count > 0 ? count : null)
        }
      } catch {
        if (!cancelled) {
          setCartBadgeCount(null)
        }
      }
    }

    hydrateCartCount()
    return () => {
      cancelled = true
    }
  }, [cartCount, enableCartBadge, pathname])

  const decoratedActionItems = useMemo(
    () => actionItems.map((item) => {
      if (item.icon !== 'cart' || !enableCartBadge) {
        return item
      }

      if (item.badge != null) {
        return item
      }

      return {
        ...item,
        badge: cartBadgeCount
      }
    }),
    [actionItems, cartBadgeCount, enableCartBadge]
  )

  const mobileItems = useMemo(
    () => [...navItems, ...decoratedActionItems],
    [decoratedActionItems, navItems]
  )

  return (
    <div className="enterprise-header-shell">
      <AnnouncementBar
        message={announcement.message}
        ctaLabel={announcement.ctaLabel}
        ctaHref={announcement.ctaHref}
      />

      <header className="enterprise-header" role="banner">
        <div className="enterprise-header-inner">
          <HeaderBrand brandName={brandName} />
          <HeaderNav items={navItems} />
          <HeaderActions items={decoratedActionItems} />

          <button
            type="button"
            className="enterprise-menu-trigger"
            aria-expanded={mobileOpen}
            aria-controls="enterprise-mobile-menu"
            onClick={() => setMobileOpen((open) => !open)}
          >
            <span className="enterprise-menu-line" />
            <span className="enterprise-menu-line" />
            <span className="enterprise-menu-line" />
            <span className="sr-only">Toggle menu</span>
          </button>
        </div>

        <div
          id="enterprise-mobile-menu"
          className={`enterprise-mobile${mobileOpen ? ' is-open' : ''}`}
        >
          <div className="enterprise-mobile-scroll">
            {mobileItems.map((item) => (
              <Link
                key={`${item.label}-${item.href}`}
                href={item.href}
                className={`enterprise-mobile-link${item.emphasis ? ' is-emphasis' : ''}`}
                onClick={() => setMobileOpen(false)}
              >
                <span>{item.label}</span>
                <span aria-hidden="true">&rarr;</span>
              </Link>
            ))}
          </div>
        </div>
      </header>
    </div>
  )
}
