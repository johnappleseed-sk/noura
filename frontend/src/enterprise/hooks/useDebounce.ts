import { useEffect, useState } from 'react'

/**
 * Executes use debounce.
 *
 * @param value The value value.
 * @param delay The delay value.
 * @returns The result of use debounce.
 */
export const useDebounce = <T>(value: T, delay = 300): T => {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedValue(value), delay)

    return () => window.clearTimeout(timer)
  }, [value, delay])

  return debouncedValue
}
