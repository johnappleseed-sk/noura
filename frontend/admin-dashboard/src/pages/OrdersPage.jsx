import { Fragment, useEffect, useState } from 'react'
import { getOrderTimeline, listOrders, updateOrderStatus } from '../shared/api/endpoints/ordersApi'
import { Spinner } from '../shared/ui/Spinner'

const ORDER_STATUSES = [
  'CREATED',
  'REVIEWED',
  'PAYMENT_PENDING',
  'PAID',
  'PACKED',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED'
]

const REFUND_STATUSES = ['NONE', 'REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED']

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

export function OrdersPage() {
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [orders, setOrders] = useState([])
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [refundFilter, setRefundFilter] = useState('')
  const [drafts, setDrafts] = useState({})
  const [timelines, setTimelines] = useState({})
  const [timelineLoadingId, setTimelineLoadingId] = useState(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const page = await listOrders({ page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' })
      const content = page?.content || []
      setOrders(content)
      setDrafts(
        Object.fromEntries(
          content.map((order) => [
            order.id,
            { status: order.status, refundStatus: order.refundStatus }
          ])
        )
      )
    } catch (err) {
      setError(err.message || 'Failed to load orders.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const normalizedSearch = search.trim().toLowerCase()
  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      !normalizedSearch ||
      order.id?.toLowerCase().includes(normalizedSearch) ||
      order.userId?.toLowerCase().includes(normalizedSearch)
    const matchesStatus = !statusFilter || order.status === statusFilter
    const matchesRefund = !refundFilter || order.refundStatus === refundFilter
    return matchesSearch && matchesStatus && matchesRefund
  })

  async function saveOrder(orderId) {
    const draft = drafts[orderId]
    if (!draft) return
    setSavingId(orderId)
    setFlash('')
    setError('')
    try {
      const updated = await updateOrderStatus(orderId, draft)
      setFlash('Order updated.')
      setOrders((current) => current.map((item) => (item.id === orderId ? updated : item)))
      setDrafts((current) => ({ ...current, [orderId]: { status: updated.status, refundStatus: updated.refundStatus } }))
    } catch (err) {
      setError(err.message || 'Failed to update order.')
    } finally {
      setSavingId(null)
    }
  }

  async function toggleTimeline(orderId) {
    if (timelines[orderId]) {
      setTimelines((current) => {
        const next = { ...current }
        delete next[orderId]
        return next
      })
      return
    }

    setTimelineLoadingId(orderId)
    setError('')
    try {
      const data = await getOrderTimeline(orderId)
      setTimelines((current) => ({ ...current, [orderId]: data }))
    } catch (err) {
      setError(err.message || 'Failed to load timeline.')
    } finally {
      setTimelineLoadingId(null)
    }
  }

  if (loading) {
    return <Spinner label="Loading orders..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Orders</h2>
        <p>Review admin-visible orders, adjust state, and inspect timeline events.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="filters">
          <label>
            Search
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Order id or user id"
            />
          </label>
          <label>
            Order status
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="">All</option>
              {ORDER_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <label>
            Refund status
            <select value={refundFilter} onChange={(event) => setRefundFilter(event.target.value)}>
              <option value="">All</option>
              {REFUND_STATUSES.map((status) => (
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
                <th>User</th>
                <th>Items</th>
                <th>Total</th>
                <th>Status</th>
                <th>Refund</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.length ? (
                filteredOrders.map((order) => {
                  const timeline = timelines[order.id]
                  const draft = drafts[order.id] || { status: order.status, refundStatus: order.refundStatus }
                  return (
                    <Fragment key={order.id}>
                      <tr>
                        <td className="mono">{order.id}</td>
                        <td className="mono">{order.userId || '-'}</td>
                        <td>{order.items?.length || 0}</td>
                        <td>{formatCurrency(order.totalAmount)}</td>
                        <td>
                          <select
                            value={draft.status}
                            onChange={(event) =>
                              setDrafts((current) => ({
                                ...current,
                                [order.id]: { ...draft, status: event.target.value }
                              }))
                            }
                          >
                            {ORDER_STATUSES.map((status) => (
                              <option key={status} value={status}>
                                {status}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <select
                            value={draft.refundStatus}
                            onChange={(event) =>
                              setDrafts((current) => ({
                                ...current,
                                [order.id]: { ...draft, refundStatus: event.target.value }
                              }))
                            }
                          >
                            {REFUND_STATUSES.map((status) => (
                              <option key={status} value={status}>
                                {status}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>{formatDate(order.createdAt)}</td>
                        <td>
                          <div className="inline-actions">
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => saveOrder(order.id)}
                              disabled={savingId === order.id}
                            >
                              {savingId === order.id ? 'Saving...' : 'Save'}
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => toggleTimeline(order.id)}
                              disabled={timelineLoadingId === order.id}
                            >
                              {timeline ? 'Hide timeline' : timelineLoadingId === order.id ? 'Loading...' : 'Timeline'}
                            </button>
                          </div>
                        </td>
                      </tr>
                      {timeline ? (
                        <tr key={`${order.id}-timeline`}>
                          <td colSpan="8" className="timeline-cell">
                            <ul className="timeline-list">
                              {timeline.length ? (
                                timeline.map((event) => (
                                  <li key={event.id}>
                                    <strong>{event.status}</strong>
                                    <span>{event.note || 'Status update'}</span>
                                    <small>{formatDate(event.createdAt)}</small>
                                  </li>
                                ))
                              ) : (
                                <li>No timeline events recorded.</li>
                              )}
                            </ul>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  )
                })
              ) : (
                <tr>
                  <td colSpan="8" className="empty-row">No orders match the current filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
