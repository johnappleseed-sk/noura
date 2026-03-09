'use client'

import { useState } from 'react'

/**
 * Accordion — Collapsible content panels.
 *
 * items: [{ id, title, content, defaultOpen? }]
 * multiple: allow multiple open at once
 */
export default function Accordion({ items = [], multiple = false, className = '' }) {
  const [openIds, setOpenIds] = useState(
    () => new Set(items.filter((i) => i.defaultOpen).map((i) => i.id))
  )

  const toggle = (id) => {
    setOpenIds((prev) => {
      const next = new Set(multiple ? prev : [])
      if (prev.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className={`accordion ${className}`}>
      {items.map((item) => {
        const isOpen = openIds.has(item.id)
        return (
          <div key={item.id} className={`accordion-item ${isOpen ? 'is-open' : ''}`}>
            <button
              type="button"
              className="accordion-trigger"
              aria-expanded={isOpen}
              aria-controls={`acc-${item.id}`}
              onClick={() => toggle(item.id)}
            >
              <span className="accordion-title">{item.title}</span>
              <span className="accordion-icon" aria-hidden="true">{isOpen ? '−' : '+'}</span>
            </button>
            {isOpen && (
              <div id={`acc-${item.id}`} className="accordion-content" role="region">
                {item.content}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
