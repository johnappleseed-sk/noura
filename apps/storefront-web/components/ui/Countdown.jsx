'use client'

import { useState, useEffect, useRef } from 'react'

/**
 * CountdownTimer — Countdown to a target date.
 */
export function CountdownTimer({ targetDate, label, onComplete, className = '' }) {
  const [remaining, setRemaining] = useState(calcRemaining(targetDate))

  useEffect(() => {
    const timer = setInterval(() => {
      const r = calcRemaining(targetDate)
      setRemaining(r)
      if (r.total <= 0) {
        clearInterval(timer)
        onComplete?.()
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [targetDate, onComplete])

  if (remaining.total <= 0) return null

  return (
    <div className={`countdown ${className}`}>
      {label && <span className="countdown-label">{label}</span>}
      <div className="countdown-units">
        {remaining.days > 0 && <Unit value={remaining.days} label="Days" />}
        <Unit value={remaining.hours} label="Hours" />
        <Unit value={remaining.minutes} label="Min" />
        <Unit value={remaining.seconds} label="Sec" />
      </div>
    </div>
  )
}

function Unit({ value, label }) {
  return (
    <div className="countdown-unit">
      <span className="countdown-value">{String(value).padStart(2, '0')}</span>
      <span className="countdown-unit-label">{label}</span>
    </div>
  )
}

function calcRemaining(target) {
  const diff = Math.max(0, new Date(target).getTime() - Date.now())
  return {
    total: diff,
    days: Math.floor(diff / 86400000),
    hours: Math.floor((diff % 86400000) / 3600000),
    minutes: Math.floor((diff % 3600000) / 60000),
    seconds: Math.floor((diff % 60000) / 1000),
  }
}

/**
 * Carousel — Horizontal scrollable with navigation arrows.
 */
export function Carousel({ children, title, className = '' }) {
  const scrollRef = useRef(null)

  const scroll = (dir) => {
    if (!scrollRef.current) return
    const amount = scrollRef.current.clientWidth * 0.8
    scrollRef.current.scrollBy({ left: dir * amount, behavior: 'smooth' })
  }

  return (
    <div className={`carousel ${className}`}>
      {title && (
        <div className="carousel-header">
          <h3 className="carousel-title">{title}</h3>
          <div className="carousel-nav">
            <button type="button" className="carousel-arrow" onClick={() => scroll(-1)} aria-label="Scroll left">‹</button>
            <button type="button" className="carousel-arrow" onClick={() => scroll(1)} aria-label="Scroll right">›</button>
          </div>
        </div>
      )}
      <div ref={scrollRef} className="carousel-track">
        {children}
      </div>
    </div>
  )
}
