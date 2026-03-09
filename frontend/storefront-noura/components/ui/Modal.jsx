'use client'

import { useEffect, useRef, useCallback } from 'react'

/**
 * Modal — Dialog overlay.
 *
 * Variants: default | alert | confirm | form | fullscreen
 * Sizes: sm | md (default) | lg | xl | full
 */
export default function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  size = 'md',
  closeOnOverlay = true,
  closeOnEscape = true,
  className = '',
}) {
  const dialogRef = useRef(null)

  const handleEsc = useCallback(
    (e) => {
      if (closeOnEscape && e.key === 'Escape') onClose?.()
    },
    [closeOnEscape, onClose]
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
    <div
      className="modal-overlay"
      onClick={closeOnOverlay ? onClose : undefined}
      role="presentation"
    >
      <div
        ref={dialogRef}
        className={`modal-dialog modal-${size} ${className}`}
        role="dialog"
        aria-modal="true"
        aria-label={title || 'Dialog'}
        onClick={(e) => e.stopPropagation()}
      >
        {(title || onClose) && (
          <div className="modal-header">
            {title && <h2 className="modal-title">{title}</h2>}
            {onClose && (
              <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
                ×
              </button>
            )}
          </div>
        )}
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  )
}

/**
 * ConfirmDialog — Pre-built confirm/cancel dialog.
 */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title = 'Are you sure?',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
}) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      size="sm"
      footer={
        <div className="modal-actions">
          <button type="button" className="button ghost" onClick={onClose}>
            {cancelLabel}
          </button>
          <button type="button" className={`button ${variant}`} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      }
    >
      {message && <p style={{ color: 'var(--muted)', margin: 0 }}>{message}</p>}
    </Modal>
  )
}
