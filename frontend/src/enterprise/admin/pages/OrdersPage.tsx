import { Button, Select, Space, Table, Tag, Typography, message } from 'antd'
import { useEffect, useState } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { fetchOrders, updateOrderStatus } from '@/admin/features/orders/ordersSlice'
import { Order, OrderStatus, RefundStatus } from '@/admin/types'

const statusOptions: OrderStatus[] = [
  'CREATED',
  'REVIEWED',
  'PAYMENT_PENDING',
  'PAID',
  'PACKED',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED',
]

const refundOptions: RefundStatus[] = ['NONE', 'REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED']

/**
 * Renders the OrdersPage component.
 *
 * @returns The rendered component tree.
 */
export const OrdersPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { items, loading } = useAdminSelector((state) => state.orders)
  const [draftStatus, setDraftStatus] = useState<Record<string, OrderStatus>>({})
  const [draftRefundStatus, setDraftRefundStatus] = useState<Record<string, RefundStatus>>({})

  useEffect(() => {
    void dispatch(fetchOrders())
  }, [dispatch])

  /**
   * Executes apply update.
   *
   * @param order The order value.
   * @returns No value.
   */
  const applyUpdate = async (order: Order): Promise<void> => {
    const status = draftStatus[order.id] ?? order.status
    const refundStatus = draftRefundStatus[order.id] ?? order.refundStatus
    await dispatch(updateOrderStatus({ id: order.id, status, refundStatus }))
    message.success(`Order ${order.id.slice(0, 8)} updated`)
  }

  return (
    <>
      <Typography.Title level={3}>Order Management</Typography.Title>
      <Table<Order>
        rowKey="id"
        loading={loading}
        dataSource={items}
        pagination={{ pageSize: 20 }}
        columns={[
          { title: 'Order ID', dataIndex: 'id', render: (id: string) => id.slice(0, 12) },
          { title: 'Date', dataIndex: 'createdAt', render: (value: string) => new Date(value).toLocaleString() },
          { title: 'Total', dataIndex: 'totalAmount', render: (value: number) => `$${value.toFixed(2)}` },
          {
            title: 'Status',
            render: (_, record) => <Tag color="blue">{draftStatus[record.id] ?? record.status}</Tag>,
          },
          {
            title: 'Refund',
            render: (_, record) => <Tag color="purple">{draftRefundStatus[record.id] ?? record.refundStatus}</Tag>,
          },
          {
            title: 'Actions',
            render: (_, record) => (
              <Space direction="vertical">
                <Space>
                  <Select<OrderStatus>
                    options={statusOptions.map((status) => ({ label: status, value: status }))}
                    size="small"
                    style={{ width: 160 }}
                    value={draftStatus[record.id] ?? record.status}
                    onChange={(value) => setDraftStatus((state) => ({ ...state, [record.id]: value }))}
                  />
                  <Select<RefundStatus>
                    options={refundOptions.map((status) => ({ label: status, value: status }))}
                    size="small"
                    style={{ width: 160 }}
                    value={draftRefundStatus[record.id] ?? record.refundStatus}
                    onChange={(value) => setDraftRefundStatus((state) => ({ ...state, [record.id]: value }))}
                  />
                </Space>
                <Button size="small" type="primary" onClick={() => void applyUpdate(record)}>
                  Save
                </Button>
              </Space>
            ),
          },
        ]}
      />
    </>
  )
}
