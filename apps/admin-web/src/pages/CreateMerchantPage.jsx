import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createAdminMerchant } from '../shared/api/endpoints/merchantAdminApi'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/PlatformOpsPages.css'

const STATUS_OPTIONS = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'INACTIVE']

const DEFAULT_FORM = {
  merchantCode: '',
  legalName: '',
  displayName: '',
  email: '',
  phone: '',
  countryCode: 'US',
  status: 'DRAFT',
  contractStartAt: '',
  contractEndAt: '',
  notes: ''
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.error?.detail || error?.response?.data?.message || error?.message || fallbackMessage
}

export function CreateMerchantPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState(DEFAULT_FORM)
  useToastFeedback({ errorMessage: error })

  async function onSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await createAdminMerchant({
        merchantCode: form.merchantCode.trim(),
        legalName: form.legalName.trim(),
        displayName: form.displayName.trim(),
        email: form.email.trim() || null,
        phone: form.phone.trim() || null,
        countryCode: form.countryCode.trim().toUpperCase() || null,
        status: form.status || null,
        contractStartAt: form.contractStartAt || null,
        contractEndAt: form.contractEndAt || null,
        notes: form.notes.trim() || null
      })

      navigate('/admin/merchants', {
        replace: true,
        state: { flash: 'Merchant created successfully.' }
      })
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to create merchant.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page platform-ops-page">
      <div className="page-head">
        <div>
          <h2>Create Merchant</h2>
          <p>Register a merchant record for contract and store onboarding.</p>
        </div>
        <div className="page-head-actions">
          <Link className="btn btn-outline" to="/admin/merchants">
            Back to merchants
          </Link>
        </div>
      </div>

      <section className="panel panel-narrow-form">
        <form className="stack-form" onSubmit={onSubmit} noValidate>
          <label>
            Merchant code
            <input
              value={form.merchantCode}
              onChange={(event) => setForm((current) => ({ ...current, merchantCode: event.target.value }))}
              placeholder="MRC-001"
              required
            />
          </label>

          <label>
            Legal name
            <input
              value={form.legalName}
              onChange={(event) => setForm((current) => ({ ...current, legalName: event.target.value }))}
              placeholder="Example Trading Co."
              required
            />
          </label>

          <label>
            Display name
            <input
              value={form.displayName}
              onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
              placeholder="Example Commerce"
              required
            />
          </label>

          <div className="filters two-up">
            <label>
              Email
              <input
                type="email"
                value={form.email}
                onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                placeholder="ops@example.com"
              />
            </label>

            <label>
              Phone
              <input
                value={form.phone}
                onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                placeholder="+1 555 100 1000"
              />
            </label>
          </div>

          <div className="filters three-up">
            <label>
              Country code
              <input
                value={form.countryCode}
                onChange={(event) => setForm((current) => ({ ...current, countryCode: event.target.value }))}
                placeholder="US"
                maxLength={12}
              />
            </label>

            <label>
              Status
              <select
                value={form.status}
                onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
              >
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="filters two-up">
            <label>
              Contract start
              <input
                type="date"
                value={form.contractStartAt}
                onChange={(event) => setForm((current) => ({ ...current, contractStartAt: event.target.value }))}
              />
            </label>

            <label>
              Contract end
              <input
                type="date"
                value={form.contractEndAt}
                onChange={(event) => setForm((current) => ({ ...current, contractEndAt: event.target.value }))}
              />
            </label>
          </div>

          <label>
            Notes
            <textarea
              rows={4}
              value={form.notes}
              onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))}
              placeholder="Optional onboarding and contract notes"
            />
          </label>

          <div className="inline-actions platform-inline-actions">
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Creating...' : 'Create merchant'}
            </button>
            <button
              className="btn btn-ghost"
              type="button"
              onClick={() => setForm(DEFAULT_FORM)}
              disabled={saving}
            >
              Reset
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
