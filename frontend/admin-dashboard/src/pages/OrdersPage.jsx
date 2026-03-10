import { Fragment, useEffect, useState } from 'react'
import { getOrder, getOrderTimeline, listOrders, updateOrderStatus } from '../shared/api/endpoints/ordersApi'
import { Spinner } from '../shared/ui/Spinner'
import { SortableHeader } from '../shared/ui/SortableHeader'

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
  const [orderSort, setOrderSort] = useState({ sortBy: 'createdAt', direction: 'desc' })
  const [drafts, setDrafts] = useState({})
  const [timelines, setTimelines] = useState({})
  const [timelineLoadingId, setTimelineLoadingId] = useState(null)
  const [detailOrder, setDetailOrder] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const page = await listOrders({ page: 0, size: 100, sortBy: orderSort.sortBy, direction: orderSort.direction })
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

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderSort.sortBy, orderSort.direction])

  function handleOrderSort(field, dir) {
    setOrderSort({ sortBy: field, direction: dir })
  }

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

  async function openDetail(orderId) {
    setDetailLoading(true)
    setError('')
    try {
      const data = await getOrder(orderId)
      setDetailOrder(data)
    } catch (err) {
      setError(err.message || 'Failed to load order details.')
    } finally {
      setDetailLoading(false)
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
                <SortableHeader label="Order" field="id" sortBy={orderSort.sortBy} direction={orderSort.direction} onSort={handleOrderSort} />
                <th>User</th>
                <th>Items</th>
                <SortableHeader label="Total" field="totalAmount" sortBy={orderSort.sortBy} direction={orderSort.direction} onSort={handleOrderSort} />
                <SortableHeader label="Status" field="status" sortBy={orderSort.sortBy} direction={orderSort.direction} onSort={handleOrderSort} />
                <th>Refund</th>
                <SortableHeader label="Created" field="createdAt" sortBy={orderSort.sortBy} direction={orderSort.direction} onSort={handleOrderSort} />
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
                              onClick={() => openDetail(order.id)}
                              disabled={detailLoading}
                            >
                              Details
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

      {detailOrder ? (
        <div className="modal-backdrop" onClick={() => setDetailOrder(null)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Order details</h3>
              <button className="modal-close" onClick={() => setDetailOrder(null)}>&times;</button>
            </div>

            <dl className="detail-grid">
              <div>
                <dt>Order ID</dt>
                <dd className="mono">{detailOrder.id}</dd>
              </div>
              <div>
                <dt>User ID</dt>
                <dd className="mono">{detailOrder.userId || '-'}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd><span className="badge badge-muted">{detailOrder.status}</span></dd>
              </div>
              <div>
                <dt>Refund status</dt>
                <dd><span className="badge badge-muted">{detailOrder.refundStatus || 'NONE'}</span></dd>
              </div>
              <div>
                <dt>Total</dt>
                <dd><strong>{formatCurrency(detailOrder.totalAmount)}</strong></dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{formatDate(detailOrder.createdAt)}</dd>
              </div>
              {detailOrder.shippingAddress ? (
                <div className="span-2">
                  <dt>Shipping address</dt>
                  <dd>{typeof detailOrder.shippingAddress === 'string' ? detailOrder.shippingAddress : JSON.stringify(detailOrder.shippingAddress)}</dd>
                </div>
              ) : null}
              {detailOrder.paymentMethod ? (
                <div>
                  <dt>Payment method</dt>
                  <dd>{detailOrder.paymentMethod}</dd>
                </div>
              ) : null}
              {detailOrder.notes ? (
                <div className="span-2">
                  <dt>Notes</dt>
                  <dd>{detailOrder.notes}</dd>
                </div>
              ) : null}
            </dl>

            {detailOrder.items?.length ? (
              <>
                <h4>Line items</h4>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>SKU</th>
                        <th>Qty</th>
                        <th>Unit price</th>
                        <th>Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailOrder.items.map((item, idx) => (
                        <tr key={item.id || idx}>
                          <td>{item.productName || item.name || '-'}</td>
                          <td className="mono">{item.sku || item.variantSku || '-'}</td>
                          <td>{item.quantity}</td>
                          <td>{formatCurrency(item.unitPrice || item.price)}</td>
                          <td>{formatCurrency((item.unitPrice || item.price || 0) * (item.quantity || 1))}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  )
}
