'use client'

import { useState, useRef, useEffect, useCallback } from 'react'
import Link from 'next/link'

/**
 * GlobalSearchBar — Full-featured search with autosuggest dropdown.
 */
export function GlobalSearchBar({
  onSearch,
  onSuggest,
  suggestions = [],
  placeholder = 'Search products, categories, brands…',
  className = ''
}) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [focusIdx, setFocusIdx] = useState(-1)
  const wrapRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    const handler = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleChange = (e) => {
    const v = e.target.value
    setQuery(v)
    setFocusIdx(-1)
    if (v.trim().length >= 2) {
      onSuggest?.(v.trim())
      setOpen(true)
    } else {
      setOpen(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (query.trim()) {
      onSearch?.(query.trim())
      setOpen(false)
    }
  }

  const handleKeyDown = (e) => {
    if (!open || !flatItems.length) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setFocusIdx((i) => Math.min(i + 1, flatItems.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setFocusIdx((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && focusIdx >= 0) {
      e.preventDefault()
      const item = flatItems[focusIdx]
      if (item?.href) {
        window.location.href = item.href
      }
      setOpen(false)
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  const flatItems = suggestions.flatMap(s => s.items || [])

  return (
    <div ref={wrapRef} className={`search-bar-wrap ${className}`}>
      <form onSubmit={handleSubmit} role="search">
        <span className="search-bar-icon">🔍</span>
        <input
          ref={inputRef}
          type="search"
          className="search-bar"
          value={query}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onFocus={() => query.trim().length >= 2 && setOpen(true)}
          placeholder={placeholder}
          aria-label="Search"
          autoComplete="off"
        />
        {query && (
          <button
            type="button"
            className="search-bar-clear"
            onClick={() => { setQuery(''); setOpen(false); inputRef.current?.focus() }}
            aria-label="Clear search"
          >✕</button>
        )}
      </form>
      {open && suggestions.length > 0 && (
        <div className="search-suggestions" role="listbox">
          {suggestions.map((section, si) => (
            <div key={si} className="search-suggestions-section">
              {section.title && (
                <div className="search-suggestions-title">{section.title}</div>
              )}
              {section.items?.map((item, ii) => {
                const globalIdx = suggestions.slice(0, si).reduce((a, s) => a + (s.items?.length || 0), 0) + ii
                return (
                  <Link
                    key={ii}
                    href={item.href || '#'}
                    className={`search-suggestion-item ${globalIdx === focusIdx ? 'focused' : ''}`}
                    onClick={() => setOpen(false)}
                    role="option"
                    aria-selected={globalIdx === focusIdx}
                  >
                    {item.thumb && (
                      <span className="search-suggestion-thumb">
                        {typeof item.thumb === 'string' ? item.thumb : item.thumb}
                      </span>
                    )}
                    <span>{item.label}</span>
                    {item.meta && <span style={{ marginLeft: 'auto', color: 'var(--muted)', fontSize: '0.78rem' }}>{item.meta}</span>}
                  </Link>
                )
              })}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * SearchHistory — Recent searches display.
 */
export function SearchHistory({ items = [], onSelect, onClear, onRemove }) {
  if (!items.length) return null

  return (
    <div className="search-history">
      <div className="search-history-header">
        <span className="search-suggestions-title">Recent Searches</span>
        {onClear && (
          <button type="button" className="button link sm" onClick={onClear}>Clear all</button>
        )}
      </div>
      <div className="search-history-list">
        {items.map((item, i) => (
          <div key={i} className="search-history-item">
            <button
              type="button"
              className="search-suggestion-item"
              onClick={() => onSelect?.(item)}
            >
              <span>🕘</span>
              <span>{item}</span>
            </button>
            {onRemove && (
              <button
                type="button"
                className="search-history-remove"
                onClick={() => onRemove(item)}
                aria-label={`Remove ${item}`}
              >✕</button>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

/**
 * FacetedFilters — Filter panel with checkbox, color swatch, and range facets.
 */
export function FacetedFilters({ facets = [], values = {}, onChange, onReset }) {
  const handleCheck = (facetId, optionValue) => {
    const currentVals = values[facetId] || []
    const newVals = currentVals.includes(optionValue)
      ? currentVals.filter(v => v !== optionValue)
      : [...currentVals, optionValue]
    onChange?.({ ...values, [facetId]: newVals })
  }

  const handleRange = (facetId, range) => {
    onChange?.({ ...values, [facetId]: range })
  }

  const activeCount = Object.values(values).reduce((a, v) => a + (Array.isArray(v) ? v.length : v ? 1 : 0), 0)

  return (
    <div className="filter-panel">
      {activeCount > 0 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{activeCount} active filters</span>
          <button type="button" className="button link sm" onClick={onReset}>Clear all</button>
        </div>
      )}
      {facets.map(facet => (
        <div key={facet.id} className="filter-section">
          <span className="filter-section-title">{facet.label}</span>
          {facet.type === 'checkbox' && (
            <div className="filter-options">
              {facet.options?.map(opt => (
                <label key={opt.value} className="check-label">
                  <input
                    type="checkbox"
                    className="check-input"
                    checked={(values[facet.id] || []).includes(opt.value)}
                    onChange={() => handleCheck(facet.id, opt.value)}
                  />
                  <span className="check-box" />
                  <span className="check-text">
                    {opt.label}
                    {opt.count != null && <span style={{ color: 'var(--muted)', marginLeft: 6, fontSize: '0.78rem' }}>({opt.count})</span>}
                  </span>
                </label>
              ))}
            </div>
          )}
          {facet.type === 'color' && (
            <div className="filter-color-grid">
              {facet.options?.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  className={`filter-color-swatch ${(values[facet.id] || []).includes(opt.value) ? 'active' : ''}`}
                  style={{ backgroundColor: opt.hex }}
                  onClick={() => handleCheck(facet.id, opt.value)}
                  aria-label={opt.label}
                  title={opt.label}
                />
              ))}
            </div>
          )}
          {facet.type === 'range' && (
            <div className="slider-wrap">
              <div className="range-labels">
                <span>{facet.formatValue?.(values[facet.id]?.[0] ?? facet.min) ?? (values[facet.id]?.[0] ?? facet.min)}</span>
                <span>{facet.formatValue?.(values[facet.id]?.[1] ?? facet.max) ?? (values[facet.id]?.[1] ?? facet.max)}</span>
              </div>
              <div className="range-slider-track">
                <input
                  type="range"
                  className="range-slider range-low"
                  min={facet.min}
                  max={facet.max}
                  step={facet.step || 1}
                  value={values[facet.id]?.[0] ?? facet.min}
                  onChange={(e) => handleRange(facet.id, [Number(e.target.value), values[facet.id]?.[1] ?? facet.max])}
                />
                <input
                  type="range"
                  className="range-slider range-high"
                  min={facet.min}
                  max={facet.max}
                  step={facet.step || 1}
                  value={values[facet.id]?.[1] ?? facet.max}
                  onChange={(e) => handleRange(facet.id, [values[facet.id]?.[0] ?? facet.min, Number(e.target.value)])}
                />
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

/**
 * ActiveFilters — Display current active filter chips with remove.
 */
export function ActiveFilters({ filters = [], onRemove, onClearAll }) {
  if (!filters.length) return null

  return (
    <div className="active-filters">
      {filters.map((f, i) => (
        <span key={i} className="active-filter">
          {f.label}
          <button type="button" onClick={() => onRemove?.(f)} aria-label={`Remove ${f.label}`}>✕</button>
        </span>
      ))}
      {filters.length > 1 && (
        <button type="button" className="button link sm" onClick={onClearAll}>Clear all</button>
      )}
    </div>
  )
}
