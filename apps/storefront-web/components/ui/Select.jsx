'use client'

import { forwardRef } from 'react'

/**
 * Select — Dropdown selector.
 *
 * options: [{ value, label, disabled? }]
 */
const Select = forwardRef(function Select(
  { label, error, helpText, required, options = [], placeholder, className = '', id, ...rest },
  ref
) {
  const inputId = id || (label ? `sel-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)

  return (
    <div className={`input-group ${error ? 'has-error' : ''}`}>
      {label && (
        <label htmlFor={inputId} className="input-label">
          {label}
          {required && <span className="required-mark"> *</span>}
        </label>
      )}
      <select
        ref={ref}
        id={inputId}
        className={`form-input form-select ${className}`}
        required={required}
        aria-invalid={error ? 'true' : undefined}
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value} disabled={opt.disabled}>
            {opt.label}
          </option>
        ))}
      </select>
      {error && <p className="input-error" role="alert">{error}</p>}
      {helpText && !error && <p className="input-help">{helpText}</p>}
    </div>
  )
})

export default Select
