'use client'

import { useState, useRef, useEffect } from 'react'

/**
 * Tooltip — Hover/focus info popup.
 *
 * Placement: top (default) | bottom | left | right
 */
export function Tooltip({ children, content, placement = 'top', className = '' }) {
  const [visible, setVisible] = useState(false)

  return (
    <span
      className={`tooltip-wrap ${className}`}
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      onFocus={() => setVisible(true)}
      onBlur={() => setVisible(false)}
    >
      {children}
      {visible && (
        <span className={`tooltip tooltip-${placement}`} role="tooltip">
          {content}
        </span>
      )}
    </span>
  )
}

/**
 * Popover — Click-triggered content popup.
 */
export function Popover({ children, trigger, placement = 'bottom', className = '' }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    if (open) document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  return (
    <div ref={ref} className={`popover-wrap ${className}`} style={{ position: 'relative', display: 'inline-flex' }}>
      <div onClick={() => setOpen(!open)} role="button" tabIndex={0} onKeyDown={(e) => e.key === 'Enter' && setOpen(!open)}>
        {trigger}
      </div>
      {open && (
        <div className={`popover popover-${placement}`}>
          {children}
        </div>
      )}
    </div>
  )
}
