/**
 * Skeleton — Loading placeholder.
 *
 * Variants: text | circle | rect | card | product-card
 */
export default function Skeleton({ variant = 'text', width, height, count = 1, className = '' }) {
  const style = { width, height }
  const items = Array.from({ length: count }, (_, i) => i)

  if (variant === 'circle') {
    return items.map((i) => (
      <div key={i} className={`skeleton skeleton-circle ${className}`} style={style} aria-hidden="true" />
    ))
  }

  if (variant === 'rect' || variant === 'card') {
    return items.map((i) => (
      <div key={i} className={`skeleton skeleton-rect ${className}`} style={style} aria-hidden="true" />
    ))
  }

  if (variant === 'product-card') {
    return items.map((i) => (
      <div key={i} className={`skeleton-product-card ${className}`} aria-hidden="true">
        <div className="skeleton skeleton-rect" style={{ height: 220 }} />
        <div style={{ padding: 16, display: 'grid', gap: 8 }}>
          <div className="skeleton skeleton-text" style={{ width: '40%' }} />
          <div className="skeleton skeleton-text" style={{ width: '80%' }} />
          <div className="skeleton skeleton-text" style={{ width: '30%' }} />
        </div>
      </div>
    ))
  }

  return items.map((i) => (
    <div key={i} className={`skeleton skeleton-text ${className}`} style={style} aria-hidden="true" />
  ))
}

/**
 * Spinner — Loading indicator.
 */
export function Spinner({ size = 24, className = '' }) {
  return (
    <svg
      className={`spinner ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-label="Loading"
      role="status"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity="0.2" />
      <path
        d="M12 2a10 10 0 0 1 10 10"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  )
}

/**
 * ProgressBar — Determinate progress indicator.
 */
export function ProgressBar({ value = 0, max = 100, variant = 'primary', label, className = '' }) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100))
  return (
    <div className={`progress-bar-wrap ${className}`}>
      {label && <span className="progress-label">{label}</span>}
      <div className="progress-track" role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={max}>
        <div className={`progress-fill progress-${variant}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}
