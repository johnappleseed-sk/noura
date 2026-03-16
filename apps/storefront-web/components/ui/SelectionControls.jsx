'use client'

import { forwardRef } from 'react'

/** Checkbox with label support. */
const Checkbox = forwardRef(function Checkbox({ label, error, className = '', id, ...rest }, ref) {
  const inputId = id || (label ? `cb-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)
  return (
    <div className={`check-group ${error ? 'has-error' : ''} ${className}`}>
      <label className="check-label" htmlFor={inputId}>
        <input ref={ref} type="checkbox" id={inputId} className="check-input" {...rest} />
        <span className="check-box" aria-hidden="true" />
        {label && <span className="check-text">{label}</span>}
      </label>
      {error && <p className="input-error">{error}</p>}
    </div>
  )
})

/** Radio with label support. */
const Radio = forwardRef(function Radio({ label, className = '', id, ...rest }, ref) {
  const inputId = id || (label ? `rd-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)
  return (
    <label className={`check-label ${className}`} htmlFor={inputId}>
      <input ref={ref} type="radio" id={inputId} className="radio-input" {...rest} />
      <span className="radio-dot" aria-hidden="true" />
      {label && <span className="check-text">{label}</span>}
    </label>
  )
})

/** Toggle switch. */
const Switch = forwardRef(function Switch({ label, className = '', id, ...rest }, ref) {
  const inputId = id || (label ? `sw-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)
  return (
    <label className={`switch-label ${className}`} htmlFor={inputId}>
      <input ref={ref} type="checkbox" id={inputId} className="switch-input" role="switch" {...rest} />
      <span className="switch-track" aria-hidden="true" />
      {label && <span className="check-text">{label}</span>}
    </label>
  )
})

export { Checkbox, Radio, Switch }
