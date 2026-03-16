'use client'

import { startTransition, useEffect, useRef, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { predictiveSearch } from '@/lib/api'
import { GlobalSearchBar } from './Search'

const CACHE_TTL_MS = 90 * 1000
const MIN_QUERY_LENGTH = 2

const scopeLabel = {
  products: 'Products',
  product: 'Products',
  categories: 'Categories',
  category: 'Categories',
  brands: 'Brands',
  brand: 'Brands',
  tags: 'Tags',
  tag: 'Tags',
  all: 'Suggestions'
}

function normalizeScope(scope) {
  return String(scope || 'all').trim().toLowerCase()
}

function resolveSuggestionHref(value) {
  return `/products?q=${encodeURIComponent(value)}`
}

function mapSuggestionSections(rows) {
  const buckets = new Map()

  for (const row of Array.isArray(rows) ? rows : []) {
    const value = String(row?.value || '').trim()
    if (!value) continue

    const scope = normalizeScope(row?.scope)
    const title = scopeLabel[scope] || 'Suggestions'
    if (!buckets.has(title)) {
      buckets.set(title, [])
    }

    const list = buckets.get(title)
    if (list.some((item) => item.label.toLowerCase() === value.toLowerCase())) {
      continue
    }

    list.push({
      label: value,
      href: resolveSuggestionHref(value),
      meta: scope !== 'all' ? scope : null
    })
  }

  return Array.from(buckets.entries())
    .map(([title, items]) => ({ title, items: items.slice(0, 6) }))
    .filter((section) => section.items.length > 0)
}

/**
 * Header search with predictive suggestions.
 * Uses debounce + short-lived in-memory cache to minimize API load on repeated queries.
 */
export default function HeaderSearch({ className = '' }) {
  const router = useRouter()
  const pathname = usePathname()
  const [suggestions, setSuggestions] = useState([])
  const requestIdRef = useRef(0)
  const debounceTimerRef = useRef(null)
  const cacheRef = useRef(new Map())

  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, [])

  const handleSearch = (query) => {
    const params = new URLSearchParams()
    params.set('q', query)
    router.push(`/products?${params.toString()}`)
  }

  const handleSuggest = (query) => {
    const normalizedQuery = String(query || '').trim()
    if (normalizedQuery.length < MIN_QUERY_LENGTH) {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
      startTransition(() => setSuggestions([]))
      return
    }

    const cacheKey = normalizedQuery.toLowerCase()
    const cached = cacheRef.current.get(cacheKey)
    if (cached && cached.expiresAt > Date.now()) {
      startTransition(() => setSuggestions(cached.sections))
      return
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    debounceTimerRef.current = setTimeout(async () => {
      const requestId = requestIdRef.current + 1
      requestIdRef.current = requestId
      try {
        const rows = await predictiveSearch(normalizedQuery)
        if (requestId !== requestIdRef.current) {
          return
        }

        const sections = mapSuggestionSections(rows)
        cacheRef.current.set(cacheKey, {
          sections,
          expiresAt: Date.now() + CACHE_TTL_MS
        })
        startTransition(() => setSuggestions(sections))
      } catch {
        if (requestId === requestIdRef.current) {
          startTransition(() => setSuggestions([]))
        }
      }
    }, 220)
  }

  return (
    <GlobalSearchBar
      className={`enterprise-header-search ${className}`.trim()}
      placeholder="Search catalog..."
      onSearch={handleSearch}
      onSuggest={handleSuggest}
      onNavigate={(href) => router.push(href)}
      suggestions={suggestions}
      key={pathname}
    />
  )
}
