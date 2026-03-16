import { useEffect, useMemo, useState } from 'react'
import { getCategoryAnalytics } from '../../../shared/api/endpoints/analyticsApi'
import { getCustomerAnalytics } from '../../../shared/api/endpoints/customerAnalyticsApi'
import { getDashboardSummary } from '../../../shared/api/endpoints/dashboardApi'
import { listOrders } from '../../../shared/api/endpoints/ordersApi'

/**
 * Returns an ISO string for valid date values.
 *
 * @param {Date} value Date value.
 * @returns {string|undefined} ISO timestamp.
 */
function toIso(value) {
  if (!value) {
    return undefined
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return undefined
  }

  return date.toISOString()
}

/**
 * Safely returns an object for map-like analytics payloads.
 *
 * @param {unknown} value Potential object value.
 * @returns {Record<string, number>} Safe object.
 */
function safeObject(value) {
  return value && typeof value === 'object' ? value : {}
}

/**
 * Filters orders to the selected date window.
 *
 * @param {Array<Record<string, unknown>>} orders Order list.
 * @param {Date} from Start date.
 * @param {Date} to End date.
 * @returns {Array<Record<string, unknown>>} Filtered order list.
 */
function filterOrdersByRange(orders, from, to) {
  return orders.filter((order) => {
    if (!order?.createdAt) {
      return false
    }

    const date = new Date(order.createdAt)
    if (Number.isNaN(date.getTime())) {
      return false
    }

    return date >= from && date <= to
  })
}

/**
 * Loads analytics data and exposes state for analytics dashboards.
 *
 * @param {number} [initialRangeDays=14] Default selected day range.
 * @returns {{
 *  rangeDays: number,
 *  setRangeDays: (value: number) => void,
 *  loading: boolean,
 *  error: string,
 *  summary: Record<string, unknown>|null,
 *  orders: Array<Record<string, unknown>>,
 *  categoryAnalytics: Array<Record<string, unknown>>,
 *  customerAnalytics: Record<string, unknown>|null,
 *  segmentCounts: Record<string, number>,
 *  countryCounts: Record<string, number>,
 * }} Analytics data state.
 */
export function useAnalyticsData(initialRangeDays = 14) {
  const [rangeDays, setRangeDays] = useState(initialRangeDays)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [summary, setSummary] = useState(null)
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])
  const [customerAnalytics, setCustomerAnalytics] = useState(null)

  useEffect(() => {
    let active = true

    async function loadBaseAnalytics() {
      try {
        const [nextSummary, nextCustomerAnalytics] = await Promise.all([
          getDashboardSummary(),
          getCustomerAnalytics()
        ])

        if (!active) {
          return
        }

        setSummary(nextSummary || null)
        setCustomerAnalytics(nextCustomerAnalytics || null)
      } catch (loadError) {
        if (!active) {
          return
        }
        setError(loadError?.message || 'Failed to load dashboard summary.')
      }
    }

    loadBaseAnalytics()

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true

    async function loadRangeAnalytics() {
      setLoading(true)
      setError('')

      const now = new Date()
      const from = new Date(now.getTime() - rangeDays * 86400000)

      try {
        const [ordersPage, categories] = await Promise.all([
          listOrders({
            page: 0,
            size: 250,
            sortBy: 'createdAt',
            direction: 'desc'
          }),
          getCategoryAnalytics({
            from: toIso(from),
            to: toIso(now)
          })
        ])

        if (!active) {
          return
        }

        const nextOrders = Array.isArray(ordersPage?.content) ? ordersPage.content : []
        const nextCategories = Array.isArray(categories) ? categories : []

        setOrders(filterOrdersByRange(nextOrders, from, now))
        setCategoryAnalytics(nextCategories)
      } catch (loadError) {
        if (!active) {
          return
        }

        setError(loadError?.message || 'Failed to load analytics.')
        setOrders([])
        setCategoryAnalytics([])
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadRangeAnalytics()

    return () => {
      active = false
    }
  }, [rangeDays])

  const segmentCounts = useMemo(
    () => safeObject(customerAnalytics?.segmentCounts),
    [customerAnalytics]
  )

  const countryCounts = useMemo(
    () => safeObject(customerAnalytics?.countryCounts),
    [customerAnalytics]
  )

  return {
    rangeDays,
    setRangeDays,
    loading,
    error,
    summary,
    orders,
    categoryAnalytics,
    customerAnalytics,
    segmentCounts,
    countryCounts
  }
}
