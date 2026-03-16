'use client'

import { useState } from 'react'

/**
 * StarRating — Display or input star rating.
 *
 * interactive: allows clicking to set rating
 */
export default function StarRating({
  value = 0,
  max = 5,
  interactive,
  onChange,
  size = 'md',
  showValue = false,
  className = '',
}) {
  const [hover, setHover] = useState(0)

  return (
    <div className={`star-rating star-rating-${size} ${interactive ? 'interactive' : ''} ${className}`}>
      {Array.from({ length: max }, (_, i) => {
        const starNum = i + 1
        const filled = interactive ? starNum <= (hover || value) : starNum <= value
        return (
          <span
            key={i}
            className={`star ${filled ? 'filled' : ''}`}
            onClick={interactive ? () => onChange?.(starNum) : undefined}
            onMouseEnter={interactive ? () => setHover(starNum) : undefined}
            onMouseLeave={interactive ? () => setHover(0) : undefined}
            role={interactive ? 'button' : 'presentation'}
            tabIndex={interactive ? 0 : -1}
            onKeyDown={interactive ? (e) => e.key === 'Enter' && onChange?.(starNum) : undefined}
            aria-label={interactive ? `Rate ${starNum} star${starNum > 1 ? 's' : ''}` : undefined}
          >
            ★
          </span>
        )
      })}
      {showValue && <span className="star-value">{value.toFixed(1)}</span>}
    </div>
  )
}

/**
 * RatingHistogram — Show rating distribution bars.
 *
 * ratings: { 5: 120, 4: 80, 3: 30, 2: 10, 1: 5 }
 */
export function RatingHistogram({ ratings = {}, total, className = '' }) {
  const sum = total || Object.values(ratings).reduce((a, b) => a + b, 0)

  return (
    <div className={`rating-histogram ${className}`}>
      {[5, 4, 3, 2, 1].map((star) => {
        const count = ratings[star] || 0
        const pct = sum ? (count / sum) * 100 : 0
        return (
          <div key={star} className="histogram-row">
            <span className="histogram-label">{star} ★</span>
            <div className="histogram-track">
              <div className="histogram-fill" style={{ width: `${pct}%` }} />
            </div>
            <span className="histogram-count">{count}</span>
          </div>
        )
      })}
    </div>
  )
}
