import { Fragment, useEffect, useState } from 'react'
import { getOrder, getOrderTimeline, listOrders, updateOrderStatus } from '../shared/api/endpoints/ordersApi'
import { PaginationControls } from '../shared/ui/PaginationControls'
import { Spinner } from '../shared/ui/Spinner'
import { SortableHeader } from '../shared/ui/SortableHeader'
import { useToastFeedback } from '../shared/ui/useToastFeedback'

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
const DEFAULT_FILTERS = { query: '', status: '', refundStatus: '' }
const DEFAULT_PAGE_SIZE = 20

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

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

export function OrdersPage() {
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  useToastFeedback({ successMessage: flash, errorMessage: error })
  const [orders, setOrders] = useState([])
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [pageState, setPageState] = useState({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 1,
    first: true,
    last: true
  })
  const [refreshTick, setRefreshTick] = useState(0)
  const [orderSort, setOrderSort] = useState({ sortBy: 'createdAt', direction: 'desc' })
  const [drafts, setDrafts] = useState({})
  const [timelines, setTimelines] = useState({})
  const [timelineLoadingId, setTimelineLoadingId] = useState(null)
  const [detailOrder, setDetailOrder] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  async function load({ page = pageState.page, size = pageState.size } = {}) {
    setLoading(true)
    setError('')
    try {
      const query = debouncedQuery.trim()
      const response = await listOrders({
        page,
        size,
        sortBy: orderSort.sortBy,
        direction: orderSort.direction,
        query: query || undefined,
        status: filters.status || undefined,
        refundStatus: filters.refundStatus || undefined
      })
      const content = Array.isArray(response) ? response : response?.content || []
      const normalizedPage = Array.isArray(response)
        ? {
            page: 0,
            size,
            totalElements: content.length,
            totalPages: 1,
            first: true,
            last: true
          }
        : {
            page: Number(response?.page ?? page),
            size: Number(response?.size ?? size),
            totalElements: Number(response?.totalElements ?? content.length),
            totalPages: Math.max(1, Number(response?.totalPages ?? 1)),
            first: Boolean(response?.first ?? page <= 0),
            last: Boolean(response?.last ?? page >= Math.max(0, Number(response?.totalPages ?? 1) - 1))
          }

      setOrders(content)
      setPageState(normalizedPage)
      setDrafts((current) => {
        const next = {}
        content.forEach((order) => {
          next[order.id] = current[order.id] || {
            status: order.status,
            refundStatus: order.refundStatus
          }
        })
        return next
      })

      setTimelines({})
      setDetailOrder(null)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load orders.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const handle = window.setTimeout(() => {
      setDebouncedQuery(filters.query)
    }, 350)
    return () => window.clearTimeout(handle)
  }, [filters.query])

  useEffect(() => {
    load({ page: pageState.page, size: pageState.size })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    pageState.page,
    pageState.size,
    orderSort.sortBy,
    orderSort.direction,
    debouncedQuery,
    filters.status,
    filters.refundStatus,
    refreshTick
  ])

  function handleOrderSort(field, dir) {
    setOrderSort({ sortBy: field, direction: dir })
    setPageState((current) => ({ ...current, page: 0 }))
  }

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
      setError(getErrorMessage(err, 'Failed to update order.'))
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
      setError(getErrorMessage(err, 'Failed to load timeline.'))
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
      setError(getErrorMessage(err, 'Failed to load order details.'))
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

      <section className="panel">
        <form className="control-toolbar" onSubmit={(event) => event.preventDefault()}>
          <label className="toolbar-field toolbar-grow">
            <span>Search</span>
            <input
              className="toolbar-control"
              value={filters.query}
              onChange={(event) => {
                setFilters((current) => ({ ...current, query: event.target.value }))
                setPageState((current) => ({ ...current, page: 0 }))
              }}
              placeholder="Order id or user id"
            />
          </label>
          <label className="toolbar-field">
            <span>Order status</span>
            <select
              className="toolbar-control"
              value={filters.status}
              onChange={(event) => {
                setFilters((current) => ({ ...current, status: event.target.value }))
                setPageState((current) => ({ ...current, page: 0 }))
              }}
            >
              <option value="">All</option>
              {ORDER_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-field">
            <span>Refund status</span>
            <select
              className="toolbar-control"
              value={filters.refundStatus}
              onChange={(event) => {
                setFilters((current) => ({ ...current, refundStatus: event.target.value }))
                setPageState((current) => ({ ...current, page: 0 }))
              }}
            >
              <option value="">All</option>
              {REFUND_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <div className="toolbar-actions">
            <button
              className="btn btn-outline toolbar-btn"
              type="button"
              onClick={() => {
                setFilters(DEFAULT_FILTERS)
                setPageState((current) => ({ ...current, page: 0 }))
              }}
            >
              Reset
            </button>
            <button
              className="btn btn-outline toolbar-btn"
              type="button"
              onClick={() => setRefreshTick((current) => current + 1)}
            >
              Refresh
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Order list</h3>
            <p>
              Showing {orders.length} of {pageState.totalElements || orders.length} orders
            </p>
          </div>
          <PaginationControls
            page={pageState.page}
            totalPages={pageState.totalPages}
            totalElements={pageState.totalElements}
            first={pageState.first}
            last={pageState.last}
            pageSize={pageState.size}
            onPageSizeChange={(nextSize) => {
              setPageState((current) => ({ ...current, page: 0, size: nextSize }))
            }}
            onPrev={() => setPageState((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
            onNext={() => setPageState((current) => ({ ...current, page: Math.min(current.totalPages - 1, current.page + 1) }))}
            noun="orders"
          />
        </div>

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
              {orders.length ? (
                orders.map((order) => {
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
