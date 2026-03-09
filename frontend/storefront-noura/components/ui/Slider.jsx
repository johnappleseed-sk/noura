'use client'

import { useState, useRef } from 'react'

/**
 * Slider — Range input for prices, quantities, ratings.
 *
 * dual: enables two-handle range selection
 */
export function Slider({
  min = 0,
  max = 100,
  step = 1,
  value,
  onChange,
  label,
  showValue = true,
  className = '',
}) {
  return (
    <div className={`slider-wrap ${className}`}>
      {label && <label className="input-label">{label}</label>}
      <div className="slider-row">
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(e) => onChange?.(Number(e.target.value))}
          className="slider"
        />
        {showValue && <span className="slider-value">{value}</span>}
      </div>
    </div>
  )
}

/**
 * RangeSlider — Dual-handle range slider (e.g. price range).
 */
export function RangeSlider({
  min = 0,
  max = 100,
  step = 1,
  low,
  high,
  onChange,
  label,
  formatValue,
  className = '',
}) {
  const fmt = formatValue || ((v) => v)

  return (
    <div className={`slider-wrap ${className}`}>
      {label && <label className="input-label">{label}</label>}
      <div className="range-slider-track">
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={low}
          onChange={(e) => {
            const v = Number(e.target.value)
            if (v <= high) onChange?.({ low: v, high })
          }}
          className="range-slider range-low"
        />
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={high}
          onChange={(e) => {
            const v = Number(e.target.value)
            if (v >= low) onChange?.({ low, high: v })
          }}
          className="range-slider range-high"
        />
      </div>
      <div className="range-labels">
        <span>{fmt(low)}</span>
        <span>{fmt(high)}</span>
      </div>
    </div>
  )
}

/**
 * QuantityStepper — Increment/decrement number input.
 */
export function QuantityStepper({
  value = 1,
  min = 1,
  max = 99,
  onChange,
  disabled,
  className = '',
}) {
  return (
    <div className={`qty-control ${className}`}>
      <button
        type="button"
        onClick={() => onChange?.(Math.max(min, value - 1))}
        disabled={disabled || value <= min}
        aria-label="Decrease quantity"
      >
        −
      </button>
      <span className="qty-value" aria-live="polite">{value}</span>
      <button
        type="button"
        onClick={() => onChange?.(Math.min(max, value + 1))}
        disabled={disabled || value >= max}
        aria-label="Increase quantity"
      >
        +
      </button>
    </div>
  )
}
