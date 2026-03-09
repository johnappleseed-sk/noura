import Link from 'next/link'
import './globals.css'
import { SkipToContent, CookieConsent } from '@/components/mobile'

const apiBaseUrl = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export const metadata = {
  title: 'Noura',
  description: 'Enterprise e-commerce storefront powered by the Noura commerce platform.'
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <SkipToContent targetId="main-content" />
        <div className="site-shell">
          <div className="announcement-bar">
            Free shipping on orders over $99 &mdash; <Link href="/products">Shop now</Link>
          </div>
          <header className="topbar">
            <div className="brand-area">
              <Link href="/" className="brand">
                <span className="brand-mark">N</span>
                <span>
                  <strong>Noura</strong>
                </span>
              </Link>
            </div>
            <nav className="topnav">
              <Link href="/products">Shop</Link>
              <Link href="/deals">Deals</Link>
              <Link href="/stores">Stores</Link>
              <Link href="/auth">Account</Link>
              <Link href="/cart">Cart</Link>
              <Link href="/orders">Orders</Link>
            </nav>
          </header>
          <main id="main-content" className="page-frame">{children}</main>
          <footer className="footer">
            <div className="footer-inner">
              <div className="footer-block">
                <span className="eyebrow">Shop</span>
                <Link href="/products">All Products</Link>
                <Link href="/deals">Deals &amp; Promotions</Link>
                <Link href="/products?sort=priceAsc">Budget Finds</Link>
                <Link href="/products?sort=priceDesc">Premium Selection</Link>
              </div>
              <div className="footer-block">
                <span className="eyebrow">Account</span>
                <Link href="/auth">My Account</Link>
                <Link href="/orders">Order History</Link>
                <Link href="/cart">Shopping Cart</Link>
                <Link href="/account/addresses">Addresses</Link>
                <Link href="/account/payments">Payment Methods</Link>
              </div>
              <div className="footer-block">
                <span className="eyebrow">Support</span>
                <Link href="/stores">Store Locator</Link>
                <Link href="/returns">Returns &amp; Refunds</Link>
                <Link href="/account/notifications">Notifications</Link>
              </div>
              <div className="footer-block">
                <span className="eyebrow">About Noura</span>
                <p>Enterprise commerce platform with full catalog management, multi-channel pricing, inventory control, and customer self-service.</p>
              </div>
            </div>
            <div className="footer-bottom">
              <p>&copy; 2026 Noura Commerce. All rights reserved.</p>
              <div style={{ display: 'flex', gap: 16 }}>
                <a href="http://localhost:5173" target="_blank" rel="noreferrer">Admin Portal</a>
                <a href={`${apiBaseUrl}/swagger-ui`} target="_blank" rel="noreferrer">API Docs</a>
              </div>
            </div>
          </footer>
        </div>
        <CookieConsent />
      </body>
    </html>
  )
}
