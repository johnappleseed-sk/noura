/**
 * Renders the PageLoader component.
 *
 * @returns The rendered component tree.
 */
export const PageLoader = (): JSX.Element => (
  <div aria-live="polite" className="flex min-h-[40vh] items-center justify-center">
    <div className="flex items-center gap-3 rounded-full border px-5 py-3 text-sm shadow-sm" style={{ borderColor: 'var(--m3-outline-variant)', background: 'var(--m3-surface-container-low)' }}>
      <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-brand-500" />
      Loading content...
    </div>
  </div>
)
