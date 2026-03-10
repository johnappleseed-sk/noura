import { useEffect, useMemo, useState } from 'react'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip
} from 'chart.js'
import { Bar, Doughnut, Line } from 'react-chartjs-2'
import { listOrders } from '../shared/api/endpoints/ordersApi'
import { getCategoryAnalytics } from '../shared/api/endpoints/analyticsApi'
import { formatCurrency } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'
import { Panel } from '../shared/ui/Panel'
import { PageHeader } from '../shared/ui/PageHeader'
import { CommerceAnalyticsOverviewPanel } from '../features/analytics/CommerceAnalyticsOverviewPanel'

/**
 * Register Chart.js modules once for the charts used on this page.
 */
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend)

/**
 * Shared number formatter for non-currency metrics.
 */
const NUMBER_FORMATTER = new Intl.NumberFormat('en-US', {
  maximumFractionDigits: 0
})

/**
 * Shared date formatter for trend chart labels.
 */
const DATE_LABEL_FORMATTER = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric'
})

/**
 * Centralized metadata for each supported category metric.
 * This avoids repeated conditional strings throughout the component.
 */
const CATEGORY_METRIC_META = {
  revenue: {
    datasetLabel: 'Revenue',
    title: 'Revenue by category',
    description: 'Top categories ranked by revenue in the default analytics window.',
    distributionTitle: 'Category distribution'
  },
  unitsSold: {
    datasetLabel: 'Units sold',
    title: 'Units sold by category',
    description: 'Top categories ranked by units sold in the default analytics window.',
    distributionTitle: 'Units distribution'
  },
  currentStock: {
    datasetLabel: 'Current stock',
    title: 'Current stock by category',
    description: 'Top categories ranked by current on-hand stock (inventory snapshot).',
    distributionTitle: 'Stock distribution'
  }
}

/**
 * Safely sum numeric values.
 *
 * @param {Array<number|string>} values
 * @returns {number}
 */
function sum(values = []) {
  return values.reduce((total, value) => total + Number(value || 0), 0)
}

/**
 * Convert a Date object into a stable YYYY-MM-DD key.
 *
 * @param {Date} date
 * @returns {string}
 */
function dayKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Build an array of Date objects for the last N days, including today.
 * The array is ordered from oldest to newest.
 *
 * @param {number} n
 * @returns {Date[]}
 */
function lastNDays(n) {
  const now = new Date()
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - (n - 1))

  const days = []
  for (let i = 0; i < n; i += 1) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    days.push(d)
  }
  return days
}

/**
 * Build a reusable color palette sized to the requested count.
 *
 * @param {number} count
 * @returns {string[]}
 */
function buildPalette(count) {
  const base = [
    '#a35f2d',
    '#0f5d8c',
    '#2f7d4a',
    '#a96c05',
    '#b42318',
    '#1f335e',
    '#ccb792',
    '#75614c'
  ]

  return Array.from({ length: count }, (_, index) => base[index % base.length])
}

/**
 * Resolve a numeric metric value from a category analytics item.
 *
 * Supported metrics:
 * - revenue
 * - unitsSold
 * - currentStock
 *
 * @param {object} item
 * @param {'revenue'|'unitsSold'|'currentStock'} metric
 * @returns {number}
 */
function metricValue(item, metric) {
  if (!item) return 0
  if (metric === 'revenue') return Number(item.revenue || 0)
  if (metric === 'unitsSold') return Number(item.unitsSold || 0)
  if (metric === 'currentStock') return Number(item.currentStock || 0)
  return 0
}

/**
 * Determine the most useful category metric to show.
 *
 * Priority:
 * 1. revenue
 * 2. unitsSold
 * 3. currentStock
 *
 * Falls back to revenue when all totals are zero.
 *
 * @param {Array<object>} items
 * @returns {'revenue'|'unitsSold'|'currentStock'}
 */
function resolveCategoryMetric(items = []) {
  const revenueTotal = sum(items.map((item) => metricValue(item, 'revenue')))
  if (revenueTotal > 0) return 'revenue'

  const unitsTotal = sum(items.map((item) => metricValue(item, 'unitsSold')))
  if (unitsTotal > 0) return 'unitsSold'

  const stockTotal = sum(items.map((item) => metricValue(item, 'currentStock')))
  if (stockTotal > 0) return 'currentStock'

  return 'revenue'
}

/**
 * Format a metric for chart ticks and tooltip display.
 *
 * @param {number|string} value
 * @param {'revenue'|'unitsSold'|'currentStock'} metric
 * @returns {string}
 */
function formatMetric(value, metric) {
  if (metric === 'revenue') return formatCurrency(value)
  return NUMBER_FORMATTER.format(Number(value || 0))
}

/**
 * Analytics page showing:
 * - 14-day revenue trend
 * - top categories by best available metric
 * - category distribution doughnut chart
 *
 * Existing business logic is preserved:
 * - uses latest orders from API
 * - uses category analytics from API
 * - auto-selects revenue/units/stock based on available totals
 */
export function AnalyticsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])

  /**
   * Load orders and category analytics in parallel.
   * Uses an "active" guard to avoid state updates after unmount.
   */
  useEffect(() => {
    let active = true

    async function load() {
      setLoading(true)
      setError('')

      try {
        const [ordersPage, categories] = await Promise.all([
          listOrders({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }),
          getCategoryAnalytics()
        ])

        if (!active) return

        setOrders(ordersPage?.content || [])
        setCategoryAnalytics(Array.isArray(categories) ? categories : [])
      } catch (err) {
        if (!active) return
        setError(err?.message || 'Unable to load analytics.')
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      active = false
    }
  }, [])

  /**
   * Build a 14-day revenue trend from order creation dates.
   * Only days inside the last 14-day window are included.
   */
  const revenueTrend = useMemo(() => {
    const days = lastNDays(14)
    const keys = days.map(dayKey)
    const labels = days.map((date) => DATE_LABEL_FORMATTER.format(date))
    const totals = Object.fromEntries(keys.map((key) => [key, 0]))

    for (const order of orders) {
      if (!order?.createdAt) continue

      const created = new Date(order.createdAt)
      const key = dayKey(created)

      if (!(key in totals)) continue
      totals[key] += Number(order.totalAmount || 0)
    }

    return {
      labels,
      values: keys.map((key) => totals[key])
    }
  }, [orders])

  /**
   * Whether the revenue trend contains any non-zero value.
   */
  const hasRevenueTrend = useMemo(
    () => revenueTrend.values.some((value) => Number(value || 0) > 0),
    [revenueTrend]
  )

  /**
   * Determine which metric is best to display for category charts.
   */
  const categoryMetric = useMemo(() => resolveCategoryMetric(categoryAnalytics), [categoryAnalytics])

  /**
   * Resolve all user-facing labels for the selected category metric.
   */
  const categoryMetricMeta = useMemo(
    () => CATEGORY_METRIC_META[categoryMetric] || CATEGORY_METRIC_META.revenue,
    [categoryMetric]
  )

  /**
   * Sort categories descending by the active metric.
   */
  const topCategories = useMemo(() => {
    return [...(categoryAnalytics || [])].sort(
      (a, b) => metricValue(b, categoryMetric) - metricValue(a, categoryMetric)
    )
  }, [categoryAnalytics, categoryMetric])

  /**
   * Data for the category bar chart.
   * Limited to the top 8 categories.
   */
  const metricByCategory = useMemo(() => {
    const top = topCategories.slice(0, 8)

    return {
      labels: top.map((item) => item.categoryName || 'Uncategorized'),
      values: top.map((item) => metricValue(item, categoryMetric))
    }
  }, [topCategories, categoryMetric])

  /**
   * Data for the category distribution doughnut chart.
   * Keeps top 5 categories and groups the remainder into "Other".
   */
  const categoryDistribution = useMemo(() => {
    const top = topCategories.slice(0, 5)
    const rest = topCategories.slice(5)
    const otherValue = rest.reduce((total, item) => total + metricValue(item, categoryMetric), 0)

    const labels = top.map((item) => item.categoryName || 'Uncategorized')
    const values = top.map((item) => metricValue(item, categoryMetric))

    if (otherValue > 0) {
      labels.push('Other')
      values.push(otherValue)
    }

    return { labels, values }
  }, [topCategories, categoryMetric])

  /**
   * Chart.js line chart data for the revenue trend.
   */
  const trendChartData = useMemo(() => {
    return {
      labels: revenueTrend.labels,
      datasets: [
        {
          label: 'Revenue',
          data: revenueTrend.values,
          borderColor: '#a35f2d',
          backgroundColor: 'rgba(163, 95, 45, 0.16)',
          pointBackgroundColor: '#a35f2d',
          tension: 0.35,
          fill: true
        }
      ]
    }
  }, [revenueTrend])

  /**
   * Chart.js options for the revenue line chart.
   */
  const trendChartOptions = useMemo(() => {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: (context) => `Revenue: ${formatCurrency(context.parsed.y)}`
          }
        }
      },
      scales: {
        y: {
          ticks: {
            callback: (value) => formatCurrency(value)
          }
        }
      }
    }
  }, [])

  /**
   * Chart.js bar chart data for category metric comparison.
   */
  const barChartData = useMemo(() => {
    const colors = buildPalette(metricByCategory.labels.length)

    return {
      labels: metricByCategory.labels,
      datasets: [
        {
          label: categoryMetricMeta.datasetLabel,
          data: metricByCategory.values,
          backgroundColor: colors.map((color) => `${color}cc`),
          borderColor: colors,
          borderWidth: 1,
          borderRadius: 10
        }
      ]
    }
  }, [metricByCategory, categoryMetricMeta])

  /**
   * Chart.js options for the category bar chart.
   */
  const barChartOptions = useMemo(() => {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (context) =>
              `${barChartData.datasets[0]?.label || 'Value'}: ${formatMetric(context.parsed.y, categoryMetric)}`
          }
        }
      },
      scales: {
        y: {
          ticks: {
            callback: (value) => formatMetric(value, categoryMetric)
          }
        }
      }
    }
  }, [barChartData, categoryMetric])

  /**
   * Chart.js doughnut chart data for category distribution.
   * Shows a placeholder dataset when no data is available.
   */
  const doughnutData = useMemo(() => {
    const total = sum(categoryDistribution.values)

    if (total <= 0) {
      return {
        labels: ['No data yet'],
        datasets: [
          {
            label: 'No data',
            data: [1],
            backgroundColor: ['#e5d9c2'],
            borderColor: ['#ccb792'],
            borderWidth: 1
          }
        ]
      }
    }

    const colors = buildPalette(categoryDistribution.labels.length)

    return {
      labels: categoryDistribution.labels,
      datasets: [
        {
          label:
            categoryMetric === 'revenue'
              ? 'Revenue share'
              : categoryMetric === 'unitsSold'
                ? 'Units share'
                : 'Stock share',
          data: categoryDistribution.values,
          backgroundColor: colors.map((color) => `${color}cc`),
          borderColor: colors,
          borderWidth: 1
        }
      ]
    }
  }, [categoryDistribution, categoryMetric])

  /**
   * Chart.js options for the doughnut chart.
   * Tooltip is disabled when placeholder data is shown.
   */
  const doughnutOptions = useMemo(() => {
    const isPlaceholder = doughnutData.labels.length === 1 && doughnutData.labels[0] === 'No data yet'

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom' },
        tooltip: isPlaceholder
          ? { enabled: false }
          : {
              callbacks: {
                label: (context) => `${context.label}: ${formatMetric(context.parsed, categoryMetric)}`
              }
            }
      },
      cutout: '64%'
    }
  }, [doughnutData, categoryMetric])

  if (loading) {
    return <Spinner label="Loading analytics..." />
  }

  return (
    <div className="page">
      <PageHeader
        title="Analytics"
        description="Trends and distribution snapshots powered by the existing commerce + inventory APIs."
      />

      <CommerceAnalyticsOverviewPanel />

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="panel-grid">
        <Panel
          title="Revenue trend"
          description="Daily order revenue across the last 14 days (based on the latest orders returned by the API)."
          className="chart-card"
        >
          {!hasRevenueTrend ? (
            <p className="empty-copy">No orders recorded in the last 14 days.</p>
          ) : null}

          <div className="chart-wrap">
            <Line data={trendChartData} options={trendChartOptions} />
          </div>
        </Panel>

        <Panel
          title={categoryMetricMeta.title}
          description={categoryMetricMeta.description}
          className="chart-card"
        >
          {!metricByCategory.labels.length ? (
            <p className="empty-copy">No category analytics are available yet.</p>
          ) : null}

          <div className="chart-wrap">
            <Bar data={barChartData} options={barChartOptions} />
          </div>
        </Panel>

        <Panel
          title={categoryMetricMeta.distributionTitle}
          description="Distribution across leading categories."
          className="chart-card"
        >
          {!categoryDistribution.labels.length ? (
            <p className="empty-copy">No category analytics are available yet.</p>
          ) : null}

          <div className="chart-wrap sm">
            <Doughnut data={doughnutData} options={doughnutOptions} />
          </div>
        </Panel>
      </div>
    </div>
  )
}