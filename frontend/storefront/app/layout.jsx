import Link from 'next/link'
import './globals.css'

export const metadata = {
  title: 'DevCore Commerce',
  description: 'Customer storefront scaffold backed by the DevCore POS catalog and inventory engine.'
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <div className="ambient ambient-a" />
        <div className="ambient ambient-b" />
        <div className="site-shell">
          <header className="topbar">
            <Link href="/" className="brand">
              <span className="brand-mark">DC</span>
              <span>
                <strong>DevCore Commerce</strong>
                <small>storefront foundation</small>
              </span>
            </Link>
            <nav className="topnav">
              <Link href="/auth">Account</Link>
              <Link href="/account/addresses">Addresses</Link>
              <Link href="/cart">Cart</Link>
              <Link href="/orders">Orders</Link>
              <Link href="/products">Catalog</Link>
              <a href="http://localhost:5173" target="_blank" rel="noreferrer">
                Admin
              </a>
              <a href="http://localhost:8080/pos" target="_blank" rel="noreferrer">
                POS
              </a>
            </nav>
          </header>
          <main className="page-frame">{children}</main>
          <footer className="footer">
            <p>Built as the customer-facing layer for the existing POS and inventory core.</p>
            <p>Public catalog now ships separately from the internal admin surfaces.</p>
          </footer>
        </div>
      </body>
    </html>
  )
}
