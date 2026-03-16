import { createContext, useCallback, useContext, useMemo, useState } from 'react'

const ToastContext = createContext(null)
const TOAST_DURATION_MS = 4200

function nextToastId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((toastId) => {
    setToasts((current) => current.filter((toast) => toast.id !== toastId))
  }, [])

  const push = useCallback((variant, message) => {
    const id = nextToastId()
    setToasts((current) => [...current, { id, variant, message }])
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, TOAST_DURATION_MS)
  }, [])

  const value = useMemo(() => ({
    success(message) {
      push('success', message)
    },
    error(message) {
      push('error', message)
    },
    warning(message) {
      push('warning', message)
    },
    info(message) {
      push('info', message)
    }
  }), [push])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast-card toast-card-${toast.variant}`} role="status">
            <span className="toast-card-message">{toast.message}</span>
            <button
              type="button"
              className="toast-card-close"
              onClick={() => dismiss(toast.id)}
              aria-label="Dismiss notification"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider')
  }
  return context
}
