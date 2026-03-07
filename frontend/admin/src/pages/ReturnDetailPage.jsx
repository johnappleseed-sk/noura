import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import {
  getReturn,
  approveReturn,
  rejectReturn,
  receiveReturn,
  processRefund,
  completeReturn,
  addReturnNote
} from '../shared/api/endpoints/returnsApi'
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

export function ReturnDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [returnData, setReturnData] = useState(null)
  const [note, setNote] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getReturn(id)
      setReturnData(result)
    } catch (err) {
      setError(err.message || 'Failed to load return details.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [id])

  const handleAction = async (action, data = {}) => {
    setError('')
    setFlash('')
    try {
      await action(id, data)
      setFlash('Action completed successfully.')
      await load()
    } catch (err) {
      setError(err.message || 'Action failed.')
    }
  }

  const handleAddNote = async (e) => {
    e.preventDefault()
    if (!note.trim()) return
    await handleAction(addReturnNote, note)
    setNote('')
  }

  if (loading) return <Spinner label="Loading return details..." />
  if (!returnData) return <div className="alert alert-error">{error || 'Return not found'}</div>

  const ret = returnData

  return (
    <div className="page">
      <div className="page-head">
        <div className="flex justify-between items-start">
          <div>
            <Link to="/admin/returns" className="text-blue-600 hover:underline text-sm mb-2 block">
              &larr; Back to Returns
            </Link>
            <h2 className="flex items-center gap-3">
              RMA-{String(ret.id).padStart(6, '0')}
              <span className={`px-3 py-1 rounded text-sm font-medium ${STATUS_COLORS[ret.status] || 'bg-gray-100'}`}>
                {ret.status}
              </span>
            </h2>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2">
            {ret.status === 'REQUESTED' && (
              <>
                <button
                  onClick={() => handleAction(approveReturn)}
                  className="btn btn-success"
                >
                  Approve
                </button>
                <button
                  onClick={() => {
                    const reason = prompt('Enter rejection reason:')
                    if (reason) handleAction(rejectReturn, { reason })
                  }}
                  className="btn btn-danger"
                >
                  Reject
                </button>
              </>
            )}
            {ret.status === 'APPROVED' && (
              <button
                onClick={() => handleAction(receiveReturn)}
                className="btn btn-primary"
              >
                Mark Items Received
              </button>
            )}
            {ret.status === 'ITEMS_RECEIVED' && (
              <button
                onClick={() => handleAction(processRefund)}
                className="btn btn-primary"
              >
                Process Refund
              </button>
            )}
            {ret.status === 'REFUND_PENDING' && (
              <button
                onClick={() => handleAction(completeReturn)}
                className="btn btn-success"
              >
                Complete Return
              </button>
            )}
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {flash && <div className="alert alert-success mb-4">{flash}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Return Items */}
          <div className="card">
            <h3 className="font-semibold mb-4">Return Items</h3>
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Qty</th>
                  <th>Reason</th>
                  <th>Condition</th>
                </tr>
              </thead>
              <tbody>
                {(ret.items || []).map((item, idx) => (
                  <tr key={idx}>
                    <td>{item.productName}</td>
                    <td className="font-mono text-sm">{item.sku}</td>
                    <td>{item.quantity}</td>
                    <td>{item.reason}</td>
                    <td>
                      <span className={`text-xs px-2 py-1 rounded ${
                        item.condition === 'NEW' ? 'bg-green-100 text-green-800' :
                        item.condition === 'GOOD' ? 'bg-blue-100 text-blue-800' :
                        item.condition === 'DAMAGED' ? 'bg-red-100 text-red-800' :
                        'bg-gray-100'
                      }`}>
                        {item.condition || 'N/A'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Customer Reason */}
          <div className="card">
            <h3 className="font-semibold mb-4">Customer's Reason</h3>
            <p className="text-gray-700">{ret.reason || 'No reason provided'}</p>
          </div>

          {/* Notes & Timeline */}
          <div className="card">
            <h3 className="font-semibold mb-4">Notes & Timeline</h3>

            <form onSubmit={handleAddNote} className="mb-4">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Add a note..."
                  className="input flex-1"
                />
                <button type="submit" className="btn btn-primary">
                  Add Note
                </button>
              </div>
            </form>

            <div className="space-y-3">
              {(ret.notes || []).map((n, idx) => (
                <div key={idx} className="border-l-2 border-gray-300 pl-4 py-2">
                  <div className="text-sm text-gray-500">
                    {n.createdBy} &bull; {new Date(n.createdAt).toLocaleString()}
                  </div>
                  <div>{n.content}</div>
                </div>
              ))}
              {(!ret.notes || ret.notes.length === 0) && (
                <p className="text-gray-500">No notes yet</p>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Customer Info */}
          <div className="card">
            <h3 className="font-semibold mb-4">Customer</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-gray-500">Name:</span> {ret.customerName}</div>
              <div><span className="text-gray-500">Email:</span> {ret.customerEmail}</div>
              <div><span className="text-gray-500">Phone:</span> {ret.customerPhone || '-'}</div>
            </div>
          </div>

          {/* Order Info */}
          <div className="card">
            <h3 className="font-semibold mb-4">Original Order</h3>
            <div className="space-y-2 text-sm">
              <div>
                <span className="text-gray-500">Order #:</span>{' '}
                <Link to={`/admin/orders/${ret.orderId}`} className="text-blue-600 hover:underline">
                  {ret.orderId}
                </Link>
              </div>
              <div><span className="text-gray-500">Date:</span> {ret.orderDate ? new Date(ret.orderDate).toLocaleDateString() : '-'}</div>
              <div><span className="text-gray-500">Total:</span> ${ret.orderTotal?.toFixed(2) || '0.00'}</div>
            </div>
          </div>

          {/* Refund Info */}
          <div className="card">
            <h3 className="font-semibold mb-4">Refund Details</h3>
            <div className="space-y-2 text-sm">
              <div>
                <span className="text-gray-500">Refund Amount:</span>{' '}
                <span className="font-semibold text-lg">${ret.refundAmount?.toFixed(2) || '0.00'}</span>
              </div>
              <div>
                <span className="text-gray-500">Refund Method:</span> {ret.refundMethod || 'Original payment method'}
              </div>
              {ret.refundedAt && (
                <div><span className="text-gray-500">Refunded:</span> {new Date(ret.refundedAt).toLocaleDateString()}</div>
              )}
            </div>
          </div>

          {/* Dates */}
          <div className="card">
            <h3 className="font-semibold mb-4">Timeline</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-gray-500">Created:</span> {new Date(ret.createdAt).toLocaleString()}</div>
              {ret.approvedAt && <div><span className="text-gray-500">Approved:</span> {new Date(ret.approvedAt).toLocaleString()}</div>}
              {ret.receivedAt && <div><span className="text-gray-500">Items Received:</span> {new Date(ret.receivedAt).toLocaleString()}</div>}
              {ret.completedAt && <div><span className="text-gray-500">Completed:</span> {new Date(ret.completedAt).toLocaleString()}</div>}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
