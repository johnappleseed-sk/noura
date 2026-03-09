import { useEffect, useMemo, useState } from 'react'
import { listOrders, updateOrderStatus } from '../shared/api/endpoints/ordersApi'
import { Spinner } from '../shared/ui/Spinner'

const REFUND_STATUS_FLOW = ['NONE', 'REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED']
const REFUNDABLE_ORDER_STATUSES = new Set(['PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'])

function formatCurrency(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2
  }).format(Number(amount || 0))
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value))
}

export function ReturnsPage() {
  const [loading, setLoading] = useState(true)
  const [workingId, setWorkingId] = useState(null)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [refundFilter, setRefundFilter] = useState('')
  const [orders, setOrders] = useState([])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const page = await listOrders({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' })
      setOrders(page?.content || [])
    } catch (err) {
      setError(err.message || 'Failed to load refund workflow.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredOrders = useMemo(() => {
    return orders.filter((order) => !refundFilter || order.refundStatus === refundFilter)
  }, [orders, refundFilter])

  async function applyRefundUpdate(order, nextRefundStatus, markOrderRefunded = false) {
    setWorkingId(order.id)
    setFlash('')
    setError('')
    try {
      const payload = {
        status: markOrderRefunded ? 'REFUNDED' : order.status,
        refundStatus: nextRefundStatus
      }
      const updated = await updateOrderStatus(order.id, payload)
      setOrders((current) => current.map((item) => (item.id === order.id ? updated : item)))
      setFlash('Refund workflow updated.')
    } catch (err) {
      setError(err.message || 'Failed to update refund workflow.')
    } finally {
      setWorkingId(null)
    }
  }

  if (loading) {
    return <Spinner label="Loading refund workflow..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Returns and refunds</h2>
        <p>Current runtime uses the active platform refund workflow on orders instead of the archived Thymeleaf RMA screens.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="filters">
          <label>
            Refund status
            <select value={refundFilter} onChange={(event) => setRefundFilter(event.target.value)}>
              <option value="">All</option>
              {REFUND_STATUS_FLOW.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <button className="btn btn-outline" onClick={load}>
            Refresh
          </button>
        </div>
      </section>

      <section className="panel">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>Total</th>
                <th>Order status</th>
                <th>Refund status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.length ? (
                filteredOrders.map((order) => (
                  <tr key={order.id}>
                    <td className="mono">{order.id}</td>
                    <td>{formatCurrency(order.totalAmount)}</td>
                    <td>{order.status}</td>
                    <td>
                      <span className={`badge badge-${order.refundStatus === 'COMPLETED' ? 'success' : order.refundStatus === 'REJECTED' ? 'danger' : order.refundStatus === 'APPROVED' ? 'info' : order.refundStatus === 'REQUESTED' ? 'warning' : 'muted'}`}>
                        {order.refundStatus}
                      </span>
                    </td>
                    <td>{formatDate(order.createdAt)}</td>
                    <td>
                      <div className="inline-actions wrap">
                        {order.refundStatus === 'NONE' ? (
                          <button
                            className="btn btn-outline btn-sm"
                            disabled={workingId === order.id}
                            onClick={() => applyRefundUpdate(order, 'REQUESTED')}
                          >
                            Request refund
                          </button>
                        ) : null}
                        {order.refundStatus === 'REQUESTED' ? (
                          <>
                            <button
                              className="btn btn-primary btn-sm"
                              disabled={workingId === order.id}
                              onClick={() => applyRefundUpdate(order, 'APPROVED')}
                            >
                              Approve
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              disabled={workingId === order.id}
                              onClick={() => applyRefundUpdate(order, 'REJECTED')}
                            >
                              Reject
                            </button>
                          </>
                        ) : null}
                        {order.refundStatus === 'APPROVED' ? (
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={workingId === order.id}
                            onClick={() => applyRefundUpdate(order, 'COMPLETED')}
                          >
                            Complete refund
                          </button>
                        ) : null}
                        {order.refundStatus === 'COMPLETED' && order.status !== 'REFUNDED' ? (
                          <button
                            className="btn btn-outline btn-sm"
                            disabled={workingId === order.id || !REFUNDABLE_ORDER_STATUSES.has(order.status)}
                            onClick={() => applyRefundUpdate(order, 'COMPLETED', true)}
                          >
                            Mark order refunded
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" className="empty-row">No refund workflow entries match the current filter.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
