import { useMemo } from 'react'
import { Bar, Line, Pie } from 'react-chartjs-2'
import 'chart.js/auto'

import { PageHeader } from '../../../shared/ui/PageHeader'
import { Panel } from '../../../shared/ui/Panel'
import { Spinner } from '../../../shared/ui/Spinner'
import { formatCurrency } from '../../../shared/ui/formatters'
import { useToastFeedback } from '../../../shared/ui/useToastFeedback'
import { CATEGORY_PALETTE, CHART_COLORS } from '../charts/chartColors'
import { barChartOptions, pieChartOptions, revenueLineOptions } from '../charts/chartConfig'
import { formatDateLabel, groupByDay, safeNumber } from '../charts/chartUtils'
import { useAnalyticsData } from '../hooks/useAnalyticsData'
import '../../../styles/pages/AdminAnalyticsDashboard.css'

/**
 * Analytics dashboard page for the admin portal.
 *
 * @returns {JSX.Element} Rendered analytics page.
 */
export function AnalyticsPage() {
  const {
    rangeDays,
    setRangeDays,
    loading,
    error,
    summary,
    orders,
    categoryAnalytics,
    segmentCounts,
    countryCounts
  } = useAnalyticsData(14)
  useToastFeedback({ errorMessage: error })

  const revenueTrend = useMemo(() => {
    const { labels, values } = groupByDay(
      orders,
      (order) => order?.createdAt,
      (order) => safeNumber(order?.totalAmount)
    )

    return {
      labels: labels.map(formatDateLabel),
      datasets: [
        {
          label: 'Revenue',
          data: values,
          borderColor: CHART_COLORS.primary,
          backgroundColor: 'rgba(163,95,45,0.16)',
          tension: 0.35,
          fill: true
        }
      ]
    }
  }, [orders])

  const ordersTrend = useMemo(() => {
    const { labels, values } = groupByDay(
      orders,
      (order) => order?.createdAt,
      () => 1
    )

    return {
      labels: labels.map(formatDateLabel),
      datasets: [
        {
          label: 'Orders',
          data: values,
          backgroundColor: CHART_COLORS.secondary,
          borderRadius: 8
        }
      ]
    }
  }, [orders])

  const categoryChart = useMemo(() => {
    const labels = categoryAnalytics.map((category) => category?.categoryName || category?.name || 'Unknown')
    const values = categoryAnalytics.map((category) => safeNumber(category?.revenue))

    return {
      labels,
      datasets: [
        {
          label: 'Revenue',
          data: values,
          backgroundColor: CHART_COLORS.primary,
          borderRadius: 8
        }
      ]
    }
  }, [categoryAnalytics])

  const segmentChart = useMemo(() => {
    const labels = Object.keys(segmentCounts)
    const values = labels.map((label) => safeNumber(segmentCounts[label]))
    const palette = labels.map((_, index) => CATEGORY_PALETTE[index % CATEGORY_PALETTE.length])

    return {
      labels,
      datasets: [
        {
          label: 'Segments',
          data: values,
          backgroundColor: palette
        }
      ]
    }
  }, [segmentCounts])

  const countryChart = useMemo(() => {
    const labels = Object.keys(countryCounts)
    const values = labels.map((label) => safeNumber(countryCounts[label]))
    const palette = labels.map((_, index) => CATEGORY_PALETTE[(index + 2) % CATEGORY_PALETTE.length])

    return {
      labels,
      datasets: [
        {
          label: 'Countries',
          data: values,
          backgroundColor: palette
        }
      ]
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
          {[7, 14, 30].map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setRangeDays(value)}
              className={rangeDays === value ? 'active' : ''}
            >
              {value}D
            </button>
          ))}
        </div>
      </PageHeader>

      {summary ? (
        <div className="admin-analytics-kpi-grid">
          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Revenue</span>
            <h3>{formatCurrency(summary.revenue)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Orders</span>
            <h3>{safeNumber(summary.ordersCount)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Users</span>
            <h3>{safeNumber(summary.usersCount)}</h3>
          </div>

          <div className="admin-analytics-kpi-card">
            <span className="admin-analytics-kpi-label">Stores</span>
            <h3>{safeNumber(summary.storesCount)}</h3>
          </div>
        </div>
      ) : null}

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
              {revenueTrend.labels.length ? (
                <Line data={revenueTrend} options={revenueLineOptions} />
              ) : (
                <p className="admin-analytics-empty">No orders in range.</p>
              )}
            </div>
          </Panel>

          <Panel
            title="Orders Trend"
            description={`Orders per day (last ${rangeDays} days).`}
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {ordersTrend.labels.length ? (
                <Bar data={ordersTrend} options={barChartOptions} />
              ) : (
                <p className="admin-analytics-empty">No orders in range.</p>
              )}
            </div>
          </Panel>

          <Panel
            title="Revenue by Category"
            description="Category performance for the selected period."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {categoryChart.labels.length ? (
                <Bar data={categoryChart} options={barChartOptions} />
              ) : (
                <p className="admin-analytics-empty">No category analytics.</p>
              )}
            </div>
          </Panel>

          <Panel
            title="Customer Segments"
            description="Customer segmentation distribution."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {segmentChart.labels.length ? (
                <Pie data={segmentChart} options={pieChartOptions} />
              ) : (
                <p className="admin-analytics-empty">No customer segment data.</p>
              )}
            </div>
          </Panel>

          <Panel
            title="Customer Countries"
            description="Customer distribution by country."
            className="admin-analytics-card"
          >
            <div className="admin-analytics-chart-area">
              {countryChart.labels.length ? (
                <Pie data={countryChart} options={pieChartOptions} />
              ) : (
                <p className="admin-analytics-empty">No customer geography data.</p>
              )}
            </div>
          </Panel>

          <Panel
            title="Top Products"
            description="High-level snapshot from the admin summary."
            className="admin-analytics-card"
          >
            {summary?.topProducts?.length ? (
              <ul className="admin-analytics-list">
                {summary.topProducts.map((name, index) => (
                  <li key={`${name}-${index}`}>{name}</li>
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
                {summary.storePerformance.map((row, index) => (
                  <li key={`${row}-${index}`}>{row}</li>
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

export default AnalyticsPage
