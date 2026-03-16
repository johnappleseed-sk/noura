'use client'

/**
 * Badge — Small labels for status, counts, and tags.
 *
 * Variants: success | warning | danger | info | neutral | accent | sale | new | trending | low-stock
 * Sizes: sm (default) | md | lg
 * removable: show × button
 */
export default function Badge({
  children,
  variant = 'neutral',
  size,
  dot,
  removable,
  onRemove,
  className = '',
  ...rest
}) {
  return (
    <span className={`badge ${variant} ${size || ''} ${dot ? 'has-dot' : ''} ${className}`} {...rest}>
      {dot && <span className="badge-dot" aria-hidden="true" />}
      {children}
      {removable && (
        <button
          type="button"
          className="badge-remove"
          onClick={onRemove}
          aria-label="Remove"
        >
          ×
        </button>
      )}
    </span>
  )
}

/**
 * Tag — Removable chip for filters and selections.
 */
export function Tag({ children, onRemove, className = '', ...rest }) {
  return (
    <span className={`tag ${className}`} {...rest}>
      {children}
      {onRemove && (
        <button type="button" className="tag-remove" onClick={onRemove} aria-label="Remove">
          ×
        </button>
      )}
    </span>
  )
}
