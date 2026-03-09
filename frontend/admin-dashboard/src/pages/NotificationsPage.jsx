import { useEffect, useState } from 'react'
import {
  broadcastNotification,
  getUnreadCount,
  listMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  pushNotificationToUser
} from '../shared/api/endpoints/notificationsApi'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const CATEGORIES = ['ORDER', 'SYSTEM', 'STORE', 'AI', 'SECURITY']

const DEFAULT_SEND_FORM = {
  targetUserId: '',
  category: 'SYSTEM',
  title: '',
  body: ''
}

const DEFAULT_BROADCAST_FORM = {
  category: 'SYSTEM',
  title: '',
  body: ''
}

export function NotificationsPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [unreadCount, setUnreadCount] = useState(0)
  const [notifications, setNotifications] = useState([])

  const [sendForm, setSendForm] = useState(DEFAULT_SEND_FORM)
  const [broadcastForm, setBroadcastForm] = useState(DEFAULT_BROADCAST_FORM)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [count, list] = await Promise.all([getUnreadCount(), listMyNotifications()])
      setUnreadCount(Number(count || 0))
      setNotifications(list || [])
    } catch (err) {
      setError(err.message || 'Failed to load notifications.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleMarkRead(notificationId) {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const updated = await markNotificationRead(notificationId)
      setNotifications((current) => current.map((item) => (String(item.id) === String(notificationId) ? updated : item)))
      setUnreadCount((current) => Math.max(0, Number(current || 0) - 1))
    } catch (err) {
      setError(err.message || 'Unable to mark as read.')
    } finally {
      setSaving(false)
    }
  }

  async function handleMarkAllRead() {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await markAllNotificationsRead()
      setFlash('All notifications marked as read.')
      await load()
    } catch (err) {
      setError(err.message || 'Unable to mark all as read.')
    } finally {
      setSaving(false)
    }
  }

  async function handleSendToUser(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await pushNotificationToUser(sendForm.targetUserId.trim(), {
        targetUserId: sendForm.targetUserId.trim(),
        category: sendForm.category,
        title: sendForm.title.trim(),
        body: sendForm.body.trim()
      })
      setFlash('Notification sent.')
      setSendForm(DEFAULT_SEND_FORM)
    } catch (err) {
      setError(err.message || 'Unable to send notification.')
    } finally {
      setSaving(false)
    }
  }

  async function handleBroadcast(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await broadcastNotification({
        category: broadcastForm.category,
        title: broadcastForm.title.trim(),
        body: broadcastForm.body.trim()
      })
      setFlash('Broadcast sent.')
      setBroadcastForm(DEFAULT_BROADCAST_FORM)
    } catch (err) {
      setError(err.message || 'Unable to broadcast notification.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading notifications..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Notifications</h2>
        <p>Review your notifications and push messages to users or broadcast system messages.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>My inbox</h3>
              <p>
                Unread: <strong>{unreadCount}</strong>
              </p>
            </div>
            <div className="inline-actions">
              <button className="btn btn-outline btn-sm" onClick={load} disabled={saving}>
                Refresh
              </button>
              <button className="btn btn-outline btn-sm" onClick={handleMarkAllRead} disabled={saving || !notifications.length}>
                Mark all read
              </button>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Created</th>
                  <th>Category</th>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {notifications.length ? (
                  notifications.map((item) => (
                    <tr key={item.id}>
                      <td>{formatDateTime(item.createdAt)}</td>
                      <td className="mono">{item.category}</td>
                      <td>
                        <strong>{item.title}</strong>
                        <div className="subtle-meta">{item.body}</div>
                      </td>
                      <td>
                        <span className={`badge ${item.read ? 'badge-muted' : 'badge-warning'}`}>
                          {item.read ? 'Read' : 'Unread'}
                        </span>
                      </td>
                      <td>
                        {!item.read ? (
                          <button className="btn btn-outline btn-sm" onClick={() => handleMarkRead(item.id)} disabled={saving}>
                            Mark read
                          </button>
                        ) : (
                          <span className="subtle-meta">-</span>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-row">No notifications.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Send to user</h3>
              <p>Push a notification to a specific user id.</p>
            </div>
          </div>

          <form onSubmit={handleSendToUser}>
            <div className="form-grid">
              <label className="span-2">
                Target user id
                <input value={sendForm.targetUserId} onChange={(event) => setSendForm((c) => ({ ...c, targetUserId: event.target.value }))} required />
              </label>
              <label>
                Category
                <select value={sendForm.category} onChange={(event) => setSendForm((c) => ({ ...c, category: event.target.value }))}>
                  {CATEGORIES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </label>
              <label className="span-2">
                Title
                <input value={sendForm.title} onChange={(event) => setSendForm((c) => ({ ...c, title: event.target.value }))} required />
              </label>
              <label className="span-2">
                Body
                <textarea rows="5" value={sendForm.body} onChange={(event) => setSendForm((c) => ({ ...c, body: event.target.value }))} required />
              </label>
            </div>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={saving || !sendForm.targetUserId.trim()}>
                {saving ? 'Sending...' : 'Send'}
              </button>
            </div>
          </form>

          <div className="divider" />

          <div className="section-head">
            <div>
              <h3>Broadcast</h3>
              <p>Send a notification to all users.</p>
            </div>
          </div>

          <form onSubmit={handleBroadcast}>
            <div className="form-grid">
              <label>
                Category
                <select value={broadcastForm.category} onChange={(event) => setBroadcastForm((c) => ({ ...c, category: event.target.value }))}>
                  {CATEGORIES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </label>
              <label className="span-2">
                Title
                <input value={broadcastForm.title} onChange={(event) => setBroadcastForm((c) => ({ ...c, title: event.target.value }))} required />
              </label>
              <label className="span-2">
                Body
                <textarea rows="5" value={broadcastForm.body} onChange={(event) => setBroadcastForm((c) => ({ ...c, body: event.target.value }))} required />
              </label>
            </div>
            <div className="inline-actions">
              <button className="btn btn-outline" type="submit" disabled={saving || !broadcastForm.title.trim()}>
                {saving ? 'Sending...' : 'Broadcast'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}

