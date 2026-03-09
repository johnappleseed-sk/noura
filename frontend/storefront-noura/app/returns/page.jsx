'use client'

import Link from 'next/link'

export default function ReturnsPage() {
  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Returns</span>
          <h1>Returns are not active on this backend profile</h1>
        </div>
      </div>

      <article className="panel notice">
        <p>
          The current local backend exposes catalog, account, cart, checkout, and order history through
          <code> /api/v1 </code>
          endpoints. Customer return workflows are implemented in a separate storefront module that is not active in
          this runtime.
        </p>
      </article>

      <div className="hero-actions">
        <Link href="/orders" className="button primary">
          View orders
        </Link>
        <Link href="/auth" className="button ghost">
          Open account
        </Link>
        <Link href="/products" className="button ghost">
          Back to catalog
        </Link>
      </div>
    </section>
  )
}
