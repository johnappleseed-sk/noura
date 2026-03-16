import { useEffect, useMemo, useState } from 'react'
import {
  approveProductSubmission,
  getAdminProductSubmission,
  listAdminProductSubmissions,
  rejectProductSubmission
} from '../shared/api/endpoints/productSubmissionAdminApi'
import { PaginationControls } from '../shared/ui/PaginationControls'
import { Spinner } from '../shared/ui/Spinner'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/PlatformOpsPages.css'

const SUBMISSION_STATUSES = ['PENDING_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED']

function normalizePage(data) {
  return {
    content: data?.content || [],
    page: Number(data?.page) || 0,
    size: Number(data?.size) || 20,
    totalElements: Number(data?.totalElements) || 0,
    totalPages: Math.max(1, Number(data?.totalPages) || 1),
    first: Boolean(data?.first),
    last: Boolean(data?.last)
  }
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.error?.detail || error?.response?.data?.message || error?.message || fallbackMessage
}

function toDisplayDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString()
}

function toStatusBadgeClass(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'APPROVED') return 'badge badge-success'
  if (value === 'REJECTED') return 'badge badge-danger'
  if (value === 'REVISION_REQUESTED') return 'badge badge-warning'
  return 'badge badge-primary'
}

export function ProductSubmissionReviewPage() {
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [savingDecision, setSavingDecision] = useState(false)
  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  const [submissionPage, setSubmissionPage] = useState(normalizePage())
  const [selectedSubmissionId, setSelectedSubmissionId] = useState('')
  const [selectedSubmission, setSelectedSubmission] = useState(null)
  const [filters, setFilters] = useState({ status: '', query: '', duplicatesOnly: false })
  const [draftFilters, setDraftFilters] = useState({ status: '', query: '', duplicatesOnly: false })
  const [decisionForm, setDecisionForm] = useState({ note: '', existingMasterProductId: '' })
  const [pager, setPager] = useState({ page: 0, size: 20 })
  useToastFeedback({ successMessage: flash, errorMessage: error })

  async function loadSubmissions() {
    setLoading(true)
    setError('')
    try {
      const result = await listAdminProductSubmissions({
        page: pager.page,
        size: pager.size,
        sortBy: 'createdAt',
        direction: 'desc',
        status: filters.status || undefined,
        query: filters.query.trim() || undefined,
        duplicatesOnly: filters.duplicatesOnly || undefined
      })
      const normalized = normalizePage(result)
      setSubmissionPage(normalized)
      if (selectedSubmissionId && !normalized.content.some((item) => String(item.id) === String(selectedSubmissionId))) {
        setSelectedSubmissionId('')
        setSelectedSubmission(null)
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load product submissions.'))
      setSubmissionPage(normalizePage())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadSubmissions()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pager.page, pager.size, filters.status, filters.query, filters.duplicatesOnly])

  async function loadSubmissionDetail(submissionId) {
    setSelectedSubmissionId(String(submissionId))
    setSelectedSubmission(null)
    setDetailLoading(true)
    setError('')
    try {
      const detail = await getAdminProductSubmission(submissionId)
      setSelectedSubmission(detail)
      setDecisionForm({
        note: '',
        existingMasterProductId: detail?.matchedMasterProductId || ''
      })
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load submission details.'))
    } finally {
      setDetailLoading(false)
    }
  }

  async function handleApprove() {
    if (!selectedSubmissionId) return
    setSavingDecision(true)
    setError('')
    setFlash('')
    try {
      await approveProductSubmission(selectedSubmissionId, {
        note: decisionForm.note.trim() || null,
        existingMasterProductId: decisionForm.existingMasterProductId.trim() || null
      })
      setFlash('Submission approved.')
      await Promise.all([loadSubmissions(), loadSubmissionDetail(selectedSubmissionId)])
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to approve submission.'))
    } finally {
      setSavingDecision(false)
    }
  }

  async function handleReject() {
    if (!selectedSubmissionId) return
    setSavingDecision(true)
    setError('')
    setFlash('')
    try {
      await rejectProductSubmission(selectedSubmissionId, {
        note: decisionForm.note.trim() || null
      })
      setFlash('Submission rejected.')
      await Promise.all([loadSubmissions(), loadSubmissionDetail(selectedSubmissionId)])
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to reject submission.'))
    } finally {
      setSavingDecision(false)
    }
  }

  const submissions = useMemo(() => submissionPage.content || [], [submissionPage.content])
  const dedupeCandidates = selectedSubmission?.dedupeCandidates || []
  const reviews = selectedSubmission?.reviews || []

  if (loading) {
    return <Spinner label="Loading product submissions..." />
  }

  return (
    <div className="page platform-ops-page">
      <div className="page-head">
        <div>
          <h2>Product Submission Review</h2>
          <p>Approve or reject merchant product candidates into the governed master catalog.</p>
        </div>
      </div>

      <div className="panel-grid platform-ops-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Submission queue</h3>
              <p>Data source: <code>/api/v1/admin/product-submissions</code>.</p>
            </div>
            <button className="btn btn-outline btn-sm" type="button" onClick={() => void loadSubmissions()}>
              Refresh
            </button>
          </div>

          <div className="filters four-up platform-filters">
            <label>
              Status
              <select
                value={draftFilters.status}
                onChange={(event) => setDraftFilters((current) => ({ ...current, status: event.target.value }))}
              >
                <option value="">All</option>
                {SUBMISSION_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </label>

            <label className="grow">
              Query
              <input
                value={draftFilters.query}
                onChange={(event) => setDraftFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Store, merchant, email, ID"
              />
            </label>

            <label className="checkbox-row platform-checkbox">
              <input
                type="checkbox"
                checked={draftFilters.duplicatesOnly}
                onChange={(event) => setDraftFilters((current) => ({ ...current, duplicatesOnly: event.target.checked }))}
              />
              Duplicates only
            </label>

            <div className="inline-actions platform-inline-actions">
              <button
                className="btn btn-outline"
                type="button"
                onClick={() => {
                  setFilters({ ...draftFilters })
                  setPager((current) => ({ ...current, page: 0 }))
                }}
              >
                Apply
              </button>
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => {
                  const reset = { status: '', query: '', duplicatesOnly: false }
                  setDraftFilters(reset)
                  setFilters(reset)
                  setPager((current) => ({ ...current, page: 0 }))
                }}
              >
                Clear
              </button>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Store</th>
                  <th>Merchant</th>
                  <th>Status</th>
                  <th>Duplicate</th>
                  <th>Requested by</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {submissions.length ? (
                  submissions.map((submission) => (
                    <tr
                      key={submission.id}
                      className={selectedSubmissionId === String(submission.id) ? 'row-selected' : ''}
                    >
                      <td className="mono">
                        <button
                          className="table-link-button"
                          type="button"
                          onClick={() => void loadSubmissionDetail(submission.id)}
                        >
                          {String(submission.id).slice(0, 8)}...
                        </button>
                      </td>
                      <td>{submission.storeName || '-'}</td>
                      <td>{submission.merchantName || '-'}</td>
                      <td>
                        <span className={toStatusBadgeClass(submission.status)}>{submission.status || 'UNKNOWN'}</span>
                      </td>
                      <td>{submission.potentialDuplicate ? 'Yes' : 'No'}</td>
                      <td>{submission.requestedByEmail || '-'}</td>
                      <td>{toDisplayDate(submission.createdAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-row" colSpan={7}>No submissions found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <PaginationControls
            page={submissionPage.page}
            totalPages={submissionPage.totalPages}
            totalElements={submissionPage.totalElements}
            first={submissionPage.first}
            last={submissionPage.last}
            pageSize={submissionPage.size}
            onPageSizeChange={(size) => setPager({ page: 0, size })}
            onPrev={() => setPager((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
            onNext={() => setPager((current) => ({ ...current, page: current.page + 1 }))}
            noun="submissions"
          />
        </section>

        <section className="panel platform-side-panel">
          <div className="section-head">
            <div>
              <h3>Review workspace</h3>
              <p>Detail source: <code>/api/v1/admin/product-submissions/&#123;id&#125;</code>.</p>
            </div>
          </div>

          {detailLoading ? <Spinner label="Loading submission details..." /> : null}

          {!detailLoading && !selectedSubmission ? (
            <p className="empty-copy">Select a submission to review candidate details.</p>
          ) : null}

          {!detailLoading && selectedSubmission ? (
            <div className="platform-review-panel">
              <dl className="platform-detail-grid">
                <div><dt>ID</dt><dd className="mono">{selectedSubmission.id}</dd></div>
                <div><dt>Status</dt><dd>{selectedSubmission.status || '-'}</dd></div>
                <div><dt>Store</dt><dd>{selectedSubmission.storeName || '-'}</dd></div>
                <div><dt>Merchant</dt><dd>{selectedSubmission.merchantName || '-'}</dd></div>
                <div><dt>Requested by</dt><dd>{selectedSubmission.requestedByEmail || '-'}</dd></div>
                <div><dt>Reviewed by</dt><dd>{selectedSubmission.reviewedByEmail || '-'}</dd></div>
                <div><dt>Reviewed at</dt><dd>{toDisplayDate(selectedSubmission.reviewedAt)}</dd></div>
                <div><dt>Potential duplicate</dt><dd>{selectedSubmission.potentialDuplicate ? 'Yes' : 'No'}</dd></div>
                <div><dt>Matched product</dt><dd className="mono">{selectedSubmission.matchedMasterProductId || '-'}</dd></div>
              </dl>

              <label>
                Review note
                <textarea
                  rows={3}
                  value={decisionForm.note}
                  onChange={(event) => setDecisionForm((current) => ({ ...current, note: event.target.value }))}
                  placeholder="Optional decision note"
                />
              </label>

              <label>
                Existing master product ID (optional)
                <input
                  value={decisionForm.existingMasterProductId}
                  onChange={(event) => setDecisionForm((current) => ({ ...current, existingMasterProductId: event.target.value }))}
                  placeholder="UUID for duplicate linkage"
                />
              </label>

              <div className="inline-actions platform-inline-actions">
                <button className="btn btn-success" type="button" disabled={savingDecision} onClick={() => void handleApprove()}>
                  {savingDecision ? 'Saving...' : 'Approve'}
                </button>
                <button className="btn btn-danger" type="button" disabled={savingDecision} onClick={() => void handleReject()}>
                  {savingDecision ? 'Saving...' : 'Reject'}
                </button>
              </div>

              <div className="platform-review-section">
                <h4>Dedupe candidates</h4>
                {dedupeCandidates.length ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Master product</th>
                          <th>Score</th>
                          <th>Reason</th>
                          <th>Barcode</th>
                        </tr>
                      </thead>
                      <tbody>
                        {dedupeCandidates.map((candidate) => (
                          <tr key={candidate.id}>
                            <td className="mono">{candidate.masterProductId || '-'}</td>
                            <td>{candidate.matchScore ?? '-'}</td>
                            <td>{candidate.matchReason || '-'}</td>
                            <td>{candidate.barcode || '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="subtle-meta">No dedupe candidates for this submission.</p>
                )}
              </div>

              <div className="platform-review-section">
                <h4>Review history</h4>
                {reviews.length ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Action</th>
                          <th>Reviewer</th>
                          <th>Note</th>
                          <th>Occurred at</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reviews.map((review) => (
                          <tr key={review.id}>
                            <td>{review.action || '-'}</td>
                            <td>{review.reviewerEmail || '-'}</td>
                            <td>{review.note || '-'}</td>
                            <td>{toDisplayDate(review.occurredAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="subtle-meta">No review history entries yet.</p>
                )}
              </div>

              <div className="platform-review-section">
                <h4>Submitted payload</h4>
                <pre className="platform-json">{JSON.stringify(selectedSubmission.product || {}, null, 2)}</pre>
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </div>
  )
}
