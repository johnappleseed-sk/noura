import { useEffect, useMemo, useState } from 'react'
import {
  createWebhookSubscription,
  deleteWebhookSubscription,
  listWebhookSubscriptions,
  updateWebhookSubscription
} from '../shared/api/endpoints/webhooksApi'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'

const EVENT_SUGGESTIONS = [
  { code: 'stock.changed', label: 'Stock changed (any movement)' },
  { code: 'stock.low', label: 'Low stock alert opened' },
  { code: 'stock.low.resolved', label: 'Low stock alert resolved' }
]

const DEFAULT_FORM = {
  eventCode: 'stock.changed',
  endpointUrl: '',
  secretToken: '',
  clearSecretToken: false,
  active: true,
  timeoutMs: '5000',
  retryCount: '3'
}

function subscriptionLabel(subscription) {
  if (!subscription) return ''
  return `${subscription.eventCode} · ${subscription.endpointUrl}`
}

export function WebhooksPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState('')
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [subscriptions, setSubscriptions] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [form, setForm] = useState(DEFAULT_FORM)

  const selected = useMemo(
    () => subscriptions.find((item) => item.id === selectedId) || null,
    [subscriptions, selectedId]
  )

  async function load(nextSelectedId = selectedId) {
    setLoading(true)
    setError('')
    try {
      const data = await listWebhookSubscriptions()
      const sorted = Array.isArray(data)
        ? data.slice().sort((a, b) => {
          const eventCmp = String(a.eventCode || '').localeCompare(String(b.eventCode || ''))
          if (eventCmp !== 0) return eventCmp
          return String(a.endpointUrl || '').localeCompare(String(b.endpointUrl || ''))
        })
        : []

      const resolvedId =
        nextSelectedId && sorted.some((item) => item.id === nextSelectedId)
          ? nextSelectedId
          : sorted[0]?.id || ''

      setSubscriptions(sorted)
      setSelectedId(resolvedId)

      if (!resolvedId) {
        setForm(DEFAULT_FORM)
      }
    } catch (err) {
      setError(err.message || 'Failed to load webhook subscriptions.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  useEffect(() => {
    if (!selected) return
    setForm({
      eventCode: selected.eventCode || DEFAULT_FORM.eventCode,
      endpointUrl: selected.endpointUrl || '',
      secretToken: '',
      clearSecretToken: false,
      active: Boolean(selected.active),
      timeoutMs: String(selected.timeoutMs ?? 5000),
      retryCount: String(selected.retryCount ?? 3)
    })
  }, [selected])

  function handleSelect(subscription) {
    setFlash('')
    setError('')
    setSelectedId(subscription.id)
  }

  function handleCreateNew() {
    setFlash('')
    setError('')
    setSelectedId('')
    setForm(DEFAULT_FORM)
  }

  async function handleSave(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const payload = {
        eventCode: form.eventCode.trim(),
        endpointUrl: form.endpointUrl.trim(),
        active: Boolean(form.active),
        timeoutMs: Number(form.timeoutMs || 5000),
        retryCount: Number(form.retryCount || 3)
      }

      const trimmedSecret = form.secretToken.trim()
      if (trimmedSecret) {
        payload.secretToken = trimmedSecret
      } else if (selectedId && form.clearSecretToken) {
        payload.secretToken = ''
      }

      const saved = selectedId
        ? await updateWebhookSubscription(selectedId, payload)
        : await createWebhookSubscription(payload)

      setFlash(selectedId ? 'Webhook subscription updated.' : 'Webhook subscription created.')
      setForm(DEFAULT_FORM)
      await load(saved?.id || selectedId)
    } catch (err) {
      setError(err.message || 'Failed to save webhook subscription.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!selectedId) return
    if (!window.confirm(`Delete this webhook?\n\n${subscriptionLabel(selected)}`)) return
    setDeletingId(selectedId)
    setFlash('')
    setError('')
    try {
      await deleteWebhookSubscription(selectedId)
      setFlash('Webhook subscription deleted.')
      setSelectedId('')
      setForm(DEFAULT_FORM)
      await load('')
    } catch (err) {
      setError(err.message || 'Failed to delete webhook subscription.')
    } finally {
      setDeletingId('')
    }
  }

  if (loading) {
    return <Spinner label="Loading webhooks..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Webhook subscriptions</h2>
        <p>Dispatch inventory change notifications to external systems. Only admins can create or modify subscriptions.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="workspace-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Subscriptions</h3>
              <p>{subscriptions.length ? `${subscriptions.length} configured` : 'No subscriptions yet.'}</p>
            </div>
            <div className="inline-actions">
              <button className="btn btn-outline btn-sm" type="button" onClick={() => load()} disabled={saving || deletingId}>
                Refresh
              </button>
              <button className="btn btn-primary btn-sm" type="button" onClick={handleCreateNew} disabled={saving || deletingId}>
                New
              </button>
            </div>
          </div>

          <div className="selection-list" role="list">
            {subscriptions.length ? (
              subscriptions.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`selection-item${item.id === selectedId ? ' active' : ''}`}
                  onClick={() => handleSelect(item)}
                >
                  <span>
                    <strong className="mono">{item.eventCode}</strong>
                    <small>{item.endpointUrl}</small>
                    <small className="subtle-meta">
                      Updated {formatDateTime(item.updatedAt)} · {item.active ? 'Active' : 'Disabled'}
                    </small>
                  </span>
                  <span className={`badge ${item.active ? 'badge-success' : 'badge-muted'}`}>{item.active ? 'ON' : 'OFF'}</span>
                </button>
              ))
            ) : (
              <p className="empty-copy">Create your first subscription to start receiving inventory events.</p>
            )}
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{selectedId ? 'Edit subscription' : 'Create subscription'}</h3>
              <p>{selectedId ? 'Update endpoint settings or rotate the secret token.' : 'Point an event code to a destination URL.'}</p>
            </div>
            {selectedId ? (
              <span className="badge badge-muted mono" title={selectedId}>
                {selectedId.slice(0, 8)}
              </span>
            ) : null}
          </div>

          {selected ? (
            <p className="subtle-meta">
              Created {formatDateTime(selected.createdAt)} · Updated {formatDateTime(selected.updatedAt)}
            </p>
          ) : null}

          <form className="stack-form" onSubmit={handleSave}>
            <label>
              Event code
              <input
                list="webhook-events"
                value={form.eventCode}
                onChange={(event) => setForm((current) => ({ ...current, eventCode: event.target.value }))}
                placeholder="stock.changed"
                required
              />
              <datalist id="webhook-events">
                {EVENT_SUGGESTIONS.map((item) => (
                  <option key={item.code} value={item.code}>
                    {item.label}
                  </option>
                ))}
              </datalist>
            </label>

            <label className="grow">
              Endpoint URL
              <input
                value={form.endpointUrl}
                onChange={(event) => setForm((current) => ({ ...current, endpointUrl: event.target.value }))}
                placeholder="https://example.com/webhooks/inventory"
                required
              />
            </label>

            <label>
              Timeout (ms)
              <input
                type="number"
                min="1000"
                max="60000"
                value={form.timeoutMs}
                onChange={(event) => setForm((current) => ({ ...current, timeoutMs: event.target.value }))}
              />
            </label>

            <label>
              Retries
              <input
                type="number"
                min="0"
                max="10"
                value={form.retryCount}
                onChange={(event) => setForm((current) => ({ ...current, retryCount: event.target.value }))}
              />
            </label>

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
              />
              Active
            </label>

            <label className="grow">
              Secret token (optional)
              <input
                type="password"
                value={form.secretToken}
                onChange={(event) => setForm((current) => ({ ...current, secretToken: event.target.value }))}
                placeholder={selectedId ? 'Leave blank to keep the current secret' : 'Optional shared secret'}
                disabled={Boolean(selectedId && form.clearSecretToken)}
              />
              <span className="subtle-meta">
                Secret tokens are write-only. For existing subscriptions, leaving this blank keeps the current token unless you clear it.
              </span>
            </label>

            {selectedId ? (
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={form.clearSecretToken}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      clearSecretToken: event.target.checked,
                      secretToken: event.target.checked ? '' : current.secretToken
                    }))
                  }
                />
                Clear secret token
              </label>
            ) : null}

            <div className="inline-actions wrap">
              <button className="btn btn-primary" type="submit" disabled={saving || deletingId}>
                {saving ? 'Saving...' : selectedId ? 'Save changes' : 'Create webhook'}
              </button>
              {selectedId ? (
                <button className="btn btn-danger" type="button" onClick={handleDelete} disabled={saving || deletingId}>
                  {deletingId ? 'Deleting...' : 'Delete'}
                </button>
              ) : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}
