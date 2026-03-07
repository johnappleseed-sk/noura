import {
  Alert,
  App as AntApp,
  Button,
  Card,
  Col,
  Empty,
  Row,
  Segmented,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Funnel,
  FunnelChart,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { PageResponse } from '@/types/api';
import type { Approval, DashboardSummary, Order, Product, Store, UserProfile } from '@/types/models';

type Timeframe = '30d' | '90d' | '365d' | 'all';
type AnalyticsTab = 'revenue' | 'catalog' | 'ops' | 'reports';

interface OrdersFetchResult {
  orders: Order[];
  loadedPages: number;
  totalPages: number;
}

interface DailyPoint {
  date: string;
  revenue: number;
  orders: number;
  discount: number;
  shipping: number;
  net: number;
  averageOrderValue: number;
  cumulativeRevenue: number;
}

interface ProductPerformanceRow {
  key: string;
  rank: number;
  product: string;
  units: number;
  revenue: number;
  averageUnitPrice: number;
}

interface StorePerformanceRow {
  key: string;
  store: string;
  orders: number;
  revenue: number;
  shipping: number;
  discount: number;
  averageOrderValue: number;
  revenueShare: number;
}

interface CategoryRow {
  key: string;
  category: string;
  products: number;
  averagePrice: number;
  averageRating: number;
  averagePopularity: number;
}

const ORDER_PAGE_SIZE = 100;
const ORDER_PAGE_LIMIT = 20;
const PIE_COLORS = ['#1565c0', '#00a8a8', '#0f9d58', '#c27803', '#8e24aa', '#e53935', '#455a64'];
const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const timeframeDays: Record<Exclude<Timeframe, 'all'>, number> = {
  '30d': 30,
  '90d': 90,
  '365d': 365,
};

/**
 * Fetches orders across pages up to a fixed limit for dashboard responsiveness.
 *
 * @returns The fetched order list and pagination coverage details.
 */
const fetchAllOrders = async (): Promise<OrdersFetchResult> => {
  const firstPage = await adminApi.getOrders({
    page: 0,
    size: ORDER_PAGE_SIZE,
    sortBy: 'createdAt',
    direction: 'desc',
  });
  const targetPages = Math.min(firstPage.totalPages, ORDER_PAGE_LIMIT);
  if (targetPages <= 1) {
    return { orders: firstPage.content, loadedPages: 1, totalPages: firstPage.totalPages };
  }
  const requests: Array<Promise<PageResponse<Order>>> = [];
  for (let index = 1; index < targetPages; index += 1) {
    requests.push(
      adminApi.getOrders({
        page: index,
        size: ORDER_PAGE_SIZE,
        sortBy: 'createdAt',
        direction: 'desc',
      }),
    );
  }
  const pages = await Promise.all(requests);
  return {
    orders: [firstPage, ...pages].flatMap((page) => page.content),
    loadedPages: targetPages,
    totalPages: firstPage.totalPages,
  };
};

/**
 * Converts value to finite number.
 *
 * @param value The raw value.
 * @returns A finite number.
 */
const toNumber = (value: unknown): number => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

/**
 * Formats number as USD currency.
 *
 * @param value The raw value.
 * @returns A formatted currency string.
 */
const currency = (value: number): string =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value);

/**
 * Escapes values for CSV output.
 *
 * @param value The raw value.
 * @returns CSV-safe field string.
 */
const csvEscape = (value: string | number): string => `"${String(value).replaceAll('"', '""')}"`;

/**
 * Downloads CSV file from tabular data.
 *
 * @param filename The output filename.
 * @param headers Column headers.
 * @param rows Table rows.
 */
const downloadCsv = (filename: string, headers: string[], rows: Array<Array<string | number>>): void => {
  const csv = [headers.map(csvEscape).join(','), ...rows.map((row) => row.map(csvEscape).join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
};

/**
 * Renders the AnalyticsPage component.
 *
 * @returns The rendered component tree.
 */
export const AnalyticsPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [loadedPages, setLoadedPages] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [timeframe, setTimeframe] = useState<Timeframe>('90d');
  const [storeFilter, setStoreFilter] = useState('all');
  const [activeTab, setActiveTab] = useState<AnalyticsTab>('revenue');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const [dashboardSummary, fetchedOrders, productsPage, storesPage, usersPage, approvalsData] = await Promise.all([
        adminApi.getDashboardSummary(),
        fetchAllOrders(),
        adminApi.getProducts({ page: 0, size: 250, sortBy: 'createdAt', direction: 'desc' }),
        adminApi.getStores({ page: 0, size: 250, sortBy: 'name', direction: 'asc' }),
        adminApi.getUsers({ page: 0, size: 250, sortBy: 'createdAt', direction: 'desc' }),
        adminApi.getApprovals(),
      ]);
      setSummary(dashboardSummary);
      setOrders(fetchedOrders.orders);
      setLoadedPages(fetchedOrders.loadedPages);
      setTotalPages(fetchedOrders.totalPages);
      setProducts(productsPage.content);
      setStores(storesPage.content);
      setUsers(usersPage.content);
      setApprovals(approvalsData);
    } catch (loadError) {
      const detail = loadError instanceof Error ? loadError.message : 'Failed to load analytics data';
      setError(detail);
      message.error(detail);
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const hasData = orders.length > 0 || products.length > 0 || approvals.length > 0;

  return (
    <>
      <PageHeader
        title="Advanced Analytics & Reporting"
        subtitle="Explore revenue, order behavior, catalog quality, and operational performance across the business."
        extra={
          <Space>
            <Button onClick={() => void loadData()} loading={loading}>
              Refresh
            </Button>
            {summary ? <Tag color="blue">Live summary synced</Tag> : null}
          </Space>
        }
      />

      {error ? <Alert type="error" showIcon message={error} /> : null}

      {loading && !hasData ? (
        <Card className="admin-section-card">
          <div className="admin-analytics-loading">
            <Spin size="large" />
            <Typography.Text type="secondary">Building analytics workspace...</Typography.Text>
          </div>
        </Card>
      ) : null}

      {!loading && !hasData ? (
        <Card className="admin-section-card">
          <Empty description="No analytics data found." />
        </Card>
      ) : null}

      {hasData ? (
        <Card className="admin-section-card">
          <Space size={12} wrap>
            <Segmented<Timeframe>
              value={timeframe}
              onChange={(value) => setTimeframe(value)}
              options={[
                { label: '30 days', value: '30d' },
                { label: '90 days', value: '90d' },
                { label: '365 days', value: '365d' },
                { label: 'All', value: 'all' },
              ]}
            />
            <Select
              value={storeFilter}
              onChange={setStoreFilter}
              options={[
                { label: 'All Stores', value: 'all' },
                ...stores.map((store) => ({ label: store.name, value: store.id })),
                { label: 'Unassigned Store', value: 'unassigned' },
              ]}
              style={{ minWidth: 240 }}
              showSearch
              optionFilterProp="label"
            />
            <Tag color="cyan">{orders.length} orders</Tag>
            <Tag color="green">{products.length} products</Tag>
            <Tag color="purple">{users.length} users</Tag>
            <Tag color="gold">{stores.length} stores</Tag>
            <Tag color="orange">{approvals.length} approvals</Tag>
          </Space>
          {totalPages > loadedPages ? (
            <Alert
              showIcon
              style={{ marginTop: 14 }}
              type="warning"
              message={`Orders include first ${loadedPages} pages out of ${totalPages} for responsive rendering.`}
            />
          ) : null}
        </Card>
      ) : null}

      {hasData ? <AnalyticsBody
        activeTab={activeTab}
        approvals={approvals}
        orders={orders}
        products={products}
        setActiveTab={setActiveTab}
        storeFilter={storeFilter}
        stores={stores}
        timeframe={timeframe}
        users={users}
      /> : null}
    </>
  );
};

interface RevenueTabProps {
  dailySeries: DailyPoint[];
  fulfillmentSeries: Array<{ name: string; value: number }>;
  hourlySeries: Array<{ hour: string; orders: number }>;
  statusSeries: Array<{ name: string; value: number }>;
  weekdaySeries: Array<{ label: string; day: number; revenue: number; orders: number }>;
}

/**
 * Renders revenue and order analytics charts.
 *
 * @param props The component props.
 * @returns The rendered component tree.
 */
const RevenueTab = ({
  dailySeries,
  fulfillmentSeries,
  hourlySeries,
  statusSeries,
  weekdaySeries,
}: RevenueTabProps): JSX.Element => (
  <Row gutter={[16, 16]}>
    <Col xs={24} xl={14}>
      <Card className="admin-section-card" title="Daily Revenue / Orders">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <ComposedChart data={dailySeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" minTickGap={26} />
              <YAxis yAxisId="left" />
              <YAxis yAxisId="right" orientation="right" />
              <Tooltip />
              <Legend />
              <Area yAxisId="left" dataKey="revenue" fill="#cfe3ff" stroke="#1565c0" />
              <Bar yAxisId="right" dataKey="orders" fill="#00a8a8" />
              <Line yAxisId="left" dataKey="net" stroke="#0f9d58" dot={false} strokeWidth={2} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={10}>
      <Card className="admin-section-card" title="Order Status Mix">
        <div className="admin-chart-wrap">
          {statusSeries.length > 0 ? (
            <ResponsiveContainer>
              <PieChart>
                <Pie data={statusSeries} dataKey="value" nameKey="name" outerRadius={108} label>
                  {statusSeries.map((entry, index) => (
                    <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No order status data" />
          )}
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Cumulative Revenue">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <LineChart data={dailySeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" minTickGap={26} />
              <YAxis />
              <Tooltip />
              <Line type="monotone" dataKey="cumulativeRevenue" stroke="#1565c0" strokeWidth={2.4} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Fulfillment Mix">
        <div className="admin-chart-wrap">
          {fulfillmentSeries.length > 0 ? (
            <ResponsiveContainer>
              <PieChart>
                <Pie data={fulfillmentSeries} dataKey="value" nameKey="name" outerRadius={108} label>
                  {fulfillmentSeries.map((entry, index) => (
                    <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No fulfillment data" />
          )}
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Revenue by Weekday">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={weekdaySeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="revenue" fill="#0f9d58" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Orders by Hour">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <AreaChart data={hourlySeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="hour" minTickGap={12} />
              <YAxis />
              <Tooltip />
              <Area dataKey="orders" stroke="#8e24aa" fill="#e2cef8" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>
  </Row>
);

interface CatalogTabProps {
  categorySeries: Array<{
    category: string;
    products: number;
    averagePrice: number;
    averageRating: number;
    averagePopularity: number;
  }>;
  popularityScatter: Array<{ name: string; popularity: number; rating: number }>;
  priceBandSeries: Array<{ label: string; count: number }>;
  ratingDistribution: Array<{ bucket: string; count: number }>;
  topProductsSeries: Array<{ name: string; units: number; revenue: number }>;
}

/**
 * Renders catalog analytics charts.
 *
 * @param props The component props.
 * @returns The rendered component tree.
 */
const CatalogTab = ({
  categorySeries,
  popularityScatter,
  priceBandSeries,
  ratingDistribution,
  topProductsSeries,
}: CatalogTabProps): JSX.Element => (
  <Row gutter={[16, 16]}>
    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Top Products by Revenue">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={topProductsSeries.slice(0, 10)} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" />
              <YAxis type="category" dataKey="name" width={140} />
              <Tooltip />
              <Bar dataKey="revenue" fill="#1565c0" radius={[0, 8, 8, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Category Distribution">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={categorySeries.slice(0, 12)}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="category" hide />
              <YAxis />
              <Tooltip />
              <Bar dataKey="products" fill="#00a8a8" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Price Band Coverage">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={priceBandSeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#0f9d58" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Rating Distribution">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={ratingDistribution}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="bucket" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#c27803" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Category Quality Radar">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <RadarChart data={categorySeries.slice(0, 8)}>
              <PolarGrid />
              <PolarAngleAxis dataKey="category" />
              <PolarRadiusAxis />
              <Radar dataKey="averageRating" stroke="#8e24aa" fill="#8e24aa" fillOpacity={0.4} />
              <Tooltip />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24}>
      <Card className="admin-section-card" title="Popularity vs Rating Scatter">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <ScatterChart>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="popularity" name="Popularity" />
              <YAxis dataKey="rating" name="Rating" />
              <Tooltip cursor={{ strokeDasharray: '3 3' }} />
              <Scatter data={popularityScatter} fill="#1565c0" />
            </ScatterChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>
  </Row>
);

interface OpsTabProps {
  approvalSeries: Array<{ status: string; count: number; amount: number }>;
  approvalTimelineSeries: Array<{ date: string; approvals: number; amount: number }>;
  conversionFunnelSeries: Array<{ stage: string; value: number }>;
  storeSeries: Array<{
    store: string;
    orders: number;
    revenue: number;
    shipping: number;
    discount: number;
    averageOrderValue: number;
  }>;
}

/**
 * Renders operations and approval analytics charts.
 *
 * @param props The component props.
 * @returns The rendered component tree.
 */
const OpsTab = ({
  approvalSeries,
  approvalTimelineSeries,
  conversionFunnelSeries,
  storeSeries,
}: OpsTabProps): JSX.Element => (
  <Row gutter={[16, 16]}>
    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Store Revenue Ranking">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={storeSeries.slice(0, 12)} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" />
              <YAxis type="category" dataKey="store" width={140} />
              <Tooltip />
              <Bar dataKey="revenue" fill="#1565c0" radius={[0, 8, 8, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={12}>
      <Card className="admin-section-card" title="Store Order Volume">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={storeSeries.slice(0, 12)}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="store" hide />
              <YAxis />
              <Tooltip />
              <Bar dataKey="orders" fill="#00a8a8" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Approval Status Count">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={approvalSeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="status" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#8e24aa" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Approval Amount by Status">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <BarChart data={approvalSeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="status" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="amount" fill="#c27803" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24} xl={8}>
      <Card className="admin-section-card" title="Order Lifecycle Funnel">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <FunnelChart>
              <Tooltip />
              <Funnel data={conversionFunnelSeries} dataKey="value" nameKey="stage" isAnimationActive={false} />
            </FunnelChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>

    <Col xs={24}>
      <Card className="admin-section-card" title="Approvals Timeline">
        <div className="admin-chart-wrap">
          <ResponsiveContainer>
            <ComposedChart data={approvalTimelineSeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" minTickGap={20} />
              <YAxis yAxisId="left" />
              <YAxis yAxisId="right" orientation="right" />
              <Tooltip />
              <Legend />
              <Bar yAxisId="left" dataKey="approvals" fill="#00a8a8" />
              <Line yAxisId="right" type="monotone" dataKey="amount" stroke="#1565c0" strokeWidth={2} dot={false} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </Col>
  </Row>
);

interface ReportsTabProps {
  categoryReport: CategoryRow[];
  productReport: ProductPerformanceRow[];
  storeReport: StorePerformanceRow[];
}

/**
 * Renders report tables and CSV export actions.
 *
 * @param props The component props.
 * @returns The rendered component tree.
 */
const ReportsTab = ({ categoryReport, productReport, storeReport }: ReportsTabProps): JSX.Element => (
  <Row gutter={[16, 16]}>
    <Col xs={24}>
      <Card
        className="admin-section-card"
        title="Top Product Revenue Report"
        extra={
          <Button
            onClick={() =>
              downloadCsv(
                'top_product_revenue_report.csv',
                ['Rank', 'Product', 'Units', 'Revenue', 'Average Unit Price'],
                productReport.map((item) => [
                  item.rank,
                  item.product,
                  item.units,
                  item.revenue.toFixed(2),
                  item.averageUnitPrice.toFixed(2),
                ]),
              )
            }
          >
            Export CSV
          </Button>
        }
      >
        <Table<ProductPerformanceRow>
          rowKey="key"
          dataSource={productReport}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 900 }}
          columns={[
            { title: 'Rank', dataIndex: 'rank', width: 90 },
            { title: 'Product', dataIndex: 'product' },
            { title: 'Units', dataIndex: 'units', width: 120 },
            { title: 'Revenue', dataIndex: 'revenue', width: 140, render: (value: number) => currency(value) },
            {
              title: 'Avg Unit Price',
              dataIndex: 'averageUnitPrice',
              width: 160,
              render: (value: number) => currency(value),
            },
          ]}
        />
      </Card>
    </Col>

    <Col xs={24}>
      <Card
        className="admin-section-card"
        title="Store Performance Report"
        extra={
          <Button
            onClick={() =>
              downloadCsv(
                'store_performance_report.csv',
                ['Store', 'Orders', 'Revenue', 'Shipping', 'Discount', 'AOV', 'Revenue Share %'],
                storeReport.map((item) => [
                  item.store,
                  item.orders,
                  item.revenue.toFixed(2),
                  item.shipping.toFixed(2),
                  item.discount.toFixed(2),
                  item.averageOrderValue.toFixed(2),
                  item.revenueShare.toFixed(2),
                ]),
              )
            }
          >
            Export CSV
          </Button>
        }
      >
        <Table<StorePerformanceRow>
          rowKey="key"
          dataSource={storeReport}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1040 }}
          columns={[
            { title: 'Store', dataIndex: 'store' },
            { title: 'Orders', dataIndex: 'orders', width: 120 },
            { title: 'Revenue', dataIndex: 'revenue', width: 140, render: (value: number) => currency(value) },
            { title: 'Shipping', dataIndex: 'shipping', width: 140, render: (value: number) => currency(value) },
            { title: 'Discount', dataIndex: 'discount', width: 140, render: (value: number) => currency(value) },
            {
              title: 'Average Order',
              dataIndex: 'averageOrderValue',
              width: 160,
              render: (value: number) => currency(value),
            },
            {
              title: 'Revenue Share',
              dataIndex: 'revenueShare',
              width: 150,
              render: (value: number) => `${value.toFixed(2)}%`,
            },
          ]}
        />
      </Card>
    </Col>

    <Col xs={24}>
      <Card
        className="admin-section-card"
        title="Category Health Report"
        extra={
          <Button
            onClick={() =>
              downloadCsv(
                'category_health_report.csv',
                ['Category', 'Products', 'Avg Price', 'Avg Rating', 'Avg Popularity'],
                categoryReport.map((item) => [
                  item.category,
                  item.products,
                  item.averagePrice.toFixed(2),
                  item.averageRating.toFixed(2),
                  item.averagePopularity.toFixed(2),
                ]),
              )
            }
          >
            Export CSV
          </Button>
        }
      >
        <Table<CategoryRow>
          rowKey="key"
          dataSource={categoryReport}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 980 }}
          columns={[
            { title: 'Category', dataIndex: 'category' },
            { title: 'Products', dataIndex: 'products', width: 120 },
            { title: 'Avg Price', dataIndex: 'averagePrice', width: 140, render: (value: number) => currency(value) },
            { title: 'Avg Rating', dataIndex: 'averageRating', width: 140, render: (value: number) => value.toFixed(2) },
            {
              title: 'Avg Popularity',
              dataIndex: 'averagePopularity',
              width: 170,
              render: (value: number) => value.toFixed(2),
            },
          ]}
        />
      </Card>
    </Col>
  </Row>
);

interface AnalyticsBodyProps {
  activeTab: AnalyticsTab;
  approvals: Approval[];
  orders: Order[];
  products: Product[];
  setActiveTab: (tab: AnalyticsTab) => void;
  storeFilter: string;
  stores: Store[];
  timeframe: Timeframe;
  users: UserProfile[];
}

/**
 * Renders analytics visualization content.
 *
 * @param props The props for this component.
 * @returns The rendered component tree.
 */
const AnalyticsBody = ({
  activeTab,
  approvals,
  orders,
  products,
  setActiveTab,
  storeFilter,
  stores,
  timeframe,
  users,
}: AnalyticsBodyProps): JSX.Element => {
  const storeNameById = useMemo(() => {
    const map = new Map<string, string>();
    stores.forEach((store) => map.set(store.id, store.name));
    return map;
  }, [stores]);

  const filteredOrders = useMemo(() => {
    const cutoff = timeframe === 'all' ? null : Date.now() - timeframeDays[timeframe] * 24 * 60 * 60 * 1000;
    return orders.filter((order) => {
      const timestamp = new Date(order.createdAt).getTime();
      const inWindow = cutoff === null || timestamp >= cutoff;
      const matchesStore =
        storeFilter === 'all'
          ? true
          : storeFilter === 'unassigned'
            ? !order.storeId
            : order.storeId === storeFilter;
      return inWindow && matchesStore;
    });
  }, [orders, storeFilter, timeframe]);

  const summaryStats = useMemo(() => {
    const totalOrders = filteredOrders.length;
    const totalRevenue = filteredOrders.reduce((acc, order) => acc + toNumber(order.totalAmount), 0);
    const totalDiscount = filteredOrders.reduce((acc, order) => acc + toNumber(order.discountAmount), 0);
    const refundedOrders = filteredOrders.filter((order) => order.refundStatus !== 'NONE').length;
    return {
      totalOrders,
      totalRevenue,
      averageOrderValue: totalOrders === 0 ? 0 : totalRevenue / totalOrders,
      discountRate: totalRevenue === 0 ? 0 : (totalDiscount / totalRevenue) * 100,
      refundRate: totalOrders === 0 ? 0 : (refundedOrders / totalOrders) * 100,
      catalogSize: products.length,
      activeStores: stores.filter((store) => store.active).length,
      activeUsers: users.filter((user) => user.enabled).length,
    };
  }, [filteredOrders, products, stores, users]);

  const dailySeries = useMemo<DailyPoint[]>(() => {
    const map = new Map<string, Omit<DailyPoint, 'cumulativeRevenue'>>();
    filteredOrders.forEach((order) => {
      const date = order.createdAt.slice(0, 10);
      const row = map.get(date) ?? {
        date,
        revenue: 0,
        orders: 0,
        discount: 0,
        shipping: 0,
        net: 0,
        averageOrderValue: 0,
      };
      row.revenue += toNumber(order.totalAmount);
      row.orders += 1;
      row.discount += toNumber(order.discountAmount);
      row.shipping += toNumber(order.shippingAmount);
      map.set(date, row);
    });
    let cumulativeRevenue = 0;
    return Array.from(map.values())
      .sort((left, right) => left.date.localeCompare(right.date))
      .map((row) => {
        const averageOrderValue = row.orders === 0 ? 0 : row.revenue / row.orders;
        const net = row.revenue - row.discount;
        cumulativeRevenue += row.revenue;
        return { ...row, net, averageOrderValue, cumulativeRevenue };
      });
  }, [filteredOrders]);

  const statusSeries = useMemo(() => {
    const map = new Map<string, number>();
    filteredOrders.forEach((order) => map.set(order.status, (map.get(order.status) ?? 0) + 1));
    return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
  }, [filteredOrders]);

  const fulfillmentSeries = useMemo(() => {
    const map = new Map<string, number>();
    filteredOrders.forEach((order) => map.set(order.fulfillmentMethod, (map.get(order.fulfillmentMethod) ?? 0) + 1));
    return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
  }, [filteredOrders]);

  const weekdaySeries = useMemo(() => {
    const rows = WEEKDAY_LABELS.map((label, day) => ({ label, day, revenue: 0, orders: 0 }));
    filteredOrders.forEach((order) => {
      const day = new Date(order.createdAt).getDay();
      rows[day].revenue += toNumber(order.totalAmount);
      rows[day].orders += 1;
    });
    return rows;
  }, [filteredOrders]);

  const hourlySeries = useMemo(() => {
    const rows = Array.from({ length: 24 }, (_, hour) => ({
      hour: `${String(hour).padStart(2, '0')}:00`,
      orders: 0,
    }));
    filteredOrders.forEach((order) => {
      const hour = new Date(order.createdAt).getHours();
      rows[hour].orders += 1;
    });
    return rows;
  }, [filteredOrders]);

  const topProductsSeries = useMemo(() => {
    const map = new Map<string, { name: string; units: number; revenue: number }>();
    filteredOrders.forEach((order) => {
      order.items.forEach((item) => {
        const name = item.productName || 'Unknown';
        const row = map.get(name) ?? { name, units: 0, revenue: 0 };
        row.units += toNumber(item.quantity);
        row.revenue += toNumber(item.lineTotal);
        map.set(name, row);
      });
    });
    return Array.from(map.values())
      .sort((left, right) => right.revenue - left.revenue)
      .slice(0, 15);
  }, [filteredOrders]);

  const categorySeries = useMemo(() => {
    const map = new Map<
      string,
      { category: string; products: number; totalPrice: number; totalRating: number; totalPopularity: number }
    >();
    products.forEach((product) => {
      const category = product.category || 'Uncategorized';
      const row = map.get(category) ?? {
        category,
        products: 0,
        totalPrice: 0,
        totalRating: 0,
        totalPopularity: 0,
      };
      row.products += 1;
      row.totalPrice += toNumber(product.price);
      row.totalRating += toNumber(product.averageRating);
      row.totalPopularity += toNumber(product.popularityScore);
      map.set(category, row);
    });
    return Array.from(map.values())
      .map((row) => ({
        category: row.category,
        products: row.products,
        averagePrice: row.products === 0 ? 0 : row.totalPrice / row.products,
        averageRating: row.products === 0 ? 0 : row.totalRating / row.products,
        averagePopularity: row.products === 0 ? 0 : row.totalPopularity / row.products,
      }))
      .sort((left, right) => right.products - left.products);
  }, [products]);

  const priceBandSeries = useMemo(() => {
    const bands = [
      { label: '$0-$25', min: 0, max: 25, count: 0 },
      { label: '$25-$50', min: 25, max: 50, count: 0 },
      { label: '$50-$100', min: 50, max: 100, count: 0 },
      { label: '$100-$250', min: 100, max: 250, count: 0 },
      { label: '$250+', min: 250, max: Number.POSITIVE_INFINITY, count: 0 },
    ];
    products.forEach((product) => {
      const band = bands.find((item) => {
        const price = toNumber(product.price);
        return price >= item.min && price < item.max;
      });
      if (band) {
        band.count += 1;
      }
    });
    return bands.map(({ label, count }) => ({ label, count }));
  }, [products]);

  const ratingDistribution = useMemo(() => {
    const buckets = [0, 0, 0, 0, 0];
    products.forEach((product) => {
      const index = Math.min(4, Math.max(0, Math.floor(toNumber(product.averageRating))));
      buckets[index] += 1;
    });
    return buckets.map((count, index) => ({ bucket: `${index}-${index + 1}`, count }));
  }, [products]);

  const popularityScatter = useMemo(
    () =>
      products
        .slice(0, 140)
        .map((product) => ({
          name: product.name,
          popularity: toNumber(product.popularityScore),
          rating: toNumber(product.averageRating),
        }))
        .filter((row) => row.popularity > 0 || row.rating > 0),
    [products],
  );

  const storeSeries = useMemo(() => {
    const map = new Map<string, { store: string; orders: number; revenue: number; shipping: number; discount: number }>();
    filteredOrders.forEach((order) => {
      const store = order.storeId ? storeNameById.get(order.storeId) ?? 'Unknown Store' : 'Unassigned Store';
      const row = map.get(store) ?? { store, orders: 0, revenue: 0, shipping: 0, discount: 0 };
      row.orders += 1;
      row.revenue += toNumber(order.totalAmount);
      row.shipping += toNumber(order.shippingAmount);
      row.discount += toNumber(order.discountAmount);
      map.set(store, row);
    });
    return Array.from(map.values())
      .map((row) => ({
        ...row,
        averageOrderValue: row.orders === 0 ? 0 : row.revenue / row.orders,
      }))
      .sort((left, right) => right.revenue - left.revenue);
  }, [filteredOrders, storeNameById]);

  const approvalSeries = useMemo(() => {
    const map = new Map<string, { status: string; count: number; amount: number }>();
    approvals.forEach((approval) => {
      const row = map.get(approval.status) ?? { status: approval.status, count: 0, amount: 0 };
      row.count += 1;
      row.amount += toNumber(approval.amount);
      map.set(approval.status, row);
    });
    return Array.from(map.values());
  }, [approvals]);

  const approvalTimelineSeries = useMemo(() => {
    const map = new Map<string, { date: string; approvals: number; amount: number }>();
    approvals.forEach((approval) => {
      const date = approval.createdAt.slice(0, 10);
      const row = map.get(date) ?? { date, approvals: 0, amount: 0 };
      row.approvals += 1;
      row.amount += toNumber(approval.amount);
      map.set(date, row);
    });
    return Array.from(map.values()).sort((left, right) => left.date.localeCompare(right.date));
  }, [approvals]);

  const conversionFunnelSeries = useMemo(() => {
    const created = filteredOrders.length;
    const paid = filteredOrders.filter((order) => ['PAID', 'PACKED', 'SHIPPED', 'DELIVERED', 'REFUNDED'].includes(order.status)).length;
    const shipped = filteredOrders.filter((order) => ['SHIPPED', 'DELIVERED'].includes(order.status)).length;
    const delivered = filteredOrders.filter((order) => order.status === 'DELIVERED').length;
    const refunded = filteredOrders.filter((order) => order.status === 'REFUNDED').length;
    return [
      { stage: 'Created', value: created },
      { stage: 'Paid', value: paid },
      { stage: 'Shipped', value: shipped },
      { stage: 'Delivered', value: delivered },
      { stage: 'Refunded', value: refunded },
    ];
  }, [filteredOrders]);

  const productReport = useMemo<ProductPerformanceRow[]>(
    () =>
      topProductsSeries.map((row, index) => ({
        key: row.name,
        rank: index + 1,
        product: row.name,
        units: row.units,
        revenue: row.revenue,
        averageUnitPrice: row.units === 0 ? 0 : row.revenue / row.units,
      })),
    [topProductsSeries],
  );

  const storeReport = useMemo<StorePerformanceRow[]>(
    () =>
      storeSeries.map((row) => ({
        key: row.store,
        store: row.store,
        orders: row.orders,
        revenue: row.revenue,
        shipping: row.shipping,
        discount: row.discount,
        averageOrderValue: row.averageOrderValue,
        revenueShare: summaryStats.totalRevenue === 0 ? 0 : (row.revenue / summaryStats.totalRevenue) * 100,
      })),
    [storeSeries, summaryStats.totalRevenue],
  );

  const categoryReport = useMemo<CategoryRow[]>(
    () =>
      categorySeries.map((row) => ({
        key: row.category,
        category: row.category,
        products: row.products,
        averagePrice: row.averagePrice,
        averageRating: row.averageRating,
        averagePopularity: row.averagePopularity,
      })),
    [categorySeries],
  );

  return (
    <>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Revenue" value={summaryStats.totalRevenue} precision={2} prefix="$" />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Orders" value={summaryStats.totalOrders} />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Avg Order Value" value={summaryStats.averageOrderValue} precision={2} prefix="$" />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Refund Rate" value={summaryStats.refundRate} precision={2} suffix="%" />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Discount Rate" value={summaryStats.discountRate} precision={2} suffix="%" />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Catalog Size" value={summaryStats.catalogSize} suffix="products" />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Active Stores" value={summaryStats.activeStores} />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card className="admin-metric-card">
            <Statistic title="Enabled Users" value={summaryStats.activeUsers} />
          </Card>
        </Col>
      </Row>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => setActiveTab(key as AnalyticsTab)}
        items={[
          { key: 'revenue', label: 'Revenue & Orders' },
          { key: 'catalog', label: 'Catalog Analytics' },
          { key: 'ops', label: 'Ops & Approvals' },
          { key: 'reports', label: 'Reports' },
        ]}
      />

      {activeTab === 'revenue' ? (
        <RevenueTab dailySeries={dailySeries} fulfillmentSeries={fulfillmentSeries} hourlySeries={hourlySeries} statusSeries={statusSeries} weekdaySeries={weekdaySeries} />
      ) : null}

      {activeTab === 'catalog' ? (
        <CatalogTab categorySeries={categorySeries} popularityScatter={popularityScatter} priceBandSeries={priceBandSeries} ratingDistribution={ratingDistribution} topProductsSeries={topProductsSeries} />
      ) : null}

      {activeTab === 'ops' ? (
        <OpsTab approvalSeries={approvalSeries} approvalTimelineSeries={approvalTimelineSeries} conversionFunnelSeries={conversionFunnelSeries} storeSeries={storeSeries} />
      ) : null}

      {activeTab === 'reports' ? (
        <ReportsTab categoryReport={categoryReport} productReport={productReport} storeReport={storeReport} />
      ) : null}
    </>
  );
};
