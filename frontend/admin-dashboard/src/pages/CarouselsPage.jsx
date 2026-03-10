import { useEffect, useMemo, useState } from 'react'
import {
  bulkCarouselAction,
  createCarousel,
  deleteCarousel,
  duplicateCarousel,
  getCarousel,
  getCarouselPreview,
  listCarousels,
  publishCarousel,
  reorderCarousels,
  restoreCarousel,
  updateCarousel,
  updateCarouselStatus
} from '../shared/api/endpoints/carouselsApi'
import { listStores } from '../shared/api/endpoints/storesApi'
import { Spinner } from '../shared/ui/Spinner'

const STATUS_OPTIONS = ['DRAFT', 'ACTIVE', 'INACTIVE', 'SCHEDULED', 'ARCHIVED']
const VISIBILITY_OPTIONS = ['PUBLIC', 'AUTHENTICATED', 'B2B', 'HIDDEN']
const LINK_TYPE_OPTIONS = ['INTERNAL', 'EXTERNAL', 'CATEGORY', 'PRODUCT', 'COLLECTION', 'CUSTOM']
const BULK_ACTION_OPTIONS = ['ACTIVATE', 'DEACTIVATE', 'PUBLISH', 'UNPUBLISH', 'ARCHIVE', 'RESTORE', 'DELETE', 'PIN', 'UNPIN']
const PREVIEW_DEVICES = ['desktop', 'mobile']

function createEmptyDraft() {
  return {
    title: '',
    slug: '',
    description: '',
    imageDesktop: '',
    imageMobile: '',
    altText: '',
    linkType: 'INTERNAL',
    linkValue: '',
    openInNewTab: false,
    buttonText: '',
    secondaryButtonText: '',
    secondaryLinkType: 'INTERNAL',
    secondaryLinkValue: '',
    secondaryOpenInNewTab: false,
    position: '',
    status: 'DRAFT',
    visibility: 'PUBLIC',
    startAt: '',
    endAt: '',
    audienceSegment: '',
    targetingRulesJson: '',
    storeId: '',
    channelId: '',
    locale: 'en-US',
    priority: 0,
    backgroundStyle: 'gradient',
    themeMetadataJson: '',
    published: false,
    pinned: false,
    analyticsKey: '',
    experimentKey: ''
  }
}

function formatDateTimeInput(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part) => String(part).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toIsoOrNull(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

function normalizeDraft(slide) {
  return {
    title: slide?.title || '',
    slug: slide?.slug || '',
    description: slide?.description || '',
    imageDesktop: slide?.imageDesktop || '',
    imageMobile: slide?.imageMobile || '',
    altText: slide?.altText || '',
    linkType: slide?.linkType || 'INTERNAL',
    linkValue: slide?.linkValue || '',
    openInNewTab: Boolean(slide?.openInNewTab),
    buttonText: slide?.buttonText || '',
    secondaryButtonText: slide?.secondaryButtonText || '',
    secondaryLinkType: slide?.secondaryLinkType || 'INTERNAL',
    secondaryLinkValue: slide?.secondaryLinkValue || '',
    secondaryOpenInNewTab: Boolean(slide?.secondaryOpenInNewTab),
    position: slide?.position ?? '',
    status: slide?.status || 'DRAFT',
    visibility: slide?.visibility || 'PUBLIC',
    startAt: formatDateTimeInput(slide?.startAt),
    endAt: formatDateTimeInput(slide?.endAt),
    audienceSegment: slide?.audienceSegment || '',
    targetingRulesJson: slide?.targetingRulesJson || '',
    storeId: slide?.storeId || '',
    channelId: slide?.channelId || '',
    locale: slide?.locale || 'en-US',
    priority: slide?.priority ?? 0,
    backgroundStyle: slide?.backgroundStyle || 'gradient',
    themeMetadataJson: slide?.themeMetadataJson || '',
    published: Boolean(slide?.published),
    pinned: Boolean(slide?.pinned),
    analyticsKey: slide?.analyticsKey || '',
    experimentKey: slide?.experimentKey || ''
  }
}

function buildPayloadFromDraft(draft) {
  // Keep the admin form state close to backend DTO names so request/response mapping stays one-to-one.
  return {
    title: draft.title.trim(),
    slug: draft.slug.trim() || null,
    description: draft.description.trim() || null,
    imageDesktop: draft.imageDesktop.trim(),
    imageMobile: draft.imageMobile.trim() || null,
    altText: draft.altText.trim() || null,
    linkType: draft.linkType,
    linkValue: draft.linkValue.trim() || null,
    openInNewTab: Boolean(draft.openInNewTab),
    buttonText: draft.buttonText.trim() || null,
    secondaryButtonText: draft.secondaryButtonText.trim() || null,
    secondaryLinkType: draft.secondaryButtonText.trim() || draft.secondaryLinkValue.trim() ? draft.secondaryLinkType : null,
    secondaryLinkValue: draft.secondaryLinkValue.trim() || null,
    secondaryOpenInNewTab: Boolean(draft.secondaryOpenInNewTab),
    position: draft.position === '' ? null : Number(draft.position),
    status: draft.status,
    visibility: draft.visibility,
    startAt: toIsoOrNull(draft.startAt),
    endAt: toIsoOrNull(draft.endAt),
    audienceSegment: draft.audienceSegment.trim() || null,
    targetingRulesJson: draft.targetingRulesJson.trim() || null,
    storeId: draft.storeId || null,
    channelId: draft.channelId.trim() || null,
    locale: draft.locale.trim() || null,
    priority: Number(draft.priority || 0),
    backgroundStyle: draft.backgroundStyle.trim() || 'gradient',
    themeMetadataJson: draft.themeMetadataJson.trim() || null,
    published: Boolean(draft.published),
    pinned: Boolean(draft.pinned),
    analyticsKey: draft.analyticsKey.trim() || null,
    experimentKey: draft.experimentKey.trim() || null
  }
}

function statusTone(slide) {
  if (slide?.deletedAt) return 'badge-muted'
  if (slide?.storefrontVisibleNow) return 'badge-success'
  if (slide?.status === 'ARCHIVED' || slide?.status === 'INACTIVE') return 'badge-muted'
  if (slide?.status === 'SCHEDULED') return 'badge-warning'
  return 'badge-info'
}

function safeParseJson(value) {
  if (!value) return {}
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

function ActionBadge({ slide }) {
  return (
    <div className="inline-actions wrap" style={{ gap: 6 }}>
      <span className={`badge ${statusTone(slide)}`}>{slide.deletedAt ? 'Deleted' : slide.status}</span>
      <span className={`badge ${slide.published ? 'badge-success' : 'badge-muted'}`}>
        {slide.published ? 'Published' : 'Unpublished'}
      </span>
      {slide.pinned ? <span className="badge badge-warning">Pinned</span> : null}
    </div>
  )
}

function PreviewCard({ draft, previewMode, previewInfo, onPreviewModeChange }) {
  const imageUrl = previewMode === 'mobile'
    ? (draft.imageMobile || draft.imageDesktop)
    : (draft.imageDesktop || draft.imageMobile)
  const theme = safeParseJson(draft.themeMetadataJson)
  const align = theme.contentPosition || theme.textAlign || 'left'
  const textColor = theme.textColor === 'dark' ? '#091224' : '#ffffff'
  const overlay = draft.backgroundStyle === 'light'
    ? 'linear-gradient(135deg, rgba(255,255,255,0.76), rgba(232,238,247,0.48))'
    : draft.backgroundStyle === 'dark'
      ? 'linear-gradient(135deg, rgba(7,17,39,0.88), rgba(2,8,23,0.72))'
      : 'linear-gradient(135deg, rgba(7,17,39,0.84), rgba(17,58,90,0.55))'

  return (
    <div className="panel" style={{ minHeight: 420 }}>
      <div className="section-head">
        <div>
          <h3>Storefront preview</h3>
          <p>Preview reflects the backend payload shape and current draft values.</p>
        </div>
        <div className="inline-actions wrap">
          {PREVIEW_DEVICES.map((device) => (
            <button
              key={device}
              type="button"
              className={`btn btn-sm ${previewMode === device ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => onPreviewModeChange(device)}
            >
              {device}
            </button>
          ))}
        </div>
      </div>

      <div
        style={{
          position: 'relative',
          borderRadius: 20,
          minHeight: previewMode === 'mobile' ? 520 : 360,
          overflow: 'hidden',
          background: '#0f172a',
          border: '1px solid rgba(148, 163, 184, 0.24)'
        }}
      >
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={draft.altText || draft.title || 'Carousel slide preview'}
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
          />
        ) : null}
        <div style={{ position: 'absolute', inset: 0, background: overlay }} />
        <div
          style={{
            position: 'relative',
            zIndex: 1,
            minHeight: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: align === 'right' ? 'flex-end' : align === 'center' ? 'center' : 'flex-start',
            padding: previewMode === 'mobile' ? 24 : 40
          }}
        >
          <div style={{ maxWidth: 520, color: textColor, textAlign: align }}>
            {draft.audienceSegment ? (
              <div style={{ fontSize: 12, letterSpacing: '0.12em', textTransform: 'uppercase', marginBottom: 12, opacity: 0.8 }}>
                {draft.audienceSegment}
              </div>
            ) : null}
            <h2 style={{ fontSize: previewMode === 'mobile' ? 34 : 56, lineHeight: 1.05, margin: 0 }}>
              {draft.title || 'Hero title'}
            </h2>
            <p style={{ fontSize: 16, lineHeight: 1.6, marginTop: 18, maxWidth: 520 }}>
              {draft.description || 'Add a description, CTA links, and schedule details to preview the storefront state.'}
            </p>
            <div className="inline-actions wrap" style={{ marginTop: 24 }}>
              {draft.buttonText ? <span className="btn btn-primary">{draft.buttonText}</span> : null}
              {draft.secondaryButtonText ? <span className="btn btn-outline">{draft.secondaryButtonText}</span> : null}
            </div>
          </div>
        </div>
      </div>

      {previewInfo ? (
        <div style={{ marginTop: 16 }}>
          <div className="inline-actions wrap" style={{ marginBottom: 8 }}>
            <span className={`badge ${previewInfo.visibleNow ? 'badge-success' : 'badge-warning'}`}>
              {previewInfo.visibleNow ? 'Visible now' : 'Not visible now'}
            </span>
            <span className="badge badge-info">Preview token: {previewInfo.previewToken}</span>
          </div>
          {previewInfo.reasons?.length ? (
            <ul className="simple-list">
              {previewInfo.reasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          ) : (
            <p className="subtle-meta">This slide is currently eligible for storefront display.</p>
          )}
        </div>
      ) : (
        <p className="subtle-meta" style={{ marginTop: 16 }}>
          Save the slide to generate backend preview diagnostics and preview token data.
        </p>
      )}
    </div>
  )
}

function ConfirmModal({ state, onClose }) {
  if (!state) return null

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(15, 23, 42, 0.54)',
        display: 'grid',
        placeItems: 'center',
        zIndex: 50,
        padding: 24
      }}
    >
      <div className="panel" style={{ width: 'min(560px, 100%)' }}>
        <div className="section-head">
          <div>
            <h3>{state.title || 'Confirm action'}</h3>
            <p>{state.message}</p>
          </div>
        </div>
        <div className="inline-actions wrap" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Cancel
          </button>
          <button
            type="button"
            className={`btn ${state.destructive ? 'btn-danger' : 'btn-primary'}`}
            onClick={async () => {
              await state.onConfirm?.()
              onClose()
            }}
          >
            {state.confirmLabel || 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  )
}

export function CarouselsPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true })
  const [storeOptions, setStoreOptions] = useState([])
  const [filters, setFilters] = useState({
    q: '',
    status: '',
    published: '',
    storeId: '',
    locale: '',
    includeDeleted: false,
    sortBy: 'position',
    direction: 'asc',
    page: 0,
    size: 20
  })
  const [selectedIds, setSelectedIds] = useState([])
  const [bulkAction, setBulkAction] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState(createEmptyDraft())
  const [previewMode, setPreviewMode] = useState('desktop')
  const [previewInfo, setPreviewInfo] = useState(null)
  const [confirmState, setConfirmState] = useState(null)
  const [draggedId, setDraggedId] = useState(null)

  const allVisibleSelected = useMemo(() => {
    const visibleIds = pageData.content.map((item) => item.id)
    return visibleIds.length > 0 && visibleIds.every((id) => selectedIds.includes(id))
  }, [pageData.content, selectedIds])

  const canReorder = filters.sortBy === 'position' && filters.direction === 'asc' && filters.page === 0

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [slidesPage, storesPage] = await Promise.all([
        listCarousels({
          q: filters.q || undefined,
          status: filters.status || undefined,
          published: filters.published === '' ? undefined : filters.published === 'true',
          storeId: filters.storeId || undefined,
          locale: filters.locale || undefined,
          includeDeleted: filters.includeDeleted,
          page: filters.page,
          size: filters.size,
          sortBy: filters.sortBy,
          direction: filters.direction
        }),
        listStores({ page: 0, size: 100, sortBy: 'name', direction: 'asc' })
      ])
      setPageData(slidesPage || { content: [], totalElements: 0, totalPages: 0, page: 0, size: filters.size, first: true, last: true })
      setStoreOptions(storesPage?.content || [])
      setSelectedIds((current) => current.filter((id) => (slidesPage?.content || []).some((slide) => slide.id === id)))
    } catch (err) {
      setError(err.message || 'Failed to load carousel slides.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.page, filters.size, filters.q, filters.status, filters.published, filters.storeId, filters.locale, filters.includeDeleted, filters.sortBy, filters.direction])

  async function editCarousel(carouselId) {
    setSaving(true)
    setError('')
    try {
      const [slide, preview] = await Promise.all([
        getCarousel(carouselId, { includeDeleted: true }),
        getCarouselPreview(carouselId)
      ])
      setEditingId(carouselId)
      setDraft(normalizeDraft(slide))
      setPreviewInfo(preview)
      setFlash('')
    } catch (err) {
      setError(err.message || 'Unable to load carousel detail.')
    } finally {
      setSaving(false)
    }
  }

  function startCreate() {
    setEditingId(null)
    setDraft(createEmptyDraft())
    setPreviewInfo(null)
    setFlash('')
    setError('')
  }

  async function saveDraft() {
    setSaving(true)
    setError('')
    setFlash('')
    try {
      const payload = buildPayloadFromDraft(draft)
      const saved = editingId
        ? await updateCarousel(editingId, payload)
        : await createCarousel(payload)
      setFlash(editingId ? 'Carousel updated.' : 'Carousel created.')
      await load()
      await editCarousel(saved.id)
    } catch (err) {
      setError(err.message || 'Unable to save carousel.')
    } finally {
      setSaving(false)
    }
  }

  async function performBulkAction() {
    if (!bulkAction || !selectedIds.length) return
    setSaving(true)
    setError('')
    setFlash('')
    try {
      await bulkCarouselAction({ ids: selectedIds, action: bulkAction })
      setFlash(`Bulk action ${bulkAction.toLowerCase()} completed.`)
      setSelectedIds([])
      await load()
      if (editingId) {
        await editCarousel(editingId)
      }
    } catch (err) {
      setError(err.message || 'Bulk action failed.')
    } finally {
      setSaving(false)
    }
  }

  async function togglePublish(slide) {
    setSaving(true)
    setError('')
    try {
      await publishCarousel(slide.id, {
        published: !slide.published,
        startAt: slide.startAt,
        endAt: slide.endAt
      })
      setFlash(slide.published ? 'Carousel unpublished.' : 'Carousel published.')
      await load()
      if (editingId === slide.id) {
        await editCarousel(slide.id)
      }
    } catch (err) {
      setError(err.message || 'Unable to change publish state.')
    } finally {
      setSaving(false)
    }
  }

  async function quickStatus(slide, status) {
    setSaving(true)
    setError('')
    try {
      await updateCarouselStatus(slide.id, status)
      setFlash(`Carousel marked ${status.toLowerCase()}.`)
      await load()
      if (editingId === slide.id) {
        await editCarousel(slide.id)
      }
    } catch (err) {
      setError(err.message || 'Unable to update status.')
    } finally {
      setSaving(false)
    }
  }

  async function runInlineAction(action, successMessage, reloadId = null) {
    setSaving(true)
    setError('')
    try {
      await action()
      setFlash(successMessage)
      await load()
      if (reloadId) {
        await editCarousel(reloadId)
      }
    } catch (err) {
      setError(err.message || 'Action failed.')
    } finally {
      setSaving(false)
    }
  }

  async function reorderPageRows(sourceId, targetId) {
    if (!canReorder || sourceId === targetId) return
    const reordered = [...pageData.content]
    const sourceIndex = reordered.findIndex((item) => item.id === sourceId)
    const targetIndex = reordered.findIndex((item) => item.id === targetId)
    if (sourceIndex < 0 || targetIndex < 0) return
    const [moved] = reordered.splice(sourceIndex, 1)
    reordered.splice(targetIndex, 0, moved)
    setPageData((current) => ({ ...current, content: reordered }))

    setSaving(true)
    setError('')
    try {
      await reorderCarousels(
        reordered.map((slide, index) => ({
          id: slide.id,
          position: index + 1
        }))
      )
      setFlash('Carousel order updated.')
      await load()
    } catch (err) {
      setError(err.message || 'Unable to reorder carousel slides.')
    } finally {
      setSaving(false)
      setDraggedId(null)
    }
  }

  if (loading) {
    return <Spinner label="Loading carousel workspace..." />
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h2>Carousels</h2>
          <p>Enterprise hero slide management with scheduling, targeting, preview tokens, and storefront-safe publishing rules.</p>
        </div>
        <div className="inline-actions wrap">
          <button type="button" className="btn btn-outline" onClick={load}>
            Refresh
          </button>
          <button type="button" className="btn btn-primary" onClick={startCreate}>
            New slide
          </button>
        </div>
      </div>

      {flash ? <div className="alert alert-success">{flash}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Filters</h3>
            <p>Search and paginate the real backend inventory of carousel slides.</p>
          </div>
        </div>
        <div className="filters">
          <label>
            Search
            <input
              value={filters.q}
              onChange={(event) => setFilters((current) => ({ ...current, q: event.target.value, page: 0 }))}
              placeholder="Title, slug, description..."
            />
          </label>
          <label>
            Status
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value, page: 0 }))}>
              <option value="">All</option>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          </label>
          <label>
            Published
            <select value={filters.published} onChange={(event) => setFilters((current) => ({ ...current, published: event.target.value, page: 0 }))}>
              <option value="">All</option>
              <option value="true">Published</option>
              <option value="false">Unpublished</option>
            </select>
          </label>
          <label>
            Store
            <select value={filters.storeId} onChange={(event) => setFilters((current) => ({ ...current, storeId: event.target.value, page: 0 }))}>
              <option value="">Global only</option>
              {storeOptions.map((store) => (
                <option key={store.id} value={store.id}>{store.name}</option>
              ))}
            </select>
          </label>
          <label>
            Locale
            <input
              value={filters.locale}
              onChange={(event) => setFilters((current) => ({ ...current, locale: event.target.value, page: 0 }))}
              placeholder="en-US"
            />
          </label>
          <label>
            Sort by
            <select value={filters.sortBy} onChange={(event) => setFilters((current) => ({ ...current, sortBy: event.target.value }))}>
              <option value="position">Position</option>
              <option value="priority">Priority</option>
              <option value="publishedAt">Published</option>
              <option value="createdAt">Created</option>
              <option value="title">Title</option>
            </select>
          </label>
          <label>
            Direction
            <select value={filters.direction} onChange={(event) => setFilters((current) => ({ ...current, direction: event.target.value }))}>
              <option value="asc">Ascending</option>
              <option value="desc">Descending</option>
            </select>
          </label>
          <label className="toggle">
            <input
              type="checkbox"
              checked={filters.includeDeleted}
              onChange={(event) => setFilters((current) => ({ ...current, includeDeleted: event.target.checked, page: 0 }))}
            />
            Include deleted
          </label>
        </div>
      </section>

      <div className="panel-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Slide inventory</h3>
              <p>{pageData.totalElements} slides matched the current filters.</p>
            </div>
            <div className="inline-actions wrap">
              <select value={bulkAction} onChange={(event) => setBulkAction(event.target.value)}>
                <option value="">Bulk action</option>
                {BULK_ACTION_OPTIONS.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
              <button
                type="button"
                className="btn btn-outline"
                disabled={!bulkAction || !selectedIds.length || saving}
                onClick={() => setConfirmState({
                  title: 'Apply bulk action',
                  message: `Apply ${bulkAction.toLowerCase()} to ${selectedIds.length} selected slides?`,
                  confirmLabel: 'Apply',
                  onConfirm: performBulkAction
                })}
              >
                Apply
              </button>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      checked={allVisibleSelected}
                      onChange={(event) => {
                        const pageIds = pageData.content.map((item) => item.id)
                        setSelectedIds((current) => {
                          if (event.target.checked) {
                            return Array.from(new Set([...current, ...pageIds]))
                          }
                          return current.filter((id) => !pageIds.includes(id))
                        })
                      }}
                    />
                  </th>
                  <th>Order</th>
                  <th>Slide</th>
                  <th>Status</th>
                  <th>Schedule</th>
                  <th>Scope</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.length ? (
                  pageData.content.map((slide) => (
                    <tr
                      key={slide.id}
                      className={editingId === slide.id ? 'row-selected' : ''}
                      draggable={canReorder}
                      onDragStart={() => setDraggedId(slide.id)}
                      onDragOver={(event) => {
                        if (canReorder) event.preventDefault()
                      }}
                      onDrop={(event) => {
                        event.preventDefault()
                        reorderPageRows(draggedId, slide.id)
                      }}
                      onClick={() => editCarousel(slide.id)}
                      role="button"
                      tabIndex={0}
                    >
                      <td onClick={(event) => event.stopPropagation()}>
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(slide.id)}
                          onChange={(event) => {
                            setSelectedIds((current) => (
                              event.target.checked
                                ? Array.from(new Set([...current, slide.id]))
                                : current.filter((id) => id !== slide.id)
                            ))
                          }}
                        />
                      </td>
                      <td>
                        <span className="mono">{slide.position}</span>
                        {canReorder ? <div className="subtle-meta">drag</div> : null}
                      </td>
                      <td>
                        <strong>{slide.title}</strong>
                        <div className="subtle-meta mono">{slide.slug}</div>
                      </td>
                      <td>
                        <ActionBadge slide={slide} />
                      </td>
                      <td>
                        <div className="subtle-meta">
                          <div>Start: {slide.startAt ? new Date(slide.startAt).toLocaleString() : 'Immediate'}</div>
                          <div>End: {slide.endAt ? new Date(slide.endAt).toLocaleString() : 'Open ended'}</div>
                        </div>
                      </td>
                      <td>
                        <div className="subtle-meta">
                          <div>{slide.storeId || 'Global'}</div>
                          <div>{slide.channelId || 'All channels'} / {slide.locale || 'All locales'}</div>
                        </div>
                      </td>
                      <td onClick={(event) => event.stopPropagation()}>
                        <div className="inline-actions wrap">
                          <button type="button" className="btn btn-outline btn-sm" onClick={() => togglePublish(slide)}>
                            {slide.published ? 'Unpublish' : 'Publish'}
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline btn-sm"
                            onClick={() => runInlineAction(() => duplicateCarousel(slide.id), 'Carousel duplicated.')}
                          >
                            Duplicate
                          </button>
                          <button type="button" className="btn btn-outline btn-sm" onClick={() => quickStatus(slide, slide.status === 'ARCHIVED' ? 'DRAFT' : 'ARCHIVED')}>
                            {slide.status === 'ARCHIVED' ? 'Unarchive' : 'Archive'}
                          </button>
                          {slide.deletedAt ? (
                            <button
                              type="button"
                              className="btn btn-outline btn-sm"
                              onClick={() => runInlineAction(() => restoreCarousel(slide.id), 'Carousel restored.', slide.id)}
                            >
                              Restore
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="btn btn-outline btn-sm"
                              onClick={() => setConfirmState({
                                title: 'Delete carousel slide',
                                message: `Soft delete "${slide.title}"? It will disappear from public APIs immediately.`,
                                confirmLabel: 'Delete',
                                destructive: true,
                                onConfirm: async () => {
                                  await deleteCarousel(slide.id)
                                  await load()
                                }
                              })}
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="7" className="empty-row">No carousel slides found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="inline-actions wrap" style={{ justifyContent: 'space-between', marginTop: 14 }}>
            <div className="subtle-meta">
              Reordering is enabled when the list is sorted by position ascending on page 1.
            </div>
            <div className="inline-actions wrap">
              <button
                type="button"
                className="btn btn-outline"
                disabled={pageData.first}
                onClick={() => setFilters((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
              >
                Previous
              </button>
              <span className="subtle-meta">
                Page {pageData.page + 1} of {Math.max(pageData.totalPages || 1, 1)}
              </span>
              <button
                type="button"
                className="btn btn-outline"
                disabled={pageData.last}
                onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
              >
                Next
              </button>
            </div>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{editingId ? 'Edit slide' : 'Create slide'}</h3>
              <p>Desktop/mobile images, scheduling, targeting, CTA configuration, and storefront metadata.</p>
            </div>
          </div>

          <div className="filters">
            <label>
              Title
              <input value={draft.title} onChange={(event) => setDraft((current) => ({ ...current, title: event.target.value }))} />
            </label>
            <label>
              Slug
              <input value={draft.slug} onChange={(event) => setDraft((current) => ({ ...current, slug: event.target.value }))} placeholder="Auto-generated if blank" />
            </label>
            <label>
              Locale
              <input value={draft.locale} onChange={(event) => setDraft((current) => ({ ...current, locale: event.target.value }))} placeholder="en-US" />
            </label>
            <label>
              Store
              <select value={draft.storeId} onChange={(event) => setDraft((current) => ({ ...current, storeId: event.target.value }))}>
                <option value="">Global</option>
                {storeOptions.map((store) => (
                  <option key={store.id} value={store.id}>{store.name}</option>
                ))}
              </select>
            </label>
            <label>
              Channel
              <input value={draft.channelId} onChange={(event) => setDraft((current) => ({ ...current, channelId: event.target.value }))} placeholder="web / app / retail..." />
            </label>
            <label>
              Status
              <select value={draft.status} onChange={(event) => setDraft((current) => ({ ...current, status: event.target.value }))}>
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
            <label>
              Visibility
              <select value={draft.visibility} onChange={(event) => setDraft((current) => ({ ...current, visibility: event.target.value }))}>
                {VISIBILITY_OPTIONS.map((visibility) => (
                  <option key={visibility} value={visibility}>{visibility}</option>
                ))}
              </select>
            </label>
            <label>
              Position
              <input type="number" value={draft.position} onChange={(event) => setDraft((current) => ({ ...current, position: event.target.value }))} />
            </label>
            <label>
              Priority
              <input type="number" value={draft.priority} onChange={(event) => setDraft((current) => ({ ...current, priority: event.target.value }))} />
            </label>
            <label>
              Background style
              <select value={draft.backgroundStyle} onChange={(event) => setDraft((current) => ({ ...current, backgroundStyle: event.target.value }))}>
                <option value="gradient">Gradient</option>
                <option value="dark">Dark</option>
                <option value="light">Light</option>
              </select>
            </label>
            <label className="toggle">
              <input type="checkbox" checked={draft.published} onChange={(event) => setDraft((current) => ({ ...current, published: event.target.checked }))} />
              Published
            </label>
            <label className="toggle">
              <input type="checkbox" checked={draft.pinned} onChange={(event) => setDraft((current) => ({ ...current, pinned: event.target.checked }))} />
              Pinned
            </label>
          </div>

          <div className="filters">
            <label style={{ gridColumn: '1 / -1' }}>
              Description
              <textarea rows="4" value={draft.description} onChange={(event) => setDraft((current) => ({ ...current, description: event.target.value }))} />
            </label>
            <label>
              Desktop image URL
              <input value={draft.imageDesktop} onChange={(event) => setDraft((current) => ({ ...current, imageDesktop: event.target.value }))} placeholder="https://cdn..." />
            </label>
            <label>
              Mobile image URL
              <input value={draft.imageMobile} onChange={(event) => setDraft((current) => ({ ...current, imageMobile: event.target.value }))} placeholder="Optional fallback to desktop" />
            </label>
            <label>
              Alt text
              <input value={draft.altText} onChange={(event) => setDraft((current) => ({ ...current, altText: event.target.value }))} />
            </label>
            <label>
              Audience segment
              <input value={draft.audienceSegment} onChange={(event) => setDraft((current) => ({ ...current, audienceSegment: event.target.value }))} placeholder="B2B / AUTHENTICATED / VIP" />
            </label>
            <label>
              Start at
              <input type="datetime-local" value={draft.startAt} onChange={(event) => setDraft((current) => ({ ...current, startAt: event.target.value }))} />
            </label>
            <label>
              End at
              <input type="datetime-local" value={draft.endAt} onChange={(event) => setDraft((current) => ({ ...current, endAt: event.target.value }))} />
            </label>
          </div>

          <div className="divider" />
          <h4 style={{ marginTop: 0 }}>Primary CTA</h4>
          <div className="filters">
            <label>
              Link type
              <select value={draft.linkType} onChange={(event) => setDraft((current) => ({ ...current, linkType: event.target.value }))}>
                {LINK_TYPE_OPTIONS.map((type) => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </label>
            <label>
              Link value
              <input value={draft.linkValue} onChange={(event) => setDraft((current) => ({ ...current, linkValue: event.target.value }))} placeholder="/products or https://..." />
            </label>
            <label>
              Button text
              <input value={draft.buttonText} onChange={(event) => setDraft((current) => ({ ...current, buttonText: event.target.value }))} />
            </label>
            <label className="toggle">
              <input type="checkbox" checked={draft.openInNewTab} onChange={(event) => setDraft((current) => ({ ...current, openInNewTab: event.target.checked }))} />
              Open in new tab
            </label>
          </div>

          <h4>Secondary CTA</h4>
          <div className="filters">
            <label>
              Link type
              <select value={draft.secondaryLinkType} onChange={(event) => setDraft((current) => ({ ...current, secondaryLinkType: event.target.value }))}>
                {LINK_TYPE_OPTIONS.map((type) => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </label>
            <label>
              Link value
              <input value={draft.secondaryLinkValue} onChange={(event) => setDraft((current) => ({ ...current, secondaryLinkValue: event.target.value }))} placeholder="/deals or https://..." />
            </label>
            <label>
              Button text
              <input value={draft.secondaryButtonText} onChange={(event) => setDraft((current) => ({ ...current, secondaryButtonText: event.target.value }))} />
            </label>
            <label className="toggle">
              <input type="checkbox" checked={draft.secondaryOpenInNewTab} onChange={(event) => setDraft((current) => ({ ...current, secondaryOpenInNewTab: event.target.checked }))} />
              Open in new tab
            </label>
          </div>

          <h4>Advanced metadata</h4>
          <div className="filters">
            <label>
              Analytics key
              <input value={draft.analyticsKey} onChange={(event) => setDraft((current) => ({ ...current, analyticsKey: event.target.value }))} />
            </label>
            <label>
              Experiment key
              <input value={draft.experimentKey} onChange={(event) => setDraft((current) => ({ ...current, experimentKey: event.target.value }))} />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Theme metadata JSON
              <textarea rows="4" value={draft.themeMetadataJson} onChange={(event) => setDraft((current) => ({ ...current, themeMetadataJson: event.target.value }))} placeholder='{"contentPosition":"left","textColor":"light"}' />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Targeting rules JSON
              <textarea rows="4" value={draft.targetingRulesJson} onChange={(event) => setDraft((current) => ({ ...current, targetingRulesJson: event.target.value }))} placeholder='{"device":"mobile","country":"US"}' />
            </label>
          </div>

          <div className="inline-actions wrap" style={{ marginTop: 18 }}>
            <button type="button" className="btn btn-primary" onClick={saveDraft} disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Save changes' : 'Create slide'}
            </button>
            <button type="button" className="btn btn-outline" onClick={startCreate} disabled={saving}>
              Reset
            </button>
            {editingId ? (
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setConfirmState({
                  title: draft.published ? 'Unpublish slide' : 'Publish slide',
                  message: draft.published
                    ? 'Unpublish this slide and keep the draft in admin?'
                    : 'Publish this slide using the current schedule window?',
                  confirmLabel: draft.published ? 'Unpublish' : 'Publish',
                  onConfirm: async () => {
                    await publishCarousel(editingId, {
                      published: !draft.published,
                      startAt: toIsoOrNull(draft.startAt),
                      endAt: toIsoOrNull(draft.endAt)
                    })
                    await load()
                    await editCarousel(editingId)
                  }
                })}
              >
                {draft.published ? 'Unpublish' : 'Publish'}
              </button>
            ) : null}
          </div>
        </section>
      </div>

      <PreviewCard draft={draft} previewMode={previewMode} previewInfo={previewInfo} onPreviewModeChange={setPreviewMode} />

      <ConfirmModal state={confirmState} onClose={() => setConfirmState(null)} />
    </div>
  )
}
