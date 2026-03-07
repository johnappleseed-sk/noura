import { Link } from 'react-router-dom'
import { Seo } from '@/components/common/Seo'

/**
 * Renders the NotFoundPage component.
 *
 * @returns The rendered component tree.
 */
export const NotFoundPage = (): JSX.Element => (
  <section className="panel mx-auto max-w-xl p-10 text-center">
    <Seo description="Page not found." title="404" />
    <h1 className="text-4xl font-black">404</h1>
    <p className="m3-subtitle mt-3 text-sm">
      The page you requested does not exist or was moved.
    </p>
    <Link className="m3-btn m3-btn-filled mt-5" to="/">
      Go Home
    </Link>
  </section>
)
