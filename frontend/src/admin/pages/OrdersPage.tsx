import { App as AntApp, Button, Card, Drawer, Select, Space, Table, Tag, Timeline, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/api/adminApi';
import { useAppDispatch } from '@/app/hooks';
import { PageHeader } from '@/components/PageHeader';
import { fetchNotifications, fetchUnreadCount } from '@/features/notifications/notificationsSlice';
import type { PageResponse } from '@/types/api';
import type { Order, OrderStatus, OrderTimelineEvent, RefundStatus } from '@/types/models';

const ORDER_STATUSES: OrderStatus[] = [
  'CREATED',
  'REVIEWED',
  'PAYMENT_PENDING',
  'PAID',
  'PACKED',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED',
];

const REFUND_STATUSES: RefundStatus[] = ['NONE', 'REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED'];

interface StatusDraft {
  status: OrderStatus;
  refundStatus: RefundStatus;
}

/**
 * Renders the OrdersPage component.
 *
 * @returns The rendered component tree.
 */
export const OrdersPage = (): JSX.Element => {
  const dispatch = useAppDispatch();
  const { message } = AntApp.useApp();
  const [pageData, setPageData] = useState<PageResponse<Order> | null>(null);
  const [loading, setLoading] = useState(false);
  const [drafts, setDrafts] = useState<Record<string, StatusDraft>>({});
  const [timelineOrderId, setTimelineOrderId] = useState<string | null>(null);
  const [timelineEvents, setTimelineEvents] = useState<OrderTimelineEvent[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getOrders({ page: 0, size: 50, sortBy: 'createdAt', direction: 'desc' });
      setPageData(result);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      void load();
    }, 15000);
    return () => window.clearInterval(timer);
  }, [load]);

  const draftFor = (order: Order): StatusDraft => drafts[order.id] ?? { status: order.status, refundStatus: order.refundStatus };

  /**
   * Loads order timeline.
   *
   * @param orderId The order id used to locate the target record.
   * @returns No value.
   */
  const loadTimeline = useCallback(
    async (orderId: string): Promise<void> => {
      setTimelineLoading(true);
      try {
        const events = await adminApi.getOrderTimeline(orderId);
        setTimelineEvents(events);
      } catch (error) {
        message.error(error instanceof Error ? error.message : 'Failed to load order timeline');
      } finally {
        setTimelineLoading(false);
      }
    },
    [message],
  );

  /**
   * Updates update order.
   *
   * @param order The order value.
   * @returns No value.
   */
  const updateOrder = async (order: Order): Promise<void> => {
    const draft = draftFor(order);
    try {
      await adminApi.updateOrderStatus(order.id, draft.status, draft.refundStatus);
      message.success('Order status updated');
      await load();
      if (timelineOrderId === order.id) {
        await loadTimeline(order.id);
      }
      void dispatch(fetchNotifications());
      void dispatch(fetchUnreadCount());
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to update order');
    }
  };

  return (
    <>
      <PageHeader
        title="Order Management"
        subtitle="Review order progress, manage refunds, and inspect event timelines for operational clarity."
      />

      <Card className="admin-section-card admin-data-card">
        <Table<Order>
          rowKey="id"
          loading={loading}
          dataSource={pageData?.content ?? []}
          pagination={false}
          scroll={{ x: 1320 }}
          columns={[
            { title: 'Order ID', dataIndex: 'id', width: 240 },
            { title: 'User', dataIndex: 'userId', width: 240 },
            {
              title: 'Status',
              render: (_, record) => <Tag color="blue">{record.status}</Tag>,
            },
            {
              title: 'Refund',
              render: (_, record) => <Tag>{record.refundStatus}</Tag>,
            },
            {
              title: 'Total',
              dataIndex: 'totalAmount',
              render: (value: number) => `$${Number(value).toFixed(2)}`,
            },
            {
              title: 'Created',
              dataIndex: 'createdAt',
              render: (value: string) => new Date(value).toLocaleString(),
            },
            {
              title: 'Actions',
              width: 380,
              render: (_, record) => (
                <Space wrap>
                  <Select<OrderStatus>
                    style={{ width: 130 }}
                    value={draftFor(record).status}
                    options={ORDER_STATUSES.map((status) => ({ label: status, value: status }))}
                    onChange={(status) =>
                      setDrafts((current) => ({
                        ...current,
                        [record.id]: {
                          ...draftFor(record),
                          status,
                        },
                      }))
                    }
                  />
                  <Select<RefundStatus>
                    style={{ width: 130 }}
                    value={draftFor(record).refundStatus}
                    options={REFUND_STATUSES.map((refundStatus) => ({ label: refundStatus, value: refundStatus }))}
                    onChange={(refundStatus) =>
                      setDrafts((current) => ({
                        ...current,
                        [record.id]: {
                          ...draftFor(record),
                          refundStatus,
                        },
                      }))
                    }
                  />
                  <Button type="primary" onClick={() => void updateOrder(record)}>
                    Save
                  </Button>
                  <Button
                    onClick={() => {
                      setTimelineOrderId(record.id);
                      void loadTimeline(record.id);
                    }}
                  >
                    Timeline
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Drawer
        title={`Order Timeline${timelineOrderId ? ` - ${timelineOrderId}` : ''}`}
        open={Boolean(timelineOrderId)}
        onClose={() => {
          setTimelineOrderId(null);
          setTimelineEvents([]);
        }}
        width={560}
      >
        <Timeline
          pending={timelineLoading ? 'Loading timeline...' : null}
          items={timelineEvents.map((event) => ({
            color: event.status === 'CANCELLED' || event.status === 'REFUNDED' ? 'red' : 'blue',
            children: (
              <Space direction="vertical" size={2}>
                <Space>
                  <Tag color="blue">{event.status}</Tag>
                  <Tag>{event.refundStatus}</Tag>
                  {event.actor ? <Typography.Text type="secondary">by {event.actor}</Typography.Text> : null}
                </Space>
                {event.note ? <Typography.Text>{event.note}</Typography.Text> : null}
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {new Date(event.createdAt).toLocaleString()}
                </Typography.Text>
              </Space>
            ),
          }))}
        />
      </Drawer>
    </>
  );
};
