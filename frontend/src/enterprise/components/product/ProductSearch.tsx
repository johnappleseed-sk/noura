import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { searchApi } from '@/api/searchApi'
import { setSearchQuery } from '@/features/products/productsSlice'
import { useDebounce } from '@/hooks/useDebounce'
import { getPredictiveSuggestions } from '@/utils/predictiveSearch'

/**
 * Renders the ProductSearch component.
 *
 * @returns The rendered component tree.
 */
export const ProductSearch = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const products = useAppSelector((state) => state.products.items)
  const activeSearch = useAppSelector((state) => state.products.filters.searchQuery)
  const [query, setQuery] = useState(activeSearch)
  const [expanded, setExpanded] = useState(false)
  const [remoteSuggestions, setRemoteSuggestions] = useState<string[]>([])
  const [remoteSuggestionsQuery, setRemoteSuggestionsQuery] = useState('')
  const [remoteTrendTags, setRemoteTrendTags] = useState<string[]>([])
  const debouncedQuery = useDebounce(query, 200)
  const localSuggestions = useMemo(
    () => getPredictiveSuggestions(products, debouncedQuery, 6),
    [products, debouncedQuery],
  )
  const suggestions = useMemo(() => {
    const trimmed = debouncedQuery.trim()
    if (!trimmed) {
      return []
    }
    if (remoteSuggestionsQuery === trimmed && remoteSuggestions.length > 0) {
      return remoteSuggestions.slice(0, 6)
    }
    return localSuggestions
  }, [debouncedQuery, localSuggestions, remoteSuggestions, remoteSuggestionsQuery])
  const localTrendTags = useMemo(
    () => Array.from(new Set(products.flatMap((product) => product.tags))).slice(0, 6),
    [products],
  )

  useEffect(() => {
    const trimmed = debouncedQuery.trim()
    if (!trimmed) {
      return
    }

    let cancelled = false

    void searchApi
      .predictive(trimmed)
      .then((items) => {
        if (cancelled) {
          return
        }
        setRemoteSuggestionsQuery(trimmed)
        setRemoteSuggestions(items.slice(0, 6))
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [debouncedQuery])

  useEffect(() => {
    if (products.length === 0 || remoteTrendTags.length > 0) {
      return
    }

    let cancelled = false

    void searchApi
      .trendTags()
      .then((items) => {
        if (cancelled) {
          return
        }
        setRemoteTrendTags(items.slice(0, 6))
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [products, remoteTrendTags.length])

  const displayedSuggestions = useMemo(
    () => (query.trim() ? suggestions : remoteTrendTags.length > 0 ? remoteTrendTags : localTrendTags),
    [localTrendTags, query, remoteTrendTags, suggestions],
  )

  /**
   * Handles submit search.
   */
  const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    dispatch(setSearchQuery(query))
    navigate('/products')
    setExpanded(false)
  }

  /**
   * Executes apply suggestion.
   *
   * @param value The value value.
   * @returns No value.
   */
  const applySuggestion = (value: string): void => {
    setQuery(value)
    dispatch(setSearchQuery(value))
    navigate('/products')
    setExpanded(false)
  }

  return (
    <div className="relative w-full max-w-xl">
      <form className="relative" onSubmit={submitSearch} role="search">
        <input
          aria-label="Search products"
          className="m3-input pr-24"
          onBlur={() => window.setTimeout(() => setExpanded(false), 120)}
          onChange={(event) => {
            setQuery(event.target.value)
            setExpanded(true)
          }}
          onFocus={() => setExpanded(true)}
          placeholder="AI search for products, brands, categories..."
          type="search"
          value={query}
        />
        <button
          className="m3-btn m3-btn-filled absolute right-1 top-1 !h-9 !rounded-full !px-4 !py-1.5 !text-xs uppercase tracking-wide"
          type="submit"
        >
          Search
        </button>
      </form>

      {expanded && displayedSuggestions.length > 0 ? (
        <ul
          aria-label="Predictive search suggestions"
          className="panel absolute z-20 mt-2 w-full overflow-hidden"
          role="listbox"
        >
          {displayedSuggestions.map((suggestion) => (
            <li key={suggestion}>
              <button
                className="w-full px-4 py-2 text-left text-sm transition hover:bg-brand-100/60"
                onClick={() => applySuggestion(suggestion)}
                onMouseDown={() => applySuggestion(suggestion)}
                type="button"
              >
                {suggestion}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
