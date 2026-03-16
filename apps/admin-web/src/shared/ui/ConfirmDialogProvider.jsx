import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

const ConfirmDialogContext = createContext(null)

function normalizeOptions(options) {
  if (typeof options === 'string') {
    return {
      title: 'Confirm action',
      message: options,
      confirmLabel: 'Confirm',
      cancelLabel: 'Cancel',
      tone: 'danger'
    }
  }

  return {
    title: options?.title || 'Confirm action',
    message: options?.message || '',
    description: options?.description || '',
    confirmLabel: options?.confirmLabel || 'Confirm',
    cancelLabel: options?.cancelLabel || 'Cancel',
    tone: options?.tone || 'danger'
  }
}

export function ConfirmDialogProvider({ children }) {
  const [dialogState, setDialogState] = useState(null)

  const close = useCallback((result) => {
    setDialogState((current) => {
      if (current?.resolve) {
        current.resolve(result)
      }
      return null
    })
  }, [])

  const confirm = useCallback((options) => (
    new Promise((resolve) => {
      setDialogState({
        ...normalizeOptions(options),
        resolve
      })
    })
  ), [])

  useEffect(() => {
    if (!dialogState) return undefined

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        close(false)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [close, dialogState])

  const value = useMemo(() => confirm, [confirm])

  return (
    <ConfirmDialogContext.Provider value={value}>
      {children}
      {dialogState ? (
        <div className="confirm-dialog-backdrop" role="presentation" onClick={() => close(false)}>
          <div
            className="confirm-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="confirm-dialog-head">
              <h3 id="confirm-dialog-title">{dialogState.title}</h3>
              <p>{dialogState.message}</p>
              {dialogState.description ? <small>{dialogState.description}</small> : null}
            </div>
            <div className="confirm-dialog-actions">
              <button type="button" className="btn btn-outline" onClick={() => close(false)}>
                {dialogState.cancelLabel}
              </button>
              <button
                type="button"
                className={`btn ${dialogState.tone === 'danger' ? 'btn-danger' : 'btn-primary'}`}
                onClick={() => close(true)}
              >
                {dialogState.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </ConfirmDialogContext.Provider>
  )
}

export function useConfirmDialog() {
  const context = useContext(ConfirmDialogContext)
  if (!context) {
    throw new Error('useConfirmDialog must be used inside ConfirmDialogProvider')
  }
  return context
}
