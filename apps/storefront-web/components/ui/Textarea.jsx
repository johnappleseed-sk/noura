'use client'

import { forwardRef } from 'react'

/**
 * Textarea — Multiline text input.
 */
const Textarea = forwardRef(function Textarea(
  { label, error, helpText, required, className = '', id, rows = 4, ...rest },
  ref
) {
  const inputId = id || (label ? `ta-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)

  return (
    <div className={`input-group ${error ? 'has-error' : ''}`}>
      {label && (
        <label htmlFor={inputId} className="input-label">
          {label}
          {required && <span className="required-mark"> *</span>}
        </label>
      )}
      <textarea
        ref={ref}
        id={inputId}
        className={`form-input form-textarea ${className}`}
        required={required}
        rows={rows}
        aria-invalid={error ? 'true' : undefined}
        {...rest}
      />
      {error && <p className="input-error" role="alert">{error}</p>}
      {helpText && !error && <p className="input-help">{helpText}</p>}
    </div>
  )
})

export default Textarea
