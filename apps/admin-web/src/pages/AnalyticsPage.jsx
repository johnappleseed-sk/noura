/**
 * AnalyticsPage
 *
 * Displays operational analytics derived from order and category endpoints.
 *
 * This refactor preserves:
 * - existing API contracts
 * - current business logic
 * - current dashboard behavior
 *
 * Improvements focus on:
 * - defensive coding
 * - async safety
 * - readability
 * - maintainability
 * - better empty-state handling
 */

import { useEffect, useMemo, useState } from "react"
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
} from "chart.js"
import { Bar, Doughnut, Line } from "react-chartjs-2"

import { listOrders } from "../shared/api/endpoints/ordersApi"
import { getCategoryAnalytics } from "../shared/api/endpoints/analyticsApi"
import { formatCurrency } from "../shared/ui/formatters"

import { Spinner } from "../shared/ui/Spinner"
import { Panel } from "../shared/ui/Panel"
import { PageHeader } from "../shared/ui/PageHeader"
import { useToastFeedback } from '../shared/ui/useToastFeedback'

import '../styles/main.css'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Tooltip,
  Legend
)

function toSafeNumber(value) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function toSafeDate(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function toDateLabel(value) {
  const date = toSafeDate(value)
  if (!date) return "Unknown"
  return date.toLocaleDateString()
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

export function AnalyticsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  useToastFeedback({ errorMessage: error })
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])
  const [range, setRange] = useState(14)

  useEffect(() => {
    let active = true

    async function loadAnalytics() {
      setLoading(true)
      setError("")

      try {
        const [ordersPage, categories] = await Promise.all([
          listOrders({ page: 0, size: 100, sortBy: "createdAt", direction: "desc" }),
          getCategoryAnalytics()
        ])

        if (!active) return

        const nextOrders = Array.isArray(ordersPage?.content) ? ordersPage.content : []
        const nextCategories = Array.isArray(categories) ? categories : []

        setOrders(nextOrders)
        setCategoryAnalytics(nextCategories)
      } catch (err) {
        if (!active) return
        setError(getErrorMessage(err, "Unable to load analytics."))
        setOrders([])
        setCategoryAnalytics([])
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadAnalytics()

    return () => {
      active = false
    }
  }, [])

  const currentOrders = useMemo(() => orders.slice(0, range), [orders, range])

  const previousOrders = useMemo(() => {
    return orders.slice(range, range * 2)
  }, [orders, range])

  const totalRevenue = useMemo(() => {
    return currentOrders.reduce((sum, order) => sum + toSafeNumber(order?.totalAmount), 0)
  }, [currentOrders])

  const totalOrders = currentOrders.length

  const avgOrderValue = useMemo(() => {
    if (!totalOrders) return 0
    return totalRevenue / totalOrders
  }, [totalOrders, totalRevenue])

  const totalCategories = categoryAnalytics.length

  const previousRevenue = useMemo(() => {
    return previousOrders.reduce((sum, order) => sum + toSafeNumber(order?.totalAmount), 0)
  }, [previousOrders])

  /**
   * Growth compares the selected current window against the immediately
   * preceding window of equal length based on the fetched order list order.
   */
  const revenueGrowth = useMemo(() => {
    if (!previousRevenue) return 0
    return ((totalRevenue - previousRevenue) / previousRevenue) * 100
  }, [previousRevenue, totalRevenue])

  const recentOrders = useMemo(() => {
    return [...currentOrders].reverse()
  }, [currentOrders])

  const trendChartData = useMemo(() => {
    return {
      labels: recentOrders.map((order) => toDateLabel(order?.createdAt)),
      datasets: [
        {
          label: "Revenue",
          data: recentOrders.map((order) => toSafeNumber(order?.totalAmount)),
          borderColor: "#a35f2d",
          backgroundColor: "rgba(163,95,45,0.15)",
          tension: 0.35,
          fill: true
        }
      ]
    }
  }, [recentOrders])

  const trendChartOptions = useMemo(() => {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => `Revenue: ${formatCurrency(toSafeNumber(ctx?.parsed?.y))}`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false }
        },
        y: {
          grid: { color: "#f1f5f9" }
        }
      }
    }
  }, [])

  const categoryLabels = useMemo(() => {
    return categoryAnalytics.map((category) => category?.categoryName || "Unknown")
  }, [categoryAnalytics])

  const categoryValues = useMemo(() => {
    return categoryAnalytics.map((category) => toSafeNumber(category?.revenue))
  }, [categoryAnalytics])

  const barChartData = useMemo(() => {
    return {
      labels: categoryLabels,
      datasets: [
        {
          label: "Revenue",
          data: categoryValues,
          backgroundColor: "#a35f2d",
          borderRadius: 8
        }
      ]
    }
  }, [categoryLabels, categoryValues])

  const doughnutData = useMemo(() => {
    return {
      labels: categoryLabels,
      datasets: [
        {
          data: categoryValues,
          backgroundColor: [
            "#a35f2d",
            "#0f5d8c",
            "#2f7d4a",
            "#a96c05",
            "#b42318",
            "#1f335e"
          ]
        }
      ]
    }
  }, [categoryLabels, categoryValues])

  if (loading) {
    return <Spinner label="Loading analytics..." />
  }

  return (
    <div className="analytics-page">
      <PageHeader
        title="Analytics Dashboard"
        description="Operational insights from commerce and inventory APIs."
      />

      <div className="range-filter" role="group" aria-label="Analytics range filter">
        <button
          type="button"
          onClick={() => setRange(7)}
          className={range === 7 ? "active" : ""}
        >
          7D
        </button>

        <button
          type="button"
          onClick={() => setRange(14)}
          className={range === 14 ? "active" : ""}
        >
          14D
        </button>

        <button
          type="button"
          onClick={() => setRange(30)}
          className={range === 30 ? "active" : ""}
        >
          30D
        </button>
      </div>

      <div className="kpi-grid">
        <div className="kpi-card">
          <span className="kpi-label">Revenue</span>
          <h3>{formatCurrency(totalRevenue)}</h3>
          <span className={`kpi-change ${revenueGrowth >= 0 ? "up" : "down"}`}>
            {revenueGrowth >= 0 ? "▲" : "▼"} {Math.abs(revenueGrowth).toFixed(1)}%
          </span>
        </div>

        <div className="kpi-card">
          <span className="kpi-label">Orders</span>
          <h3>{totalOrders}</h3>
        </div>

        <div className="kpi-card">
          <span className="kpi-label">Avg Order</span>
          <h3>{formatCurrency(avgOrderValue)}</h3>
        </div>

        <div className="kpi-card">
          <span className="kpi-label">Categories</span>
          <h3>{totalCategories}</h3>
        </div>
      </div>

      <div className="analytics-grid">
        <Panel
          title="Revenue Trend"
          description="Revenue trend across the selected period."
          className="chart-card large"
        >
          <div className="chart-area">
            {recentOrders.length ? (
              <Line data={trendChartData} options={trendChartOptions} />
            ) : (
              <p className="empty-copy">No order trend data available.</p>
            )}
          </div>
        </Panel>

        <Panel
          title="Revenue by Category"
          description="Category performance."
          className="chart-card"
        >
          <div className="chart-area">
            {categoryLabels.length ? (
              <Bar data={barChartData} />
            ) : (
              <p className="empty-copy">No category revenue data available.</p>
            )}
          </div>
        </Panel>

        <Panel
          title="Category Distribution"
          description="Revenue share across categories."
          className="chart-card"
        >
          <div className="chart-area doughnut">
            {categoryLabels.length ? (
              <Doughnut data={doughnutData} />
            ) : (
              <p className="empty-copy">No category distribution data available.</p>
            )}
          </div>
        </Panel>
      </div>
    </div>
  )
}
