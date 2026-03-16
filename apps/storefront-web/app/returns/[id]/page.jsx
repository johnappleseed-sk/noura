'use client'

import Link from 'next/link'

export default function ReturnDetailPage() {
  return (
    <section className="section">
      <div className="panel notice">
        <p>Return detail pages are unavailable because the active backend profile does not expose customer returns APIs.</p>
        <div className="hero-actions">
          <Link href="/returns" className="button primary">
            Back to returns
          </Link>
          <Link href="/orders" className="button ghost">
            View orders
          </Link>
        </div>
      </div>
    </section>
  )
}
