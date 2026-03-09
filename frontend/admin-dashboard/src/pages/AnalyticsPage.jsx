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

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend)

function sum(values = []) {
  return values.reduce((total, value) => total + Number(value || 0), 0)
}

function dayKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

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
  const colors = []
  for (let i = 0; i < count; i += 1) {
    colors.push(base[i % base.length])
  }
  return colors
}

function metricValue(item, metric) {
  if (!item) return 0
  if (metric === 'revenue') return Number(item.revenue || 0)
  if (metric === 'unitsSold') return Number(item.unitsSold || 0)
  if (metric === 'currentStock') return Number(item.currentStock || 0)
  return 0
}

function resolveCategoryMetric(items = []) {
  const revenueTotal = sum(items.map((item) => metricValue(item, 'revenue')))
  if (revenueTotal > 0) return 'revenue'

  const unitsTotal = sum(items.map((item) => metricValue(item, 'unitsSold')))
  if (unitsTotal > 0) return 'unitsSold'

  const stockTotal = sum(items.map((item) => metricValue(item, 'currentStock')))
  if (stockTotal > 0) return 'currentStock'

  return 'revenue'
}

function formatMetric(value, metric) {
  if (metric === 'revenue') return formatCurrency(value)
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(Number(value || 0))
}

export function AnalyticsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [ordersPage, categories] = await Promise.all([
        listOrders({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }),
        getCategoryAnalytics()
      ])
      setOrders(ordersPage?.content || [])
      setCategoryAnalytics(Array.isArray(categories) ? categories : [])
    } catch (err) {
      setError(err.message || 'Unable to load analytics.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const revenueTrend = useMemo(() => {
    const days = lastNDays(14)
    const keys = days.map(dayKey)
    const labels = days.map((date) =>
      new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(date)
    )

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

  const hasRevenueTrend = useMemo(() => revenueTrend.values.some((value) => Number(value || 0) > 0), [revenueTrend])

  const categoryMetric = useMemo(
    () => resolveCategoryMetric(categoryAnalytics),
    [categoryAnalytics]
  )

  const topCategories = useMemo(() => {
    const items = [...(categoryAnalytics || [])]
    items.sort((a, b) => metricValue(b, categoryMetric) - metricValue(a, categoryMetric))
    return items
  }, [categoryAnalytics, categoryMetric])

  const metricByCategory = useMemo(() => {
    const top = topCategories.slice(0, 8)
    return {
      labels: top.map((item) => item.categoryName || 'Uncategorized'),
      values: top.map((item) => metricValue(item, categoryMetric))
    }
  }, [topCategories, categoryMetric])

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

  const barChartData = useMemo(() => {
    const colors = buildPalette(metricByCategory.labels.length)
    return {
      labels: metricByCategory.labels,
      datasets: [
        {
          label: categoryMetric === 'revenue' ? 'Revenue' : categoryMetric === 'unitsSold' ? 'Units sold' : 'Current stock',
          data: metricByCategory.values,
          backgroundColor: colors.map((color) => `${color}cc`),
          borderColor: colors,
          borderWidth: 1,
          borderRadius: 10
        }
      ]
    }
  }, [metricByCategory, categoryMetric])

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
          label: categoryMetric === 'revenue' ? 'Revenue share' : categoryMetric === 'unitsSold' ? 'Units share' : 'Stock share',
          data: categoryDistribution.values,
          backgroundColor: colors.map((color) => `${color}cc`),
          borderColor: colors,
          borderWidth: 1
        }
      ]
    }
  }, [categoryDistribution, categoryMetric])

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

  const categoryMetricTitle =
    categoryMetric === 'revenue'
      ? 'Revenue by category'
      : categoryMetric === 'unitsSold'
        ? 'Units sold by category'
        : 'Current stock by category'

  const categoryMetricDescription =
    categoryMetric === 'revenue'
      ? 'Top categories ranked by revenue in the default analytics window.'
      : categoryMetric === 'unitsSold'
        ? 'Top categories ranked by units sold in the default analytics window.'
        : 'Top categories ranked by current on-hand stock (inventory snapshot).'

  const distributionTitle =
    categoryMetric === 'revenue'
      ? 'Category distribution'
      : categoryMetric === 'unitsSold'
        ? 'Units distribution'
        : 'Stock distribution'

  return (
    <div className="page">
      <PageHeader
        title="Analytics"
        description="Trends and distribution snapshots powered by the existing commerce + inventory APIs."
      />

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
          title={categoryMetricTitle}
          description={categoryMetricDescription}
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
          title={distributionTitle}
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
