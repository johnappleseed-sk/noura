'use client'

import { useEffect, useRef, useCallback } from 'react'

/**
 * Drawer — Slide-in side panel.
 *
 * Position: right (default) | left | bottom
 * Sizes: sm (320px) | md (420px) | lg (560px) | xl (720px) | full
 */
export default function Drawer({
  open,
  onClose,
  title,
  children,
  footer,
  position = 'right',
  size = 'md',
  className = '',
}) {
  const handleEsc = useCallback(
    (e) => {
      if (e.key === 'Escape') onClose?.()
    },
    [onClose]
  )

  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden'
      document.addEventListener('keydown', handleEsc)
    }
    return () => {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', handleEsc)
    }
  }, [open, handleEsc])

  if (!open) return null

  return (
    <div className="drawer-overlay" onClick={onClose} role="presentation">
      <aside
        className={`drawer drawer-${position} drawer-${size} ${className}`}
        role="dialog"
        aria-modal="true"
        aria-label={title || 'Panel'}
        onClick={(e) => e.stopPropagation()}
      >
        {(title || onClose) && (
          <div className="drawer-header">
            {title && <h2 className="drawer-title">{title}</h2>}
            {onClose && (
              <button type="button" className="drawer-close" onClick={onClose} aria-label="Close">
                ×
              </button>
            )}
          </div>
        )}
        <div className="drawer-body">{children}</div>
        {footer && <div className="drawer-footer">{footer}</div>}
      </aside>
    </div>
  )
}
