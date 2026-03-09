import Link from 'next/link'

export default function NotFound() {
  return (
    <section className="panel notice">
      <h1>Product not found</h1>
      <p>The requested storefront record is unavailable or the catalog API is offline.</p>
      <Link href="/products" className="button primary">
        Return to catalog
      </Link>
    </section>
  )
}
