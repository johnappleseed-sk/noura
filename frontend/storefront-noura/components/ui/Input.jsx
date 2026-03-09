'use client'

import { forwardRef } from 'react'

/**
 * Input — Text field with label, error, and help text support.
 *
 * Types: text | email | password | tel | number | search | url
 * States: error | disabled | readOnly
 * Extras: prefix, suffix, clearable
 */
const Input = forwardRef(function Input(
  {
    label,
    error,
    helpText,
    required,
    prefix,
    suffix,
    className = '',
    wrapperClassName = '',
    id,
    ...rest
  },
  ref
) {
  const inputId = id || (label ? `input-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)

  return (
    <div className={`input-group ${error ? 'has-error' : ''} ${wrapperClassName}`}>
      {label && (
        <label htmlFor={inputId} className="input-label">
          {label}
          {required && <span className="required-mark" aria-hidden="true"> *</span>}
        </label>
      )}
      <div className="input-wrapper">
        {prefix && <span className="input-prefix">{prefix}</span>}
        <input
          ref={ref}
          id={inputId}
          className={`form-input ${className}`}
          required={required}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${inputId}-error` : helpText ? `${inputId}-help` : undefined}
          {...rest}
        />
        {suffix && <span className="input-suffix">{suffix}</span>}
      </div>
      {error && <p id={`${inputId}-error`} className="input-error" role="alert">{error}</p>}
      {helpText && !error && <p id={`${inputId}-help`} className="input-help">{helpText}</p>}
    </div>
  )
})

export default Input
