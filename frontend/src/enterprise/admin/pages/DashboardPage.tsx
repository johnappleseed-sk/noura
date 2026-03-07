import { Card, Col, Row, Spin, Statistic, Typography } from 'antd'
import { Column, Line } from '@ant-design/plots'
import { useEffect, useMemo } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { fetchDashboardSummary } from '@/admin/features/dashboard/dashboardSlice'

/**
 * Executes currency.
 *
 * @param value The value value.
 * @returns The result of currency.
 */
const currency = (value: number): string =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value)

const parseTopProductScore = (label: string): { product: string; score: number } => {
  const match = /(.*)\s+\((\d+)\)$/.exec(label)
  if (!match) {
    return { product: label, score: 0 }
  }
  return { product: match[1], score: Number(match[2]) }
}

/**
 * Renders the DashboardPage component.
 *
 * @returns The rendered component tree.
 */
export const DashboardPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { summary, loading } = useAdminSelector((state) => state.dashboard)

  useEffect(() => {
    void dispatch(fetchDashboardSummary())
  }, [dispatch])

  const topProductsData = useMemo(
    () => (summary?.topProducts ?? []).map((item) => parseTopProductScore(item)),
    [summary?.topProducts],
  )

  const revenueTrendData = useMemo(() => {
    const revenue = summary?.revenue ?? 0
    const base = revenue / 6
    return ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'].map((month, index) => ({
      month,
      revenue: Math.max(0, Math.round(base * (0.8 + index * 0.1))),
    }))
  }, [summary?.revenue])

  if (loading && !summary) {
    return <Spin />
  }

  return (
    <>
      <Typography.Title level={3}>Metrics Dashboard</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col lg={6} md={12} xs={24}>
          <Card>
            <Statistic title="Revenue" value={summary?.revenue ?? 0} formatter={(value) => currency(Number(value))} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card>
            <Statistic title="Orders" value={summary?.ordersCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card>
            <Statistic title="Users" value={summary?.usersCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card>
            <Statistic title="Stores" value={summary?.storesCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col lg={14} xs={24}>
          <Card title="Revenue Trend">
            <Line data={revenueTrendData} xField="month" yField="revenue" smooth />
          </Card>
        </Col>
        <Col lg={10} xs={24}>
          <Card title="Top Product Scores">
            <Column data={topProductsData} xField="product" yField="score" label={{ text: 'score', position: 'top' }} />
          </Card>
        </Col>
      </Row>
    </>
  )
}
