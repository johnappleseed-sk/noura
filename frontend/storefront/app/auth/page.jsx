import Link from 'next/link'

export default function AuthLandingPage() {
  return (
    <section className="section">
      <div className="panel">
        <div className="section-head">
          <div>
            <span className="eyebrow">Customer account</span>
            <h1>Sign in or create an account</h1>
          </div>
        </div>

        <div className="hero-actions">
          <Link href="/auth/login" className="button primary">
            Sign in
          </Link>
          <Link href="/auth/register" className="button ghost">
            Create account
          </Link>
          <Link href="/products" className="button ghost">
            Continue browsing
          </Link>
        </div>
      </div>
    </section>
  )
}
