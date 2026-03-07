import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listReturns, approveReturn, rejectReturn } from '../shared/api/endpoints/returnsApi'
import { Spinner } from '../shared/ui/Spinner'

const STATUS_COLORS = {
  REQUESTED: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-blue-100 text-blue-800',
  ITEMS_RECEIVED: 'bg-purple-100 text-purple-800',
  REFUND_PENDING: 'bg-orange-100 text-orange-800',
  COMPLETED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-100 text-gray-800'
}

export function ReturnsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [returns, setReturns] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await listReturns({
        status: statusFilter || undefined,
        page,
        size: 20,
        sort: 'createdAt',
        dir: 'desc'
      })
      setReturns(result?.items || [])
      setTotalPages(result?.totalPages || 0)
    } catch (err) {
      setError(err.message || 'Failed to load returns.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [statusFilter, page])

  const handleApprove = async (id) => {
    if (!confirm('Approve this return request?')) return
    setError('')
    setFlash('')
    try {
      await approveReturn(id)
      setFlash('Return approved.')
      await load()
    } catch (err) {
      setError(err.message || 'Failed to approve return.')
    }
  }

  const handleReject = async (id) => {
    const reason = prompt('Enter rejection reason:')
    if (!reason) return
    setError('')
    setFlash('')
    try {
      await rejectReturn(id, { reason })
      setFlash('Return rejected.')
      await load()
    } catch (err) {
      setError(err.message || 'Failed to reject return.')
    }
  }

  if (loading) return <Spinner label="Loading returns..." />

  return (
    <div className="page">
      <div className="page-head">
        <h2>Returns & RMA Management</h2>
        <p>Process customer return requests and refunds</p>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {flash && <div className="alert alert-success mb-4">{flash}</div>}

      {/* Filters */}
      <div className="card mb-4">
        <div className="flex gap-4 items-center">
          <label className="font-medium">Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}
            className="input w-48"
          >
            <option value="">All Statuses</option>
            <option value="REQUESTED">Requested</option>
            <option value="APPROVED">Approved</option>
            <option value="ITEMS_RECEIVED">Items Received</option>
            <option value="REFUND_PENDING">Refund Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      {/* Returns Table */}
      <div className="card">
        <table className="table w-full">
          <thead>
            <tr>
              <th>RMA #</th>
              <th>Order</th>
              <th>Customer</th>
              <th>Items</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {returns.map((ret) => (
              <tr key={ret.id}>
                <td>
                  <Link
                    to={`/admin/returns/${ret.id}`}
                    className="font-mono text-blue-600 hover:underline"
                  >
                    RMA-{String(ret.id).padStart(6, '0')}
                  </Link>
                </td>
                <td>
                  <Link
                    to={`/admin/orders/${ret.orderId}`}
                    className="text-blue-600 hover:underline"
                  >
                    #{ret.orderId}
                  </Link>
                </td>
                <td>{ret.customerName || ret.customerEmail || '-'}</td>
                <td>{ret.itemCount} item(s)</td>
                <td className="max-w-xs truncate">{ret.reason}</td>
                <td>
                  <span className={`px-2 py-1 rounded text-xs font-medium ${STATUS_COLORS[ret.status] || 'bg-gray-100'}`}>
                    {ret.status}
                  </span>
                </td>
                <td>{new Date(ret.createdAt).toLocaleDateString()}</td>
                <td>
                  <div className="flex gap-2">
                    {ret.status === 'REQUESTED' && (
                      <>
                        <button
                          onClick={() => handleApprove(ret.id)}
                          className="btn btn-sm btn-success"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => handleReject(ret.id)}
                          className="btn btn-sm btn-danger"
                        >
                          Reject
                        </button>
                      </>
                    )}
                    <Link
                      to={`/admin/returns/${ret.id}`}
                      className="btn btn-sm"
                    >
                      View
                    </Link>
                  </div>
                </td>
              </tr>
            ))}
            {returns.length === 0 && (
              <tr>
                <td colSpan="8" className="text-center text-gray-500 py-8">
                  No returns found
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
