import { useAppSelector } from '@/app/hooks'

const customerServiceLinks = ['Help Center', 'Shipping', 'Returns', 'Order Tracking', 'Contact Support']
const companyLinks = ['About Us', 'Blog', 'Careers', 'Press', 'Investors']
const policyLinks = ['Privacy Policy', 'Terms of Service', 'Cookie Policy', 'Accessibility', 'Compliance']
const marketplaceLinks = ['Sell on Noura', 'Partner Program', 'Developer API', 'Affiliate Program']
const paymentMethods = ['Visa', 'Mastercard', 'Amex', 'PayPal', 'Stripe']
const socialChannels = ['Instagram', 'LinkedIn', 'YouTube', 'X']

/**
 * Renders the Footer component.
 *
 * @returns The rendered component tree.
 */
export const Footer = (): JSX.Element => {
  const geo = useAppSelector((state) => state.geo.context)

  /**
   * Executes back to top.
   *
   * @returns No value.
   */
  const backToTop = (): void => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <footer
      className="mt-10 border-t"
      style={{
        borderColor: 'var(--m3-outline-variant)',
        background: 'color-mix(in oklab, var(--m3-surface-container-lowest) 90%, transparent)',
      }}
    >
      <div className="w-full space-y-8 px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-5">
          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide">Customer Service</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {customerServiceLinks.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide">About</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {companyLinks.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide">Policies</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {policyLinks.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide">Marketplace</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {marketplaceLinks.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide">Connect</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {socialChannels.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>
        </div>

        <div className="grid gap-4 border-t pt-6 sm:grid-cols-2" style={{ borderColor: 'var(--m3-outline-variant)' }}>
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wide">Payment Methods</h3>
            <div className="mt-2 flex flex-wrap gap-2">
              {paymentMethods.map((method) => (
                <span className="m3-chip" key={method}>
                  {method}
                </span>
              ))}
            </div>
          </section>

          <section className="sm:text-right">
            <h3 className="text-xs font-semibold uppercase tracking-wide">Region</h3>
            <p className="m3-subtitle mt-2 text-sm">
              {geo.region.toUpperCase()} / {geo.language} / {geo.currency}
            </p>
            <p className="m3-subtitle text-xs">Country: {geo.countryCode}</p>
          </section>
        </div>

        <div className="flex flex-col gap-3 border-t pt-6 text-xs sm:flex-row sm:items-center sm:justify-between" style={{ borderColor: 'var(--m3-outline-variant)' }}>
          <p>
            © 2026 Noura Commerce. All rights reserved. Enterprise demo for multi-region storefront architecture.
          </p>
          <button className="m3-btn m3-btn-outlined !h-9 !rounded-xl !px-3" onClick={backToTop} type="button">
            Back to top
          </button>
        </div>
      </div>
    </footer>
  )
}
