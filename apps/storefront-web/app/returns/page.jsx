import Link from 'next/link'
import { getRuntimeFeatures } from '@/lib/api'

export default async function ReturnsPage() {
  let runtimeFeatures = null

  try {
    runtimeFeatures = await getRuntimeFeatures()
  } catch {
    runtimeFeatures = null
  }

  const returnsEnabled = runtimeFeatures?.features?.['storefront.returns'] === true
  const returnsMessage =
    runtimeFeatures?.messages?.['storefront.returns'] ||
    'The active runtime does not expose storefront returns APIs.'

  return (
    <section className="section stack-gap">
      <div className="section-head">
        <div>
          <span className="eyebrow">Returns</span>
          <h1>
            {returnsEnabled
              ? 'Returns are enabled in backend but not yet wired in this storefront client'
              : 'Returns are not active on this backend profile'}
          </h1>
        </div>
      </div>

      <article className="panel notice">
        <p>
          {returnsEnabled ? (
            <>The runtime contract reports customer returns as enabled, but this storefront route is currently informational only.</>
          ) : (
            <>
              The current backend exposes catalog, account, cart, checkout, and order history through
              <code> /api/v1 </code>
              endpoints. {returnsMessage}
            </>
          )}
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
