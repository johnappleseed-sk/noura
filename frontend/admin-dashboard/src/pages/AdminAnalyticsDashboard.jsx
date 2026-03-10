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

function toIso(value) {
  if (!value) return undefined
  return new Date(value).toISOString()
}

function groupByDay(items, getDate, getValue) {
  const map = new Map()
  for (const item of items) {
    const raw = getDate(item)
    if (!raw) continue
    const day = new Date(raw).toLocaleDateString('en-US')
    map.set(day, (map.get(day) || 0) + (getValue(item) || 0))
  }
  const labels = Array.from(map.keys())
  const values = labels.map((label) => map.get(label) || 0)
  return { labels, values }
}

function safeObject(value) {
  if (!value || typeof value !== 'object') return {}
  return value
}

export function AdminAnalyticsDashboard() {
  const [rangeDays, setRangeDays] = useState(14)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [summary, setSummary] = useState(null)
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])
  const [customerAnalytics, setCustomerAnalytics] = useState(null)

  useEffect(() => {
    async function loadBase() {
      setError('')
      try {
        const [nextSummary, nextCustomer] = await Promise.all([
          getDashboardSummary(),
          getCustomerAnalytics()
        ])
        setSummary(nextSummary || null)
        setCustomerAnalytics(nextCustomer || null)
      } catch (err) {
        setError(err.message || 'Failed to load dashboard summary.')
      }
    }
    loadBase()
  }, [])

  useEffect(() => {
    async function loadRange() {
      setLoading(true)
      setError('')

      const now = new Date()
      const from = new Date(now.getTime() - rangeDays * 24 * 60 * 60 * 1000)

      try {
        const [ordersPage, categories] = await Promise.all([
          listOrders({ page: 0, size: 250, sortBy: 'createdAt', direction: 'desc' }),
          getCategoryAnalytics({ from: toIso(from), to: toIso(now) })
        ])

        const rawOrders = Array.isArray(ordersPage?.content) ? ordersPage.content : []
        const filteredOrders = rawOrders.filter((order) => {
          if (!order?.createdAt) return false
          const createdAt = new Date(order.createdAt)
          return !Number.isNaN(createdAt.getTime()) && createdAt >= from && createdAt <= now
        })
        setOrders(filteredOrders)
        setCategoryAnalytics(Array.isArray(categories) ? categories : [])
      } catch (err) {
        setError(err.message || 'Failed to load analytics.')
        setOrders([])
        setCategoryAnalytics([])
      } finally {
        setLoading(false)
      }
    }

    loadRange()
  }, [rangeDays])

  const revenueTrend = useMemo(() => {
    const { labels, values } = groupByDay(
      orders.slice().reverse(),
      (o) => o.createdAt,
      (o) => Number(o.totalAmount || 0)
    )

    return {
      labels,
      datasets: [
        {
          label: 'Revenue',
          data: values,
          borderColor: '#a35f2d',
          backgroundColor: 'rgba(163,95,45,0.15)',
          tension: 0.35,
          fill: true
        }
      ]
    }
  }, [orders])

  const ordersTrend = useMemo(() => {
    const { labels, values } = groupByDay(
      orders.slice().reverse(),
      (o) => o.createdAt,
      () => 1
    )

    return {
      labels,
      datasets: [
        {
          label: 'Orders',
          data: values,
          backgroundColor: '#0f5d8c',
          borderRadius: 8
        }
      ]
    }
  }, [orders])

  const categoryChart = useMemo(() => {
    const labels = categoryAnalytics.map((c) => c.categoryName || c.name || 'Unknown')
    const values = categoryAnalytics.map((c) => Number(c.revenue || 0))

    return {
      labels,
      datasets: [
        {
          label: 'Revenue',
          data: values,
          backgroundColor: '#a35f2d',
          borderRadius: 8
        }
      ]
    }
  }, [categoryAnalytics])

  const segmentCounts = safeObject(customerAnalytics?.segmentCounts)
  const countryCounts = safeObject(customerAnalytics?.countryCounts)

  const segmentsChart = useMemo(() => {
    const labels = Object.keys(segmentCounts)
    const values = labels.map((key) => Number(segmentCounts[key] || 0))

    return {
      labels,
      datasets: [
        {
          label: 'Segments',
          data: values,
          backgroundColor: ['#a35f2d', '#0f5d8c', '#2f7d4a', '#a96c05', '#b42318', '#1f335e']
        }
      ]
    }
  }, [segmentCounts])

  const countriesChart = useMemo(() => {
    const labels = Object.keys(countryCounts)
    const values = labels.map((key) => Number(countryCounts[key] || 0))

    return {
      labels,
      datasets: [
        {
          label: 'Countries',
          data: values,
          backgroundColor: ['#2f7d4a', '#0f5d8c', '#a96c05', '#b42318', '#1f335e', '#a35f2d']
        }
      ]
    }
  }, [countryCounts])

  if (loading && !summary) {
    return <Spinner label="Loading analytics dashboard..." />
  }

  return (
    <div className="analytics-page">
      <PageHeader
        title="Admin Analytics Dashboard"
        description="Revenue, orders, categories, and customer insights from commerce APIs."
      >
        <div className="range-filter" style={{ marginTop: 0 }}>
          <button onClick={() => setRangeDays(7)} className={rangeDays === 7 ? 'active' : ''}>7D</button>
          <button onClick={() => setRangeDays(14)} className={rangeDays === 14 ? 'active' : ''}>14D</button>
          <button onClick={() => setRangeDays(30)} className={rangeDays === 30 ? 'active' : ''}>30D</button>
        </div>
      </PageHeader>

      {error ? <div className="alert alert-error">{error}</div> : null}

      {summary ? (
        <div className="kpi-grid">
          <div className="kpi-card">
            <span className="kpi-label">Revenue</span>
            <h3>{formatCurrency(summary.revenue)}</h3>
          </div>
          <div className="kpi-card">
            <span className="kpi-label">Orders</span>
            <h3>{Number(summary.ordersCount || 0)}</h3>
          </div>
          <div className="kpi-card">
            <span className="kpi-label">Users</span>
            <h3>{Number(summary.usersCount || 0)}</h3>
          </div>
          <div className="kpi-card">
            <span className="kpi-label">Stores</span>
            <h3>{Number(summary.storesCount || 0)}</h3>
          </div>
        </div>
      ) : null}

      {loading ? (
        <Spinner label="Refreshing analytics..." />
      ) : (
        <div className="analytics-grid">
          <Panel title="Revenue Trend" description={`Daily revenue (last ${rangeDays} days).`} className="chart-card large">
            <div className="chart-area">
              {revenueTrend.labels.length ? <Line data={revenueTrend} /> : <p className="empty-copy">No orders in range.</p>}
            </div>
          </Panel>

          <Panel title="Orders Trend" description={`Orders per day (last ${rangeDays} days).`} className="chart-card">
            <div className="chart-area">
              {ordersTrend.labels.length ? <Bar data={ordersTrend} /> : <p className="empty-copy">No orders in range.</p>}
            </div>
          </Panel>

          <Panel title="Revenue by Category" description="Category performance for the selected period." className="chart-card">
            <div className="chart-area">
              {categoryChart.labels.length ? <Bar data={categoryChart} /> : <p className="empty-copy">No category analytics.</p>}
            </div>
          </Panel>

          <Panel title="Customer Segments" description="Customer segmentation distribution." className="chart-card">
            <div className="chart-area">
              {segmentsChart.labels.length ? <Pie data={segmentsChart} /> : <p className="empty-copy">No customer segment data.</p>}
            </div>
          </Panel>

          <Panel title="Customer Countries" description="Customer distribution by country." className="chart-card">
            <div className="chart-area">
              {countriesChart.labels.length ? <Pie data={countriesChart} /> : <p className="empty-copy">No customer geography data.</p>}
            </div>
          </Panel>

          <Panel title="Top Products" description="High-level snapshot from the admin summary." className="chart-card">
            {summary?.topProducts?.length ? (
              <ul style={{ margin: 0, paddingLeft: '1.1rem' }}>
                {summary.topProducts.map((name, idx) => (
                  <li key={`${name}-${idx}`}>{name}</li>
                ))}
              </ul>
            ) : (
              <p className="empty-copy">No top products available.</p>
            )}
          </Panel>

          <Panel title="Store Performance" description="High-level snapshot from the admin summary." className="chart-card">
            {summary?.storePerformance?.length ? (
              <ul style={{ margin: 0, paddingLeft: '1.1rem' }}>
                {summary.storePerformance.map((row, idx) => (
                  <li key={`${row}-${idx}`}>{row}</li>
                ))}
              </ul>
            ) : (
              <p className="empty-copy">No store performance data available.</p>
            )}
          </Panel>
        </div>
      )}
    </div>
  )
}

export default AdminAnalyticsDashboard
