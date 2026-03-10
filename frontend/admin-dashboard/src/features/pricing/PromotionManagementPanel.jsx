import { useEffect, useState } from 'react'
import {
  createPromotion,
  listPromotions,
  updatePromotion
} from '../../shared/api/endpoints/pricingApi'
import { formatDateTime } from '../../shared/ui/formatters'

const PROMOTION_TYPES = [
  'PERCENTAGE',
  'FIXED',
  'BUY_X_GET_Y',
  'FREE_SHIPPING',
  'CART_THRESHOLD_DISCOUNT',
  'PRODUCT_BUNDLE_DISCOUNT'
]

const APPLICATION_ENTITY_TYPES = ['CATEGORY', 'PRODUCT', 'VARIANT', 'COLLECTION']

const DEFAULT_FORM = {
  name: '',
  code: '',
  description: '',
  type: 'PERCENTAGE',
  couponCode: '',
  conditionsJson: '{"percent":10}',
  startDate: '',
  endDate: '',
  active: true,
  stackable: true,
  priority: '0',
  usageLimitTotal: '',
  usageLimitPerCustomer: '',
  customerSegment: ''
}

function parseJson(text, fallback = {}) {
  const raw = String(text || '').trim()
  if (!raw) return fallback
  return JSON.parse(raw)
}

function toIso(value) {
  const raw = String(value || '').trim()
  if (!raw) return null
  return new Date(raw).toISOString()
}

export function PromotionManagementPanel() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [promotions, setPromotions] = useState([])
  const [filters, setFilters] = useState({ query: '', active: '', archived: '' })
  const [form, setForm] = useState(DEFAULT_FORM)
  const [applications, setApplications] = useState([{ applicableEntityType: 'PRODUCT', applicableEntityId: '' }])

  async function load(nextFilters = filters) {
    setLoading(true)
    setError('')
    try {
      const data = await listPromotions({
        query: nextFilters.query || undefined,
        active: nextFilters.active === '' ? undefined : nextFilters.active === 'true',
        archived: nextFilters.archived === '' ? undefined : nextFilters.archived === 'true'
      })
      setPromotions(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Unable to load promotions.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleCreate(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await createPromotion({
        name: form.name.trim(),
        code: form.code.trim() || null,
        description: form.description.trim() || null,
        type: form.type,
        couponCode: form.couponCode.trim() || null,
        conditions: parseJson(form.conditionsJson, {}),
        startDate: toIso(form.startDate),
        endDate: toIso(form.endDate),
        active: form.active,
        stackable: form.stackable,
        priority: Number(form.priority || 0),
        usageLimitTotal: form.usageLimitTotal ? Number(form.usageLimitTotal) : null,
        usageLimitPerCustomer: form.usageLimitPerCustomer ? Number(form.usageLimitPerCustomer) : null,
        customerSegment: form.customerSegment.trim() || null,
        applications: applications
          .map((item) => ({ ...item, applicableEntityId: item.applicableEntityId.trim() }))
          .filter((item) => item.applicableEntityId)
      })
      setFlash('Promotion created.')
      setForm(DEFAULT_FORM)
      setApplications([{ applicableEntityType: 'PRODUCT', applicableEntityId: '' }])
      await load()
    } catch (err) {
      setError(err.message || 'Unable to create promotion.')
    } finally {
      setSaving(false)
    }
  }

  async function togglePromotion(promotion, patch) {
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await updatePromotion(promotion.id, {
        name: promotion.name,
        code: promotion.code,
        description: promotion.description,
        type: promotion.type,
        couponCode: promotion.couponCode,
        conditions: promotion.conditions || {},
        startDate: promotion.startDate,
        endDate: promotion.endDate,
        active: patch.active ?? promotion.active,
        stackable: promotion.stackable,
        priority: promotion.priority,
        usageLimitTotal: promotion.usageLimitTotal,
        usageLimitPerCustomer: promotion.usageLimitPerCustomer,
        customerSegment: promotion.customerSegment,
        archived: patch.archived ?? promotion.archived,
        applications: promotion.applications || []
      })
      await load()
    } catch (err) {
      setError(err.message || 'Unable to update promotion.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h3>Enterprise promotions</h3>
          <p>Configure rule-based promotions, coupon-linked offers, usage limits, and archival state from one management panel.</p>
        </div>
        <button className="btn btn-outline btn-sm" type="button" onClick={() => load()} disabled={loading}>
          Refresh
        </button>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <form className="filters four-up" onSubmit={(event) => { event.preventDefault(); load(filters) }} style={{ marginBottom: 24 }}>
        <label>
          Search
          <input value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))} placeholder="Name or code" />
        </label>
        <label>
          Active
          <select value={filters.active} onChange={(event) => setFilters((current) => ({ ...current, active: event.target.value }))}>
            <option value="">All</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </label>
        <label>
          Archived
          <select value={filters.archived} onChange={(event) => setFilters((current) => ({ ...current, archived: event.target.value }))}>
            <option value="">All</option>
            <option value="false">Live</option>
            <option value="true">Archived</option>
          </select>
        </label>
        <div className="inline-actions">
          <button className="btn btn-primary" type="submit">Apply</button>
        </div>
      </form>

      <div className="table-card" style={{ marginBottom: 28 }}>
        <table className="data-table compact">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Code</th>
              <th>Window</th>
              <th>Usage</th>
              <th>Status</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {promotions.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-copy">No promotions found.</td>
              </tr>
            ) : promotions.map((promotion) => (
              <tr key={promotion.id}>
                <td>
                  <strong>{promotion.name}</strong>
                  {promotion.description ? <div className="muted-text">{promotion.description}</div> : null}
                </td>
                <td>{promotion.type}</td>
                <td>{promotion.code || promotion.couponCode || '—'}</td>
                <td>
                  <div>{promotion.startDate ? formatDateTime(promotion.startDate) : 'Now'}</div>
                  <div className="muted-text">{promotion.endDate ? formatDateTime(promotion.endDate) : 'No expiry'}</div>
                </td>
                <td>
                  {promotion.usageCount || 0}
                  {promotion.usageLimitTotal ? ` / ${promotion.usageLimitTotal}` : ''}
                </td>
                <td>
                  <span className={`status-badge ${promotion.archived ? 'neutral' : promotion.active ? 'success' : 'warning'}`}>
                    {promotion.archived ? 'Archived' : promotion.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td style={{ textAlign: 'right' }}>
                  <div className="inline-actions" style={{ justifyContent: 'flex-end' }}>
                    <button className="btn btn-outline btn-sm" type="button" disabled={saving} onClick={() => togglePromotion(promotion, { active: !promotion.active })}>
                      {promotion.active ? 'Deactivate' : 'Activate'}
                    </button>
                    <button className="btn btn-outline btn-sm" type="button" disabled={saving} onClick={() => togglePromotion(promotion, { archived: !promotion.archived, active: promotion.archived ? promotion.active : false })}>
                      {promotion.archived ? 'Restore' : 'Archive'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className="filters four-up" onSubmit={handleCreate}>
        <label>
          Name
          <input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required />
        </label>
        <label>
          Code
          <input value={form.code} onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))} placeholder="SPRING25" />
        </label>
        <label>
          Type
          <select value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}>
            {PROMOTION_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
          </select>
        </label>
        <label>
          Coupon code
          <input value={form.couponCode} onChange={(event) => setForm((current) => ({ ...current, couponCode: event.target.value }))} />
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          Description
          <textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} rows={2} />
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          Conditions JSON
          <textarea value={form.conditionsJson} onChange={(event) => setForm((current) => ({ ...current, conditionsJson: event.target.value }))} rows={4} spellCheck="false" />
        </label>
        <label>
          Start date
          <input type="datetime-local" value={form.startDate} onChange={(event) => setForm((current) => ({ ...current, startDate: event.target.value }))} />
        </label>
        <label>
          End date
          <input type="datetime-local" value={form.endDate} onChange={(event) => setForm((current) => ({ ...current, endDate: event.target.value }))} />
        </label>
        <label>
          Priority
          <input type="number" value={form.priority} onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))} />
        </label>
        <label>
          Customer segment
          <input value={form.customerSegment} onChange={(event) => setForm((current) => ({ ...current, customerSegment: event.target.value }))} placeholder="VIP" />
        </label>
        <label>
          Usage limit total
          <input type="number" value={form.usageLimitTotal} onChange={(event) => setForm((current) => ({ ...current, usageLimitTotal: event.target.value }))} />
        </label>
        <label>
          Usage limit per customer
          <input type="number" value={form.usageLimitPerCustomer} onChange={(event) => setForm((current) => ({ ...current, usageLimitPerCustomer: event.target.value }))} />
        </label>
        <label className="checkbox-tile">
          <span>Active</span>
          <input type="checkbox" checked={form.active} onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))} />
        </label>
        <label className="checkbox-tile">
          <span>Stackable</span>
          <input type="checkbox" checked={form.stackable} onChange={(event) => setForm((current) => ({ ...current, stackable: event.target.checked }))} />
        </label>

        <div style={{ gridColumn: '1 / -1' }}>
          <h4 style={{ marginBottom: 8 }}>Applications</h4>
          <div style={{ display: 'grid', gap: 10 }}>
            {applications.map((item, index) => (
              <div key={index} style={{ display: 'grid', gridTemplateColumns: '220px 1fr auto', gap: 10 }}>
                <select value={item.applicableEntityType} onChange={(event) => setApplications((current) => current.map((entry, currentIndex) => currentIndex === index ? { ...entry, applicableEntityType: event.target.value } : entry))}>
                  {APPLICATION_ENTITY_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                </select>
                <input value={item.applicableEntityId} onChange={(event) => setApplications((current) => current.map((entry, currentIndex) => currentIndex === index ? { ...entry, applicableEntityId: event.target.value } : entry))} placeholder="Entity UUID" />
                <button className="btn btn-outline btn-sm" type="button" onClick={() => setApplications((current) => current.filter((_, currentIndex) => currentIndex !== index))}>Remove</button>
              </div>
            ))}
          </div>
          <button className="btn btn-outline btn-sm" type="button" style={{ marginTop: 10 }} onClick={() => setApplications((current) => [...current, { applicableEntityType: 'PRODUCT', applicableEntityId: '' }])}>
            Add application
          </button>
        </div>

        <div className="inline-actions" style={{ gridColumn: '1 / -1' }}>
          <button className="btn btn-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Create promotion'}</button>
        </div>
      </form>
    </section>
  )
}
