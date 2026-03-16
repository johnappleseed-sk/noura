/**
 * Safely converts an arbitrary value into a finite number.
 *
 * @param {unknown} value Raw value.
 * @returns {number} Safe numeric value.
 */
export function safeNumber(value) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
}

/**
 * Formats a date value for chart labels.
 *
 * @param {unknown} value Raw date value.
 * @returns {string} User-friendly date label.
 */
export function formatDateLabel(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'
  return date.toLocaleDateString()
}

/**
 * Groups items by day and sums the mapped value.
 *
 * @template T
 * @param {T[]} items Source items.
 * @param {(item: T) => unknown} getDate Mapper that returns a date-like value.
 * @param {(item: T) => unknown} getValue Mapper that returns a numeric value.
 * @returns {{labels: string[], values: number[]}} Grouped labels and values.
 */
export function groupByDay(items, getDate, getValue) {
  const totalsByDay = new Map()

  for (const item of items) {
    const rawDate = getDate(item)
    const date = new Date(rawDate)

    if (Number.isNaN(date.getTime())) {
      continue
    }

    const key = date.toISOString().slice(0, 10)
    const value = safeNumber(getValue(item))
    totalsByDay.set(key, (totalsByDay.get(key) || 0) + value)
  }

  const labels = Array.from(totalsByDay.keys()).sort((a, b) => a.localeCompare(b))
  const values = labels.map((label) => totalsByDay.get(label) || 0)

  return { labels, values }
}
