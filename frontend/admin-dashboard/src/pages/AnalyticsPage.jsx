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

export function AnalyticsPage() {

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [orders, setOrders] = useState([])
  const [categoryAnalytics, setCategoryAnalytics] = useState([])
  const [range, setRange] = useState(14)

  async function load() {

    setLoading(true)
    setError("")

    try {

      const [ordersPage, categories] = await Promise.all([
        listOrders({ page: 0, size: 100, sortBy: "createdAt", direction: "desc" }),
        getCategoryAnalytics()
      ])

      setOrders(ordersPage?.content || [])
      setCategoryAnalytics(Array.isArray(categories) ? categories : [])

    } catch (err) {

      setError(err.message || "Unable to load analytics.")

    } finally {

      setLoading(false)

    }
  }

  useEffect(() => {
    load()
  }, [])

  /* ---------------- KPI METRICS ---------------- */

  const totalRevenue = useMemo(() => {
    return orders.slice(0, range).reduce(
      (sum, o) => sum + Number(o.totalAmount || 0),
      0
    )
  }, [orders, range])

  const totalOrders = orders.slice(0, range).length

  const avgOrderValue = useMemo(() => {
    if (!totalOrders) return 0
    return totalRevenue / totalOrders
  }, [totalOrders, totalRevenue])

  const totalCategories = categoryAnalytics.length

  /* -------- Growth calculation -------- */

  const previousOrders = orders.slice(range, range * 2)

  const previousRevenue = previousOrders.reduce(
    (sum, o) => sum + Number(o.totalAmount || 0),
    0
  )

  const revenueGrowth = previousRevenue
    ? ((totalRevenue - previousRevenue) / previousRevenue) * 100
    : 0

  /* ---------------- Revenue Trend ---------------- */

  const recentOrders = orders.slice(0, range).reverse()

  const trendChartData = {
    labels: recentOrders.map(o =>
      new Date(o.createdAt).toLocaleDateString()
    ),
    datasets: [
      {
        label: "Revenue",
        data: recentOrders.map(o => o.totalAmount),
        borderColor: "#a35f2d",
        backgroundColor: "rgba(163,95,45,0.15)",
        tension: 0.35,
        fill: true
      }
    ]
  }

  const trendChartOptions = {
    responsive: true,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => `Revenue: ${formatCurrency(ctx.parsed.y)}`
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

  /* ---------------- Category Charts ---------------- */

  const categoryLabels = categoryAnalytics.map(c => c.categoryName)
  const categoryValues = categoryAnalytics.map(c => c.revenue)

  const barChartData = {
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

  const doughnutData = {
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

  if (loading) {
    return <Spinner label="Loading analytics..." />
  }

  return (
    <div className="analytics-page">

      <PageHeader
        title="Analytics Dashboard"
        description="Operational insights from commerce and inventory APIs."
      />

      {error && (
        <div className="alert-error">{error}</div>
      )}

      {/* RANGE FILTER */}

      <div className="range-filter">

        <button
          onClick={() => setRange(7)}
          className={range === 7 ? "active" : ""}
        >
          7D
        </button>

        <button
          onClick={() => setRange(14)}
          className={range === 14 ? "active" : ""}
        >
          14D
        </button>

        <button
          onClick={() => setRange(30)}
          className={range === 30 ? "active" : ""}
        >
          30D
        </button>

      </div>

      {/* KPI CARDS */}

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

      {/* CHART GRID */}

      <div className="analytics-grid">

        <Panel
          title="Revenue Trend"
          description="Revenue trend across the selected period."
          className="chart-card large"
        >
          <div className="chart-area">
            <Line data={trendChartData} options={trendChartOptions} />
          </div>
        </Panel>

        <Panel
          title="Revenue by Category"
          description="Category performance."
          className="chart-card"
        >
          <div className="chart-area">
            <Bar data={barChartData} />
          </div>
        </Panel>

        <Panel
          title="Category Distribution"
          description="Revenue share across categories."
          className="chart-card"
        >
          <div className="chart-area doughnut">
            <Doughnut data={doughnutData} />
          </div>
        </Panel>

      </div>
    </div>
  )
}