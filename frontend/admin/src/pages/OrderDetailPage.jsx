import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getOrder,
  updateOrderStatus,
  cancelOrder,
  markShipped,
  markDelivered,
  addOrderNote,
  refundOrder
} from '../shared/api/endpoints/ordersApi'
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

export function OrderDetailPage() {
  const { id } = useParams()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [order, setOrder] = useState(null)
  const [note, setNote] = useState('')
  const [showShipModal, setShowShipModal] = useState(false)
  const [shipData, setShipData] = useState({ carrier: '', trackingNumber: '' })

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getOrder(id)
      setOrder(result)
    } catch (err) {
      setError(err.message || 'Failed to load order details.')
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

  const handleShip = async (e) => {
    e.preventDefault()
    await handleAction(markShipped, shipData)
    setShowShipModal(false)
    setShipData({ carrier: '', trackingNumber: '' })
  }

  const handleAddNote = async (e) => {
    e.preventDefault()
    if (!note.trim()) return
    await handleAction(addOrderNote, note)
    setNote('')
  }

  if (loading) return <Spinner label="Loading order details..." />
  if (!order) return <div className="alert alert-error">{error || 'Order not found'}</div>

  return (
    <div className="page">
      <div className="page-head">
        <div className="flex justify-between items-start">
          <div>
            <Link to="/admin/orders" className="text-blue-600 hover:underline text-sm mb-2 block">
              &larr; Back to Orders
            </Link>
            <h2 className="flex items-center gap-3">
              Order #{order.id}
              <span className={`px-3 py-1 rounded text-sm font-medium ${STATUS_COLORS[order.status] || 'bg-gray-100'}`}>
                {order.status?.replace(/_/g, ' ')}
              </span>
            </h2>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2">
            {order.status === 'CONFIRMED' && (
              <button
                onClick={() => handleAction(updateOrderStatus, 'PROCESSING')}
                className="btn btn-primary"
              >
                Start Processing
              </button>
            )}
            {order.status === 'PROCESSING' && (
              <button
                onClick={() => setShowShipModal(true)}
                className="btn btn-primary"
              >
                Mark Shipped
              </button>
            )}
            {order.status === 'SHIPPED' && (
              <button
                onClick={() => handleAction(markDelivered)}
                className="btn btn-success"
              >
                Mark Delivered
              </button>
            )}
            {['CONFIRMED', 'PROCESSING'].includes(order.status) && (
              <button
                onClick={() => {
                  const reason = prompt('Enter cancellation reason:')
                  if (reason) handleAction(cancelOrder, reason)
                }}
                className="btn btn-danger"
              >
                Cancel Order
              </button>
            )}
            {order.status === 'DELIVERED' && !order.refunded && (
              <button
                onClick={() => {
                  if (confirm('Process full refund?')) {
                    handleAction(refundOrder, { amount: order.totalAmount })
                  }
                }}
                className="btn btn-warning"
              >
                Refund
              </button>
            )}
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {flash && <div className="alert alert-success mb-4">{flash}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Order Items */}
          <div className="card">
            <h3 className="font-semibold mb-4">Order Items</h3>
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Price</th>
                  <th>Qty</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {(order.items || []).map((item, idx) => (
                  <tr key={idx}>
                    <td>
                      <div>{item.productName}</div>
                      {item.variantName && <div className="text-sm text-gray-500">{item.variantName}</div>}
                    </td>
                    <td className="font-mono text-sm">{item.sku}</td>
                    <td>${item.unitPrice?.toFixed(2)}</td>
                    <td>{item.quantity}</td>
                    <td className="font-medium">${item.lineTotal?.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan="4" className="text-right font-medium">Subtotal:</td>
                  <td className="font-medium">${order.subtotal?.toFixed(2)}</td>
                </tr>
                {order.discountAmount > 0 && (
                  <tr>
                    <td colSpan="4" className="text-right text-green-600">Discount:</td>
                    <td className="text-green-600">-${order.discountAmount?.toFixed(2)}</td>
                  </tr>
                )}
                <tr>
                  <td colSpan="4" className="text-right">Shipping:</td>
                  <td>${order.shippingAmount?.toFixed(2) || '0.00'}</td>
                </tr>
                <tr>
                  <td colSpan="4" className="text-right">Tax:</td>
                  <td>${order.taxAmount?.toFixed(2) || '0.00'}</td>
                </tr>
                <tr className="border-t">
                  <td colSpan="4" className="text-right font-bold text-lg">Total:</td>
                  <td className="font-bold text-lg">${order.totalAmount?.toFixed(2)}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Shipping Info */}
          {order.status === 'SHIPPED' && order.trackingNumber && (
            <div className="card">
              <h3 className="font-semibold mb-4">Shipping Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-gray-500">Carrier:</span>
                  <div className="font-medium">{order.shippingCarrier || '-'}</div>
                </div>
                <div>
                  <span className="text-gray-500">Tracking Number:</span>
                  <div className="font-mono">{order.trackingNumber}</div>
                </div>
              </div>
            </div>
          )}

          {/* Notes */}
          <div className="card">
            <h3 className="font-semibold mb-4">Order Notes</h3>

            <form onSubmit={handleAddNote} className="mb-4">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Add a note..."
                  className="input flex-1"
                />
                <button type="submit" className="btn btn-primary">Add Note</button>
              </div>
            </form>

            <div className="space-y-3">
              {(order.notes || []).map((n, idx) => (
                <div key={idx} className="border-l-2 border-gray-300 pl-4 py-2">
                  <div className="text-sm text-gray-500">
                    {n.createdBy} &bull; {new Date(n.createdAt).toLocaleString()}
                  </div>
                  <div>{n.content}</div>
                </div>
              ))}
              {(!order.notes || order.notes.length === 0) && (
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
              <div><span className="text-gray-500">Name:</span> {order.customerName}</div>
              <div><span className="text-gray-500">Email:</span> {order.customerEmail}</div>
              <div><span className="text-gray-500">Phone:</span> {order.customerPhone || '-'}</div>
            </div>
          </div>

          {/* Shipping Address */}
          <div className="card">
            <h3 className="font-semibold mb-4">Shipping Address</h3>
            <address className="text-sm not-italic">
              {order.shippingAddress?.name}<br />
              {order.shippingAddress?.line1}<br />
              {order.shippingAddress?.line2 && <>{order.shippingAddress.line2}<br /></>}
              {order.shippingAddress?.city}, {order.shippingAddress?.state} {order.shippingAddress?.postalCode}<br />
              {order.shippingAddress?.country}
            </address>
          </div>

          {/* Billing Address */}
          <div className="card">
            <h3 className="font-semibold mb-4">Billing Address</h3>
            <address className="text-sm not-italic">
              {order.billingAddress?.name}<br />
              {order.billingAddress?.line1}<br />
              {order.billingAddress?.line2 && <>{order.billingAddress.line2}<br /></>}
              {order.billingAddress?.city}, {order.billingAddress?.state} {order.billingAddress?.postalCode}<br />
              {order.billingAddress?.country}
            </address>
          </div>

          {/* Payment Info */}
          <div className="card">
            <h3 className="font-semibold mb-4">Payment</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-gray-500">Method:</span> {order.paymentMethod || '-'}</div>
              <div><span className="text-gray-500">Status:</span> {order.paymentStatus}</div>
              {order.paymentReference && (
                <div><span className="text-gray-500">Reference:</span> {order.paymentReference}</div>
              )}
            </div>
          </div>

          {/* Timestamps */}
          <div className="card">
            <h3 className="font-semibold mb-4">Timeline</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-gray-500">Created:</span> {new Date(order.createdAt).toLocaleString()}</div>
              {order.confirmedAt && <div><span className="text-gray-500">Confirmed:</span> {new Date(order.confirmedAt).toLocaleString()}</div>}
              {order.shippedAt && <div><span className="text-gray-500">Shipped:</span> {new Date(order.shippedAt).toLocaleString()}</div>}
              {order.deliveredAt && <div><span className="text-gray-500">Delivered:</span> {new Date(order.deliveredAt).toLocaleString()}</div>}
            </div>
          </div>
        </div>
      </div>

      {/* Ship Modal */}
      {showShipModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-96">
            <h3 className="font-semibold text-lg mb-4">Mark as Shipped</h3>
            <form onSubmit={handleShip}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Carrier</label>
                  <select
                    value={shipData.carrier}
                    onChange={(e) => setShipData(s => ({ ...s, carrier: e.target.value }))}
                    className="input w-full"
                    required
                  >
                    <option value="">Select carrier...</option>
                    <option value="fedex">FedEx</option>
                    <option value="ups">UPS</option>
                    <option value="usps">USPS</option>
                    <option value="dhl">DHL</option>
                    <option value="other">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Tracking Number</label>
                  <input
                    type="text"
                    value={shipData.trackingNumber}
                    onChange={(e) => setShipData(s => ({ ...s, trackingNumber: e.target.value }))}
                    className="input w-full"
                    required
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <button
                  type="button"
                  onClick={() => setShowShipModal(false)}
                  className="btn"
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Mark Shipped
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
