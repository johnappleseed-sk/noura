import Link from 'next/link'

/**
 * EmptyState — Placeholder for zero data.
 */
export function EmptyState({ icon, title, description, action, actionLabel, actionHref, className = '' }) {
  return (
    <div className={`empty-state ${className}`}>
      {icon && <div className="empty-icon">{icon}</div>}
      <div className="empty-copy-block">
        {title && <h2>{title}</h2>}
        {description && <p>{description}</p>}
      </div>
      {(action || actionHref) && (
        <div className="empty-actions">
          {actionHref ? (
            <Link href={actionHref} className="button primary">{actionLabel || 'Go'}</Link>
          ) : (
            <button type="button" className="button primary" onClick={action}>{actionLabel || 'Retry'}</button>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * ErrorState — Error display with retry.
 */
export function ErrorState({ title = 'Something went wrong', message, onRetry, className = '' }) {
  return (
    <div className={`empty-state error-state ${className}`}>
      <div className="empty-copy-block">
        <span className="eyebrow" style={{ color: 'var(--danger)' }}>Error</span>
        <h2>{title}</h2>
        {message && <p>{message}</p>}
      </div>
      {onRetry && (
        <div className="empty-actions">
          <button type="button" className="button primary" onClick={onRetry}>Try Again</button>
        </div>
      )}
    </div>
  )
}
