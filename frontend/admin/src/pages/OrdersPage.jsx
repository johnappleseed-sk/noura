import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listOrders, updateOrderStatus, cancelOrder } from '../shared/api/endpoints/ordersApi'
import { Spinner } from '../shared/ui/Spinner'

const STATUS_COLORS = {
  PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PROCESSING: 'bg-purple-100 text-purple-800',
  SHIPPED: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  REFUNDED: 'bg-gray-100 text-gray-800',
  PAYMENT_FAILED: 'bg-red-100 text-red-800'
}

export function OrdersPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [orders, setOrders] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await listOrders({
        status: statusFilter || undefined,
        q: searchQuery || undefined,
        page,
        size: 20,
        sort: 'createdAt',
        dir: 'desc'
      })
      setOrders(result?.items || [])
      setTotalPages(result?.totalPages || 0)
    } catch (err) {
      setError(err.message || 'Failed to load orders.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [statusFilter, page])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    load()
  }

  const handleCancel = async (id) => {
    const reason = prompt('Enter cancellation reason:')
    if (!reason) return
    setError('')
    setFlash('')
    try {
      await cancelOrder(id, reason)
      setFlash('Order cancelled.')
      await load()
    } catch (err) {
      setError(err.message || 'Failed to cancel order.')
    }
  }

  if (loading) return <Spinner label="Loading orders..." />

  return (
    <div className="page">
      <div className="page-head">
        <h2>Orders Management</h2>
        <p>View and manage customer orders</p>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {flash && <div className="alert alert-success mb-4">{flash}</div>}

      {/* Filters */}
      <div className="card mb-4">
        <div className="flex flex-wrap gap-4 items-center">
          <label className="font-medium">Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}
            className="input w-48"
          >
            <option value="">All Statuses</option>
            <option value="PENDING_PAYMENT">Pending Payment</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="SHIPPED">Shipped</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="REFUNDED">Refunded</option>
          </select>

          <form onSubmit={handleSearch} className="flex gap-2 ml-auto">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search order # or customer..."
              className="input w-64"
            />
            <button type="submit" className="btn btn-primary">Search</button>
          </form>
        </div>
      </div>

      {/* Orders Table */}
      <div className="card">
        <table className="table w-full">
          <thead>
            <tr>
              <th>Order #</th>
              <th>Customer</th>
              <th>Items</th>
              <th>Total</th>
              <th>Status</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td>
                  <Link
                    to={`/admin/orders/${order.id}`}
                    className="font-mono text-blue-600 hover:underline"
                  >
                    #{order.id}
                  </Link>
                </td>
                <td>
                  <div>{order.customerName || '-'}</div>
                  <div className="text-sm text-gray-500">{order.customerEmail}</div>
                </td>
                <td>{order.itemCount} item(s)</td>
                <td className="font-medium">${order.totalAmount?.toFixed(2) || '0.00'}</td>
                <td>
                  <span className={`px-2 py-1 rounded text-xs font-medium ${STATUS_COLORS[order.status] || 'bg-gray-100'}`}>
                    {order.status?.replace(/_/g, ' ')}
                  </span>
                </td>
                <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                <td>
                  <div className="flex gap-2">
                    <Link
                      to={`/admin/orders/${order.id}`}
                      className="btn btn-sm"
                    >
                      View
                    </Link>
                    {['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING'].includes(order.status) && (
                      <button
                        onClick={() => handleCancel(order.id)}
                        className="btn btn-sm btn-danger"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td colSpan="7" className="text-center text-gray-500 py-8">
                  No orders found
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-4">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="btn btn-sm"
            >
              Previous
            </button>
            <span className="self-center">
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="btn btn-sm"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
