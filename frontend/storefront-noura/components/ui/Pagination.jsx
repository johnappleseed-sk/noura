import Link from 'next/link'

/**
 * Pagination — Page controls.
 */
export default function Pagination({
  page = 0,
  totalPages,
  hasNext,
  hasPrevious,
  buildHref,
  onPageChange,
  className = '',
}) {
  const pages = totalPages
    ? Array.from({ length: totalPages }, (_, i) => i)
    : null

  const Btn = onPageChange ? 'button' : Link

  return (
    <nav className={`pagination ${className}`} aria-label="Pagination">
      {(hasPrevious ?? page > 0) ? (
        <Btn
          {...(onPageChange
            ? { type: 'button', onClick: () => onPageChange(page - 1) }
            : { href: buildHref(page - 1) }
          )}
          className="button ghost sm"
          aria-label="Previous page"
        >
          ← Prev
        </Btn>
      ) : (
        <span />
      )}

      {pages && pages.length <= 7 ? (
        <div className="pagination-pages">
          {pages.map((p) => (
            <Btn
              key={p}
              {...(onPageChange
                ? { type: 'button', onClick: () => onPageChange(p) }
                : { href: buildHref(p) }
              )}
              className={`pagination-page ${p === page ? 'active' : ''}`}
              aria-current={p === page ? 'page' : undefined}
            >
              {p + 1}
            </Btn>
          ))}
        </div>
      ) : (
        <span className="pagination-label">Page {page + 1}{totalPages ? ` of ${totalPages}` : ''}</span>
      )}

      {(hasNext ?? (totalPages ? page < totalPages - 1 : false)) ? (
        <Btn
          {...(onPageChange
            ? { type: 'button', onClick: () => onPageChange(page + 1) }
            : { href: buildHref(page + 1) }
          )}
          className="button ghost sm"
          aria-label="Next page"
        >
          Next →
        </Btn>
      ) : (
        <span />
      )}
    </nav>
  )
}

/**
 * Stepper — Multi-step progress indicator.
 *
 * steps: [{ label, description? }]
 */
export function Stepper({ steps = [], currentStep = 0, className = '' }) {
  return (
    <div className={`stepper ${className}`} role="list">
      {steps.map((step, i) => (
        <div
          key={i}
          className={`stepper-step ${i < currentStep ? 'completed' : ''} ${i === currentStep ? 'active' : ''}`}
          role="listitem"
        >
          <div className="stepper-indicator">
            {i < currentStep ? '✓' : i + 1}
          </div>
          <div className="stepper-label">
            <span className="stepper-title">{step.label}</span>
            {step.description && <span className="stepper-desc">{step.description}</span>}
          </div>
          {i < steps.length - 1 && <div className="stepper-connector" />}
        </div>
      ))}
    </div>
  )
}
