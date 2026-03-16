/**
 * Admin Analytics Dashboard
 *
 * Displays analytics from commerce APIs:
 * - Revenue trends
 * - Orders trends
 * - Category performance
 * - Customer segmentation
 * - Geographic distribution
 *
 * This refactor improves:
 * - defensive coding
 * - async safety
 * - maintainability
 * - chart data generation
 *
 * Business logic and API behavior remain unchanged.
 */

import { useEffect, useMemo, useState } from 'react'
import { Bar, Line, Pie } from 'react-chartjs-2'
import 'chart.js/auto'

import { getDashboardSummary } from '../shared/api/endpoints/dashboardApi'
import { getCategoryAnalytics } from '../shared/api/endpoints/analyticsApi'
import { listOrders } from '../shared/api/endpoints/ordersApi'
import { getCustomerAnalytics } from '../shared/api/endpoints/customerAnalyticsApi'

import { Spinner } from '../shared/ui/Spinner'
import { Panel } from '../shared/ui/Panel'
import { PageHeader } from '../shared/ui/PageHeader'
import { formatCurrency } from '../shared/ui/formatters'
import { useToastFeedback } from '../shared/ui/useToastFeedback'

import '../styles/pages/AdminAnalyticsDashboard.css'
/**
 * Convert value to ISO safely.
 */
function toIso(value) {
  if (!value) return undefined
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString()
}

/**
 * Safely ensure an object.
 */
function safeObject(value) {
  return value && typeof value === 'object' ? value : {}
}

/**
 * Group items by calendar day.
 */
function groupByDay(items, getDate, getValue) {
  const map = new Map()

  for (const item of items) {
    const raw = getDate(item)
    if (!raw) continue

    const date = new Date(raw)
    if (Number.isNaN(date.getTime())) continue

    const dayKey = date.toISOString().slice(0, 10)

    const value = Number(getValue(item) || 0)
    map.set(dayKey, (map.get(dayKey) || 0) + value)
  }

  const labels = Array.from(map.keys())
  const values = labels.map((l) => map.get(l) || 0)

  return { labels, values }
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

export function AdminAnalyticsDashboard() {

  const [rangeDays, setRangeDays] = useState(14)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useToastFeedback({ errorMessage: error })

  const [summary, setSummary] = useState(null)
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])
  const [customerAnalytics, setCustomerAnalytics] = useState(null)

  /**
   * Load base dashboard data.
   */
  useEffect(() => {
    let active = true

    async function loadBase() {
      setError('')

      try {
        const [nextSummary, nextCustomer] = await Promise.all([
          getDashboardSummary(),
          getCustomerAnalytics()
        ])

        if (!active) return

        setSummary(nextSummary || null)
        setCustomerAnalytics(nextCustomer || null)

      } catch (err) {
        if (!active) return
        setError(getErrorMessage(err, 'Failed to load dashboard summary.'))
      }
    }

    loadBase()

    return () => {
      active = false
    }

  }, [])

  /**
   * Load time-range dependent analytics.
   */
  useEffect(() => {
    let active = true

    async function loadRange() {

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

        if (!active) return

        const rawOrders = Array.isArray(ordersPage?.content)
          ? ordersPage.content
          : []

        const filteredOrders = rawOrders.filter((order) => {

          if (!order?.createdAt) return false

          const date = new Date(order.createdAt)

          if (Number.isNaN(date.getTime())) return false

          return date >= from && date <= now
        })

        setOrders(filteredOrders)
        setCategoryAnalytics(Array.isArray(categories) ? categories : [])

      } catch (err) {

        if (!active) return

        setError(getErrorMessage(err, 'Failed to load analytics.'))
        setOrders([])
        setCategoryAnalytics([])

      } finally {

        if (active) setLoading(false)

      }
    }

    loadRange()

    return () => {
      active = false
    }

  }, [rangeDays])

  /**
   * Revenue trend chart.
   */
  const revenueTrend = useMemo(() => {

    const { labels, values } = groupByDay(
      [...orders].reverse(),
      (o) => o.createdAt,
      (o) => Number(o.totalAmount || 0)
    )

    return {
      labels,
      datasets: [{
        label: 'Revenue',
        data: values,
        borderColor: '#a35f2d',
        backgroundColor: 'rgba(163,95,45,0.15)',
        tension: 0.35,
        fill: true
      }]
    }

  }, [orders])

  /**
   * Orders trend chart.
   */
  const ordersTrend = useMemo(() => {

    const { labels, values } = groupByDay(
      [...orders].reverse(),
      (o) => o.createdAt,
      () => 1
    )

    return {
      labels,
      datasets: [{
        label: 'Orders',
        data: values,
        backgroundColor: '#0f5d8c',
        borderRadius: 8
      }]
    }

  }, [orders])

  /**
   * Category revenue chart.
   */
  const categoryChart = useMemo(() => {

    const labels = categoryAnalytics.map(
      (c) => c.categoryName || c.name || 'Unknown'
    )

    const values = categoryAnalytics.map(
      (c) => Number(c.revenue || 0)
    )

    return {
      labels,
      datasets: [{
        label: 'Revenue',
        data: values,
        backgroundColor: '#a35f2d',
        borderRadius: 8
      }]
    }

  }, [categoryAnalytics])

  const segmentCounts = safeObject(customerAnalytics?.segmentCounts)
  const countryCounts = safeObject(customerAnalytics?.countryCounts)

  const segmentsChart = useMemo(() => {

    const labels = Object.keys(segmentCounts)
    const values = labels.map((k) => Number(segmentCounts[k] || 0))

    return {
      labels,
      datasets: [{
        label: 'Segments',
        data: values,
        backgroundColor: [
          '#a35f2d',
          '#0f5d8c',
          '#2f7d4a',
          '#a96c05',
          '#b42318',
          '#1f335e'
        ]
      }]
    }

  }, [segmentCounts])

  const countriesChart = useMemo(() => {

    const labels = Object.keys(countryCounts)
    const values = labels.map((k) => Number(countryCounts[k] || 0))

    return {
      labels,
      datasets: [{
        label: 'Countries',
        data: values,
        backgroundColor: [
          '#2f7d4a',
          '#0f5d8c',
          '#a96c05',
          '#b42318',
          '#1f335e',
          '#a35f2d'
        ]
      }]
    }

  }, [countryCounts])

  if (loading && !summary) {
    return <Spinner label="Loading analytics dashboard..." />
  }

  return (
    <div className="page admin-analytics-page">

      <PageHeader
        title="Admin Analytics Dashboard"
        description="Revenue, orders, categories, and customer insights from commerce APIs."
      >
        <div className="admin-analytics-range" role="group" aria-label="Analytics range filter">
          {[7, 14, 30].map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setRangeDays(d)}
              className={rangeDays === d ? 'active' : ''}
            >
              {d}D
            </button>
          ))}
        </div>
      </PageHeader>

      {summary && (
        <div className="admin-analytics-kpi-grid">

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Revenue</span>
            <h3>{formatCurrency(summary.revenue)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Orders</span>
            <h3>{Number(summary.ordersCount || 0)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Users</span>
            <h3>{Number(summary.usersCount || 0)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Stores</span>
            <h3>{Number(summary.storesCount || 0)}</h3>
          </div>

        </div>
      )}

      {loading ? (
        <Spinner label="Refreshing analytics..." />
      ) : (
        <div className="admin-analytics-grid">

          <Panel
            title="Revenue Trend"
            description={`Daily revenue (last ${rangeDays} days).`}
            className="admin-analytics-card admin-analytics-card-wide"
          >
            <div className="admin-analytics-chart-area">
              {revenueTrend.labels.length
                ? <Line data={revenueTrend} />
                : <p className="admin-analytics-empty">No orders in range.</p>}
            </div>
          </Panel>

          <Panel
            title="Orders Trend"
            description={`Orders per day (last ${rangeDays} days).`}
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {ordersTrend.labels.length
                ? <Bar data={ordersTrend} />
                : <p className="admin-analytics-empty">No orders in range.</p>}
            </div>
          </Panel>

          <Panel
            title="Revenue by Category"
            description="Category performance for the selected period."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {categoryChart.labels.length
                ? <Bar data={categoryChart} />
                : <p className="admin-analytics-empty">No category analytics.</p>}
            </div>
          </Panel>

          <Panel
            title="Customer Segments"
            description="Customer segmentation distribution."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {segmentsChart.labels.length
                ? <Pie data={segmentsChart} />
                : <p className="admin-analytics-empty">No customer segment data.</p>}
            </div>
          </Panel>

          <Panel
            title="Customer Countries"
            description="Customer distribution by country."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {countriesChart.labels.length
                ? <Pie data={countriesChart} />
                : <p className="admin-analytics-empty">No customer geography data.</p>}
            </div>
          </Panel>

          <Panel
            title="Top Products"
            description="High-level snapshot from the admin summary."
            className="admin-analytics-card"
          >
            {summary?.topProducts?.length ? (
              <ul className="admin-analytics-list">
                {summary.topProducts.map((name, idx) => (
                  <li key={`${name}-${idx}`}>{name}</li>
                ))}
              </ul>
            ) : (
              <p className="admin-analytics-empty">No top products available.</p>
            )}
          </Panel>

          <Panel
            title="Store Performance"
            description="High-level snapshot from the admin summary."
            className="admin-analytics-card"
          >
            {summary?.storePerformance?.length ? (
              <ul className="admin-analytics-list">
                {summary.storePerformance.map((row, idx) => (
                  <li key={`${row}-${idx}`}>{row}</li>
                ))}
              </ul>
            ) : (
              <p className="admin-analytics-empty">No store performance data available.</p>
            )}
          </Panel>

        </div>
      )}
    </div>
  )
}

export default AdminAnalyticsDashboard
