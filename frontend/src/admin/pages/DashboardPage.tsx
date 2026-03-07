import { DollarCircleOutlined, RiseOutlined, ShopOutlined, ShoppingCartOutlined, TeamOutlined } from '@ant-design/icons';
import { App as AntApp, Card, Col, Empty, Row, Spin, Statistic, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { DashboardSummary } from '@/types/models';

/**
 * Executes parse top product.
 *
 * @param raw The raw value.
 * @returns The result of parse top product.
 */
const parseTopProduct = (raw: string): { name: string; score: number } => {
  const match = raw.match(/^(.*)\s+\((\d+)\)$/);
  if (!match) {
    return { name: raw, score: 0 };
  }
  return { name: match[1], score: Number(match[2]) };
};

/**
 * Renders the DashboardPage component.
 *
 * @returns The rendered component tree.
 */
export const DashboardPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    /**
     * Executes load.
     *
     * @returns No value.
     */
    const load = async (): Promise<void> => {
      setLoading(true);
      try {
        const result = await adminApi.getDashboardSummary();
        setSummary(result);
      } catch (error) {
        message.error(error instanceof Error ? error.message : 'Failed to load dashboard');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [message]);

  const chartData = useMemo(() => (summary?.topProducts ?? []).map(parseTopProduct), [summary]);

  if (loading && !summary) {
    return (
      <Card className="admin-section-card">
        <Spin />
      </Card>
    );
  }

  return (
    <>
      <PageHeader
        title="Metrics Dashboard"
        subtitle="Monitor core KPIs, top-performing products, and store-level performance signals."
        extra={summary ? <Tag color="blue">Live snapshot</Tag> : null}
      />

      <Row gutter={[18, 18]}>
        <Col xs={24} md={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic
              title={
                <span>
                  <DollarCircleOutlined style={{ marginRight: 6 }} />
                  Revenue
                </span>
              }
              precision={2}
              prefix="$"
              value={Number(summary?.revenue ?? 0)}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Orders" prefix={<ShoppingCartOutlined />} value={summary?.ordersCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Users" prefix={<TeamOutlined />} value={summary?.usersCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Stores" prefix={<ShopOutlined />} value={summary?.storesCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[18, 18]}>
        <Col xs={24} xl={14}>
          <Card className="admin-section-card" title="Top Products by Demand">
            <div className="admin-chart-wrap">
              <ResponsiveContainer>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" hide />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="score" fill="#1677ff" radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col xs={24} xl={10}>
          <Card className="admin-section-card" title="Store Performance Signals">
            {(summary?.storePerformance ?? []).length > 0 ? (
              (summary?.storePerformance ?? []).map((item) => (
                <Typography.Paragraph key={item} style={{ marginBottom: 8 }}>
                  <RiseOutlined style={{ marginRight: 8, color: '#1565c0' }} />
                  {item}
                </Typography.Paragraph>
              ))
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No store performance data yet" />
            )}
          </Card>
        </Col>
      </Row>
    </>
  );
};
