'use client'

import { useEffect, useState, useCallback, createContext, useContext } from 'react'

const ToastContext = createContext(null)

/**
 * useToast — Hook to show toasts from anywhere.
 *
 * const toast = useToast()
 * toast.success('Item added!')
 * toast.error('Failed to save')
 * toast.info('Processing...')
 * toast.warning('Low stock')
 */
export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within <ToastProvider>')
  return ctx
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const remove = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const add = useCallback((message, variant = 'info', duration = 4000) => {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, message, variant }])
    if (duration > 0) setTimeout(() => remove(id), duration)
    return id
  }, [remove])

  const api = {
    success: (msg, dur) => add(msg, 'success', dur),
    error: (msg, dur) => add(msg, 'danger', dur),
    warning: (msg, dur) => add(msg, 'warning', dur),
    info: (msg, dur) => add(msg, 'info', dur),
    remove,
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-container" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.variant}`} role="status">
            <span className="toast-message">{t.message}</span>
            <button
              type="button"
              className="toast-close"
              onClick={() => remove(t.id)}
              aria-label="Dismiss"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

/**
 * Alert — Inline notification banner.
 *
 * Variants: info | success | warning | danger
 */
export function Alert({ variant = 'info', title, children, dismissible, onDismiss, className = '' }) {
  const [visible, setVisible] = useState(true)

  if (!visible) return null

  return (
    <div className={`alert alert-${variant} ${className}`} role="alert">
      <div className="alert-content">
        {title && <strong className="alert-title">{title}</strong>}
        <div className="alert-body">{children}</div>
      </div>
      {dismissible && (
        <button
          type="button"
          className="alert-close"
          onClick={() => { setVisible(false); onDismiss?.() }}
          aria-label="Dismiss"
        >
          ×
        </button>
      )}
    </div>
  )
}
