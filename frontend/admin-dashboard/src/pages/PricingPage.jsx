import { useEffect, useMemo, useState } from 'react'
import {
  createPriceList,
  createPromotion,
  listActivePromotions,
  listPriceLists,
  quoteVariantPrice,
  upsertPrice
} from '../shared/api/endpoints/pricingApi'
import { formatCurrency, formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'
import { PromotionManagementPanel } from '../features/pricing/PromotionManagementPanel'

const PRICE_LIST_TYPES = ['BASE', 'SALE', 'GROUP', 'CHANNEL']
const PROMOTION_TYPES = ['PERCENTAGE', 'FIXED', 'BUY_X_GET_Y', 'FREE_SHIPPING']
const APPLICATION_ENTITY_TYPES = ['CATEGORY', 'PRODUCT', 'VARIANT']

const DEFAULT_PRICE_LIST_FORM = {
  name: '',
  type: 'BASE',
  customerGroupId: '',
  channelId: ''
}

const DEFAULT_PRICE_FORM = {
  variantId: '',
  priceListId: '',
  amount: '',
  currency: 'USD',
  startDate: '',
  endDate: '',
  priority: ''
}

const DEFAULT_QUOTE_FORM = {
  variantId: '',
  customerGroupId: '',
  channelId: ''
}

const DEFAULT_PROMO_FORM = {
  name: '',
  type: 'PERCENTAGE',
  couponCode: '',
  conditionsJson: '',
  startDate: '',
  endDate: '',
  active: true,
  priority: '0'
}

function parseJson(text, label) {
  const trimmed = String(text || '').trim()
  if (!trimmed) return null
  try {
    return JSON.parse(trimmed)
  } catch (_) {
    throw new Error(`${label} must be valid JSON.`)
  }
}

function toInstant(value) {
  const raw = String(value || '').trim()
  if (!raw) return null
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    throw new Error('Invalid date/time value.')
  }
  return date.toISOString()
}

function toNumber(value, label, { required = false } = {}) {
  const raw = String(value ?? '').trim()
  if (!raw) {
    if (required) throw new Error(`${label} is required.`)
    return null
  }
  const numeric = Number(raw)
  if (Number.isNaN(numeric)) {
    throw new Error(`${label} must be a number.`)
  }
  return numeric
}

export function PricingPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')

  const [priceLists, setPriceLists] = useState([])
  const [promotions, setPromotions] = useState([])

  const [priceListForm, setPriceListForm] = useState(DEFAULT_PRICE_LIST_FORM)
  const [priceForm, setPriceForm] = useState(DEFAULT_PRICE_FORM)
  const [lastUpsertedPrice, setLastUpsertedPrice] = useState(null)

  const [quoteForm, setQuoteForm] = useState(DEFAULT_QUOTE_FORM)
  const [quoteResult, setQuoteResult] = useState(null)

  const [promoForm, setPromoForm] = useState(DEFAULT_PROMO_FORM)
  const [promoApplications, setPromoApplications] = useState([
    { applicableEntityType: 'PRODUCT', applicableEntityId: '' }
  ])

  const priceListOptions = useMemo(() => {
    return [...priceLists].sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))
  }, [priceLists])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [lists, activePromos] = await Promise.all([listPriceLists(), listActivePromotions()])
      setPriceLists(lists || [])
      setPromotions(activePromos || [])
      setPriceForm((current) => ({
        ...current,
        priceListId: current.priceListId || (lists?.[0]?.id || '')
      }))
    } catch (err) {
      setError(err.message || 'Failed to load pricing workspace.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleCreatePriceList(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      await createPriceList({
        name: priceListForm.name.trim(),
        type: priceListForm.type,
        customerGroupId: priceListForm.customerGroupId?.trim() || null,
        channelId: priceListForm.channelId?.trim() || null
      })
      setFlash('Price list created.')
      setPriceListForm(DEFAULT_PRICE_LIST_FORM)
      await load()
    } catch (err) {
      setError(err.message || 'Unable to create price list.')
    } finally {
      setSaving(false)
    }
  }

  async function handleUpsertPrice(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const priorityNumber = toNumber(priceForm.priority, 'Priority')
      const payload = {
        variantId: priceForm.variantId.trim(),
        priceListId: priceForm.priceListId,
        amount: toNumber(priceForm.amount, 'Amount', { required: true }),
        currency: priceForm.currency.trim().toUpperCase(),
        startDate: toInstant(priceForm.startDate),
        endDate: toInstant(priceForm.endDate),
        priority: priorityNumber == null ? null : Math.trunc(priorityNumber)
      }
      const saved = await upsertPrice(payload)
      setLastUpsertedPrice(saved)
      setFlash('Price upserted.')
    } catch (err) {
      setError(err.message || 'Unable to upsert price.')
    } finally {
      setSaving(false)
    }
  }

  async function handleQuote(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const variantId = quoteForm.variantId.trim()
      const quote = await quoteVariantPrice(variantId, {
        customerGroupId: quoteForm.customerGroupId?.trim() || undefined,
        channelId: quoteForm.channelId?.trim() || undefined
      })
      setQuoteResult(quote)
      setFlash('Quote calculated.')
    } catch (err) {
      setError(err.message || 'Unable to quote variant price.')
    } finally {
      setSaving(false)
    }
  }

  async function handleCreatePromotion(event) {
    event.preventDefault()
    setSaving(true)
    setFlash('')
    setError('')
    try {
      const applications = promoApplications
        .map((item) => ({
          applicableEntityType: item.applicableEntityType,
          applicableEntityId: item.applicableEntityId.trim()
        }))
        .filter((item) => item.applicableEntityId)

      const priorityNumber = toNumber(promoForm.priority, 'Priority')
      const payload = {
        name: promoForm.name.trim(),
        type: promoForm.type,
        couponCode: promoForm.couponCode?.trim() || null,
        conditions: parseJson(promoForm.conditionsJson, 'Conditions') || null,
        startDate: toInstant(promoForm.startDate),
        endDate: toInstant(promoForm.endDate),
        active: Boolean(promoForm.active),
        priority: priorityNumber == null ? null : Math.trunc(priorityNumber),
        applications: applications.length ? applications : null
      }

      await createPromotion(payload)
      setFlash('Promotion created.')
      setPromoForm(DEFAULT_PROMO_FORM)
      setPromoApplications([{ applicableEntityType: 'PRODUCT', applicableEntityId: '' }])
      await load()
    } catch (err) {
      setError(err.message || 'Unable to create promotion.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Spinner label="Loading pricing workspace..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <h2>Pricing</h2>
        <p>Manage price lists, override variant pricing, and create promotions.</p>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <PromotionManagementPanel />

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Price lists</h3>
              <p>Create and inspect platform price lists.</p>
            </div>
            <button className="btn btn-outline btn-sm" onClick={load}>
              Refresh
            </button>
          </div>

          {priceLists.length ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Customer group</th>
                    <th>Channel</th>
                  </tr>
                </thead>
                <tbody>
                  {priceLists.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.name}</strong>
                        <div className="subtle-meta mono">{item.id}</div>
                      </td>
                      <td className="mono">{item.type}</td>
                      <td className="mono">{item.customerGroupId || '-'}</td>
                      <td className="mono">{item.channelId || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="empty-copy">No price lists defined yet.</p>
          )}

          <div className="divider" />

          <form onSubmit={handleCreatePriceList}>
            <div className="form-grid">
              <label className="span-2">
                Name
                <input
                  value={priceListForm.name}
                  onChange={(event) => setPriceListForm((c) => ({ ...c, name: event.target.value }))}
                  placeholder="Base USD"
                  required
                />
              </label>
              <label>
                Type
                <select value={priceListForm.type} onChange={(event) => setPriceListForm((c) => ({ ...c, type: event.target.value }))}>
                  {PRICE_LIST_TYPES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Customer group id (optional)
                <input value={priceListForm.customerGroupId} onChange={(event) => setPriceListForm((c) => ({ ...c, customerGroupId: event.target.value }))} />
              </label>
              <label className="span-2">
                Channel id (optional)
                <input value={priceListForm.channelId} onChange={(event) => setPriceListForm((c) => ({ ...c, channelId: event.target.value }))} />
              </label>
            </div>

            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={saving || !priceListForm.name.trim()}>
                {saving ? 'Saving...' : 'Create price list'}
              </button>
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Variant prices</h3>
              <p>Upsert list-based overrides and quote effective price (price list + promotion engine).</p>
            </div>
          </div>

          <form onSubmit={handleUpsertPrice}>
            <div className="form-grid">
              <label className="span-2">
                Variant id
                <input value={priceForm.variantId} onChange={(event) => setPriceForm((c) => ({ ...c, variantId: event.target.value }))} required />
              </label>
              <label className="span-2">
                Price list
                <select value={priceForm.priceListId} onChange={(event) => setPriceForm((c) => ({ ...c, priceListId: event.target.value }))} required>
                  <option value="">Select...</option>
                  {priceListOptions.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name} ({item.type})
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Amount
                <input value={priceForm.amount} onChange={(event) => setPriceForm((c) => ({ ...c, amount: event.target.value }))} required />
              </label>
              <label>
                Currency
                <input value={priceForm.currency} onChange={(event) => setPriceForm((c) => ({ ...c, currency: event.target.value }))} required />
              </label>
              <label>
                Start (optional)
                <input type="datetime-local" value={priceForm.startDate} onChange={(event) => setPriceForm((c) => ({ ...c, startDate: event.target.value }))} />
              </label>
              <label>
                End (optional)
                <input type="datetime-local" value={priceForm.endDate} onChange={(event) => setPriceForm((c) => ({ ...c, endDate: event.target.value }))} />
              </label>
              <label>
                Priority (optional)
                <input value={priceForm.priority} onChange={(event) => setPriceForm((c) => ({ ...c, priority: event.target.value }))} placeholder="0" />
              </label>
            </div>
            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={saving || !priceForm.variantId.trim() || !priceForm.priceListId || !priceForm.amount}>
                {saving ? 'Saving...' : 'Upsert price'}
              </button>
            </div>
          </form>

          {lastUpsertedPrice ? (
            <div className="alert alert-info" style={{ marginTop: 14 }}>
              Saved {formatCurrency(lastUpsertedPrice.amount, lastUpsertedPrice.currency)} for variant <span className="mono">{lastUpsertedPrice.variantId}</span>.
            </div>
          ) : null}

          <div className="divider" />

          <form onSubmit={handleQuote}>
            <div className="form-grid">
              <label className="span-2">
                Variant id
                <input value={quoteForm.variantId} onChange={(event) => setQuoteForm((c) => ({ ...c, variantId: event.target.value }))} required />
              </label>
              <label className="span-2">
                Customer group id (optional)
                <input value={quoteForm.customerGroupId} onChange={(event) => setQuoteForm((c) => ({ ...c, customerGroupId: event.target.value }))} />
              </label>
              <label className="span-2">
                Channel id (optional)
                <input value={quoteForm.channelId} onChange={(event) => setQuoteForm((c) => ({ ...c, channelId: event.target.value }))} />
              </label>
            </div>
            <div className="inline-actions">
              <button className="btn btn-outline" type="submit" disabled={saving || !quoteForm.variantId.trim()}>
                {saving ? 'Working...' : 'Quote price'}
              </button>
            </div>
          </form>

          {quoteResult ? (
            <div className="panel" style={{ marginTop: 18 }}>
              <h4 style={{ marginTop: 0 }}>Quote result</h4>
              <ul className="simple-list">
                <li>
                  <strong>Variant</strong>: <span className="mono">{quoteResult.variantId}</span>
                </li>
                <li>
                  <strong>Currency</strong>: {quoteResult.currency}
                </li>
                <li>
                  <strong>Base</strong>: {formatCurrency(quoteResult.baseAmount, quoteResult.currency)}
                </li>
                <li>
                  <strong>Final</strong>: {formatCurrency(quoteResult.finalAmount, quoteResult.currency)}
                </li>
                <li>
                  <strong>Applied promotions</strong>: {(quoteResult.appliedPromotionIds || []).length ? quoteResult.appliedPromotionIds.join(', ') : '-'}
                </li>
              </ul>
            </div>
          ) : null}
        </section>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Promotions</h3>
            <p>Create promotions and view currently active promotions.</p>
          </div>
        </div>

        {promotions.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Promotion</th>
                  <th>Type</th>
                  <th>Coupon</th>
                  <th>Active</th>
                  <th>Priority</th>
                  <th>Start</th>
                  <th>End</th>
                </tr>
              </thead>
              <tbody>
                {promotions.map((promo) => (
                  <tr key={promo.id}>
                    <td>
                      <strong>{promo.name}</strong>
                      <div className="subtle-meta mono">{promo.id}</div>
                    </td>
                    <td className="mono">{promo.type}</td>
                    <td className="mono">{promo.couponCode || '-'}</td>
                    <td>{promo.active ? 'Yes' : 'No'}</td>
                    <td>{promo.priority}</td>
                    <td>{formatDateTime(promo.startDate)}</td>
                    <td>{formatDateTime(promo.endDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="empty-copy">No active promotions.</p>
        )}

        <div className="divider" />

        <form onSubmit={handleCreatePromotion}>
          <div className="form-grid">
            <label className="span-2">
              Name
              <input value={promoForm.name} onChange={(event) => setPromoForm((c) => ({ ...c, name: event.target.value }))} required />
            </label>
            <label>
              Type
              <select value={promoForm.type} onChange={(event) => setPromoForm((c) => ({ ...c, type: event.target.value }))}>
                {PROMOTION_TYPES.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Coupon code (optional)
              <input value={promoForm.couponCode} onChange={(event) => setPromoForm((c) => ({ ...c, couponCode: event.target.value }))} />
            </label>
            <label className="span-2">
              Conditions (JSON, optional)
              <textarea
                rows="4"
                value={promoForm.conditionsJson}
                onChange={(event) => setPromoForm((c) => ({ ...c, conditionsJson: event.target.value }))}
                placeholder='{"minSubtotal": 50}'
              />
            </label>
            <label>
              Start (optional)
              <input type="datetime-local" value={promoForm.startDate} onChange={(event) => setPromoForm((c) => ({ ...c, startDate: event.target.value }))} />
            </label>
            <label>
              End (optional)
              <input type="datetime-local" value={promoForm.endDate} onChange={(event) => setPromoForm((c) => ({ ...c, endDate: event.target.value }))} />
            </label>
            <label>
              Priority
              <input value={promoForm.priority} onChange={(event) => setPromoForm((c) => ({ ...c, priority: event.target.value }))} />
            </label>
          </div>

          <div className="toggle-row">
            <label className="toggle">
              <input type="checkbox" checked={promoForm.active} onChange={(event) => setPromoForm((c) => ({ ...c, active: event.target.checked }))} />
              Active
            </label>
          </div>

          <div className="divider" />

          <h4 style={{ marginTop: 0 }}>Applications (optional)</h4>
          <p className="subtle-meta">Target a category/product/variant by id. Leave blank to create a globally applicable promotion.</p>

          <div className="stack-list" style={{ marginTop: 12 }}>
            {promoApplications.map((row, index) => (
              <article key={`promo-app-${index}`} className="stack-card">
                <div className="form-grid" style={{ marginBottom: 0 }}>
                  <label>
                    Entity type
                    <select
                      value={row.applicableEntityType}
                      onChange={(event) =>
                        setPromoApplications((current) =>
                          current.map((item, idx) => (idx === index ? { ...item, applicableEntityType: event.target.value } : item))
                        )
                      }
                    >
                      {APPLICATION_ENTITY_TYPES.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="span-2">
                    Entity id
                    <input
                      value={row.applicableEntityId}
                      onChange={(event) =>
                        setPromoApplications((current) =>
                          current.map((item, idx) => (idx === index ? { ...item, applicableEntityId: event.target.value } : item))
                        )
                      }
                      placeholder="UUID"
                    />
                  </label>
                </div>
                <div className="inline-actions">
                  <button
                    className="btn btn-outline btn-sm"
                    type="button"
                    onClick={() => setPromoApplications((current) => current.filter((_, idx) => idx !== index))}
                    disabled={promoApplications.length <= 1}
                  >
                    Remove
                  </button>
                </div>
              </article>
            ))}
          </div>

          <div className="inline-actions wrap" style={{ marginTop: 14 }}>
            <button
              className="btn btn-outline"
              type="button"
              onClick={() => setPromoApplications((current) => [...current, { applicableEntityType: 'PRODUCT', applicableEntityId: '' }])}
            >
              Add application
            </button>
            <button className="btn btn-primary" type="submit" disabled={saving || !promoForm.name.trim()}>
              {saving ? 'Saving...' : 'Create promotion'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
