import { useEffect, useMemo, useRef, useState } from 'react'
import { Icon } from './Icon'

const RECENTS_KEY = 'noura.admin.search.recents'

function normalize(value) {
  return String(value || '').trim().toLowerCase()
}

function safeReadRecents() {
  try {
    const stored = JSON.parse(localStorage.getItem(RECENTS_KEY) || '[]')
    if (!Array.isArray(stored)) return []
    return stored.filter((item) => typeof item === 'string').slice(0, 6)
  } catch (_) {
    return []
  }
}

function safeWriteRecents(values) {
  try {
    localStorage.setItem(RECENTS_KEY, JSON.stringify(values))
  } catch (_) {
    // ignore
  }
}

export function CommandPalette({ open, onClose, items = [] }) {
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const [recents, setRecents] = useState(() => safeReadRecents())
  const inputRef = useRef(null)

  const filtered = useMemo(() => {
    const q = normalize(query)
    if (!q) return items
    return items.filter((item) => {
      const haystack = `${item.label || ''} ${item.description || ''} ${item.keywords || ''}`
      return normalize(haystack).includes(q)
    })
  }, [items, query])

  const visibleItems = useMemo(() => filtered.slice(0, 12), [filtered])

  useEffect(() => {
    if (!open) return
    setQuery('')
    setActiveIndex(0)
    setRecents(safeReadRecents())
    const id = setTimeout(() => inputRef.current?.focus(), 0)
    return () => clearTimeout(id)
  }, [open])

  useEffect(() => {
    if (!open) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose?.()
        return
      }

      if (event.key === 'ArrowDown') {
        event.preventDefault()
        setActiveIndex((current) => Math.min(current + 1, Math.max(visibleItems.length - 1, 0)))
        return
      }

      if (event.key === 'ArrowUp') {
        event.preventDefault()
        setActiveIndex((current) => Math.max(current - 1, 0))
        return
      }

      if (event.key === 'Enter') {
        const selected = visibleItems[activeIndex]
        if (!selected) return
        event.preventDefault()
        trackRecentSearch(query || selected.label)
        selected.onSelect?.()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [activeIndex, onClose, open, visibleItems])

  if (!open) return null

  const showRecents = !normalize(query) && recents.length

  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label="Global search">
      <button type="button" className="overlay-backdrop" aria-label="Close search" onClick={onClose} />

      <div className="modal command-modal" role="document">
        <div className="modal-head">
          <Icon name="search" />
          <input
            ref={inputRef}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setActiveIndex(0)
            }}
            placeholder="Search pages and actions…"
            aria-label="Search"
          />
          <span className="kbd">Esc</span>
        </div>

        {showRecents ? (
          <div className="command-section">
            <p className="command-label">Recent</p>
            <div className="command-chips">
              {recents.map((item) => (
                <button
                  type="button"
                  key={item}
                  className="chip"
                  onClick={() => setQuery(item)}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <div className="modal-body">
          {visibleItems.length ? (
            <ul className="command-list" role="listbox" aria-label="Search results">
              {visibleItems.map((item, index) => (
                <li key={item.id || item.label} role="option" aria-selected={index === activeIndex}>
                  <button
                    type="button"
                    className={`command-item${index === activeIndex ? ' active' : ''}`}
                    onMouseEnter={() => setActiveIndex(index)}
                    onClick={() => {
                      trackRecentSearch(query || item.label)
                      item.onSelect?.()
                    }}
                  >
                    {item.icon ? <Icon name={item.icon} className="command-icon" /> : null}
                    <span className="command-text">
                      <strong>{item.label}</strong>
                      {item.description ? <span className="command-desc">{item.description}</span> : null}
                    </span>
                    {item.hint ? <span className="command-hint">{item.hint}</span> : null}
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="empty-copy">No results match your search.</p>
          )}
        </div>
      </div>
    </div>
  )
}

export function trackRecentSearch(value) {
  const normalized = normalize(value)
  if (!normalized) return
  const next = [normalized, ...safeReadRecents().filter((item) => item !== normalized)].slice(0, 6)
  safeWriteRecents(next)
}
