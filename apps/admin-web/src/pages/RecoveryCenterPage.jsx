import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  applyRecoveryAction,
  approveRecoveryApproval,
  cancelRecoveryJob,
  downloadRecoveryFailureReport,
  listRecoveryApprovalRequests,
  listRecoveryAuditLogs,
  listRecoveryJobs,
  listRecoveryRecords,
  listRecoveryVersions,
  rejectRecoveryApproval,
  requestRecoveryActionApproval,
  requestRecoveryBulkApproval,
  retryRecoveryJob,
  submitRecoveryBulkAction
} from '../shared/api/endpoints/recoveryAdminApi'
import { CAPABILITIES, hasCapability } from '../shared/auth/roles'
import { useAuth } from '../features/auth/useAuth'
import { useConfirmDialog } from '../shared/ui/ConfirmDialogProvider'
import { formatDateTime } from '../shared/ui/formatters'
import { Spinner } from '../shared/ui/Spinner'
import { PaginationControls } from '../shared/ui/PaginationControls'
import { useToast } from '../shared/ui/ToastProvider'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import { apiHost } from '../shared/api/httpClient'
import { getAccessToken } from '../shared/auth/tokenStorage'
import '../styles/pages/RecoveryCenterPage.css'

const ENTITY_TYPES = ['STORE', 'SERVICE_AREA', 'INVENTORY_CATEGORY', 'INVENTORY_PRODUCT', 'PRODUCT']
const RECORD_STATES = ['ACTIVE', 'INACTIVE', 'ARCHIVED', 'TRASHED', 'PURGED', 'ANONYMIZED']
const BULK_ACTIONS = ['TRASH', 'ARCHIVE', 'DEACTIVATE', 'RESTORE', 'APPLY_LEGAL_HOLD']
const DEFAULT_PAGE_SIZE = 25
const RECOVERY_ACTION_TYPES = [
  'CREATE',
  'UPDATE',
  'ACTIVATE',
  'DEACTIVATE',
  'ARCHIVE',
  'TRASH',
  'RESTORE',
  'UNDO_TRASH',
  'HARD_DELETE',
  'APPLY_LEGAL_HOLD',
  'RELEASE_LEGAL_HOLD',
  'ANONYMIZE',
  'RESTORE_POINT_IN_TIME'
]
const JOB_STATUSES = ['VALIDATING', 'QUEUED', 'CANCEL_REQUESTED', 'RUNNING', 'CANCELLED', 'COMPLETED', 'PARTIAL_SUCCESS', 'FAILED']
const DESTRUCTIVE_ACTIONS = new Set(['TRASH', 'ARCHIVE', 'DEACTIVATE', 'ANONYMIZE', 'HARD_DELETE'])
const HIGH_IMPACT_ACTIONS = new Set(['TRASH', 'ANONYMIZE', 'HARD_DELETE'])

const DEFAULT_FILTERS = {
  entityType: '',
  lifecycleState: '',
  query: '',
  riskLevel: '',
  overdueOnly: false
}

const DEFAULT_JOB_FILTERS = {
  query: '',
  entityType: '',
  status: '',
  onlyErrors: false
}

const DEFAULT_AUDIT_FILTERS = {
  query: '',
  entityType: '',
  actionType: '',
  actionStatus: '',
  onlyErrors: false
}

const APPROVAL_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'EXECUTED', 'EXECUTION_FAILED']

const DEFAULT_APPROVAL_FILTERS = {
  query: '',
  entityType: '',
  actionType: '',
  status: 'PENDING'
}

const DEFAULT_BULK_FORM = {
  entityType: 'STORE',
  actionType: 'TRASH',
  entityIds: '',
  reason: '',
  changeTicket: '',
  retentionDays: '30',
  legalHoldUntil: '',
  dryRun: true
}

function emptyPage({ page = 0, size = DEFAULT_PAGE_SIZE } = {}) {
  return {
    content: [],
    page,
    size,
    totalElements: 0,
    totalPages: 0,
    first: page <= 0,
    last: true
  }
}

function toDateTimeLocalValue(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toIsoOrNull(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function summarizeRecord(record) {
  if (!record) return ''
  return `${record.entityType} · ${record.displayName || record.entityId}`
}

function describeAction(actionType) {
  switch (actionType) {
    case 'TRASH':
      return 'Move the selected record to trash.'
    case 'ARCHIVE':
      return 'Archive the selected record without deleting it.'
    case 'DEACTIVATE':
      return 'Mark the selected record inactive.'
    case 'RESTORE':
      return 'Restore the selected record to active state.'
    case 'HARD_DELETE':
      return 'Permanently delete the selected record after a full backup.'
    case 'ANONYMIZE':
      return 'Apply GDPR-safe anonymization and remove the live record.'
    case 'APPLY_LEGAL_HOLD':
      return 'Protect the selected record from purge until the legal-hold expiry.'
    case 'RELEASE_LEGAL_HOLD':
      return 'Release the legal hold protection for the selected record.'
    case 'RESTORE_POINT_IN_TIME':
      return 'Restore the selected record to a captured version timestamp.'
    default:
      return 'Apply a governed lifecycle action.'
  }
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function formatTimelineDay(value) {
  if (!value) return 'Unknown date'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown date'
  return date.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' })
}

function formatTimelineTime(value) {
  if (!value) return '--:--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--:--'
  return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function isLegalHoldActive(record) {
  if (!record?.legalHoldUntil) return false
  const holdUntil = new Date(record.legalHoldUntil)
  if (Number.isNaN(holdUntil.getTime())) return false
  return holdUntil.getTime() > Date.now()
}

function isRetentionOverdue(record) {
  if (!record?.retentionUntil) return false
  if (isLegalHoldActive(record)) return false
  const retentionUntil = new Date(record.retentionUntil)
  if (Number.isNaN(retentionUntil.getTime())) return false
  return retentionUntil.getTime() <= Date.now()
}

function getRecordRisk(record) {
  const lifecycle = String(record?.lifecycleState || '').toUpperCase()
  if (lifecycle === 'PURGED' || lifecycle === 'TRASHED') return 'HIGH'
  if (isRetentionOverdue(record)) return 'HIGH'
  if (!record?.backupVerified && (lifecycle === 'ARCHIVED' || lifecycle === 'TRASHED')) return 'HIGH'
  if (lifecycle === 'ARCHIVED' || lifecycle === 'INACTIVE' || lifecycle === 'ANONYMIZED') return 'MEDIUM'
  return 'LOW'
}

function isPurgeReady(record) {
  return Boolean(isRetentionOverdue(record) && record?.backupVerified && !record?.anonymized)
}

function hasRecoveryAuditError(entry) {
  const status = normalizeText(entry?.actionStatus)
  if (status && !['success', 'ok', 'completed'].includes(status)) {
    return true
  }
  const actionType = normalizeText(entry?.actionType)
  return ['fail', 'error', 'reject', 'deny', 'timeout', 'abort'].some((token) => actionType.includes(token))
}

function hasRecoveryJobError(job) {
  const status = normalizeText(job?.status)
  if (['failed', 'error', 'cancelled', 'aborted', 'partial_failure', 'partial_success'].includes(status)) {
    return true
  }
  return Number(job?.failedItems || 0) > 0
}

function formatJson(value) {
  if (!value) return 'No payload available.'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return String(value)
  }
}

function parseJsonArray(value) {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function resolveLogsUrl(correlationId) {
  const template = import.meta.env.VITE_LOGS_QUERY_URL_TEMPLATE
  const cid = String(correlationId || '').trim()
  if (!template || !cid) return ''
  return String(template).replaceAll('{{correlationId}}', encodeURIComponent(cid))
}

function safeParseJson(value) {
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function computeJsonDiff(beforeValue, afterValue, maxChanges = 200) {
  const changes = []

  const typeOf = (value) => {
    if (Array.isArray(value)) return 'array'
    if (value === null) return 'null'
    return typeof value
  }

  const walk = (before, after, path) => {
    if (changes.length >= maxChanges) return
    if (before === after) return

    const beforeType = typeOf(before)
    const afterType = typeOf(after)
    if (beforeType !== afterType) {
      changes.push({ path, before, after })
      return
    }

    if (beforeType === 'object') {
      const keys = new Set([...Object.keys(before || {}), ...Object.keys(after || {})])
      Array.from(keys)
        .sort()
        .forEach((key) => {
          walk(before?.[key], after?.[key], path ? `${path}.${key}` : key)
        })
      return
    }

    if (beforeType === 'array') {
      if (JSON.stringify(before) !== JSON.stringify(after)) {
        changes.push({ path, before, after })
      }
      return
    }

    changes.push({ path, before, after })
  }

  walk(beforeValue, afterValue, '')
  return changes
}

function formatDiffValue(value) {
  if (value === undefined) return 'undefined'
  if (value === null) return 'null'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

const SAVED_VIEWS_KEY = 'noura.recovery.savedViews.v1'

function loadSavedViews() {
  try {
    const raw = window.localStorage.getItem(SAVED_VIEWS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persistSavedViews(views) {
  try {
    window.localStorage.setItem(SAVED_VIEWS_KEY, JSON.stringify(views || []))
  } catch {
    // ignore
  }
}

function csvCell(value) {
  if (value === null || value === undefined) return ''
  const text = String(value)
  return `"${text.replaceAll('"', '""')}"`
}

function fileStamp() {
  return new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-')
}

function downloadCsv(filename, lines) {
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

function toRecordSearchableText(record) {
  return [
    record.entityType,
    record.entityId,
    record.displayName,
    record.lifecycleState,
    record.lastActionBy,
    record.lastReason
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function toJobSearchableText(job) {
  return [
    job.entityType,
    job.actionType,
    job.status,
    job.requestedBy,
    job.errorSummary,
    job.validationSummaryJson,
    job.resultSummaryJson
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function toAuditSearchableText(entry) {
  return [
    entry.entityType,
    entry.entityId,
    entry.actionType,
    entry.actionStatus,
    entry.actor,
    entry.correlationId,
    entry.message,
    entry.metadataJson
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function toApprovalSearchableText(approval) {
  return [
    approval.requestKind,
    approval.entityType,
    approval.entityId,
    approval.actionType,
    approval.status,
    approval.requestedBy,
    approval.reviewedBy,
    approval.changeTicket,
    approval.reason,
    approval.reviewerNotes,
    approval.executionError
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function riskBadgeClass(riskLevel) {
  if (riskLevel === 'HIGH') return 'badge-danger'
  if (riskLevel === 'MEDIUM') return 'badge-warning'
  return 'badge-success'
}

function severityBadgeClass(severity) {
  if (severity === 'HIGH') return 'badge-danger'
  if (severity === 'MEDIUM') return 'badge-warning'
  return 'badge-muted'
}

function asProgressPercent(job) {
  const total = Number(job?.totalItems || 0)
  const processed = Number(job?.processedItems || 0)
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((processed / total) * 100)))
}

export function RecoveryCenterPage() {
  const { auth } = useAuth()
  const toast = useToast()
  const confirm = useConfirmDialog()
  const canPurge = hasCapability(auth, CAPABILITIES.GOVERNANCE_RECOVERY_PURGE)
  const hasLoadedRef = useRef(false)
  const streamAlertSeenRef = useRef(new Set())
  const [searchParams, setSearchParams] = useSearchParams()

  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [jobsLoading, setJobsLoading] = useState(false)
  const [auditLoading, setAuditLoading] = useState(false)
  const [error, setError] = useState('')
  const [lastRefreshAt, setLastRefreshAt] = useState('')
  const [streamStatus, setStreamStatus] = useState('disconnected')

  const [filters, setFilters] = useState(() => ({
    ...DEFAULT_FILTERS,
    entityType: searchParams.get('entityType') || '',
    lifecycleState: searchParams.get('state') || '',
    query: searchParams.get('q') || '',
    riskLevel: searchParams.get('risk') || '',
    overdueOnly: ['1', 'true', 'yes'].includes(String(searchParams.get('overdue') || '').toLowerCase())
  }))
  const [recordsPage, setRecordsPage] = useState({ content: [], totalElements: 0 })
  const [selectedRecordKey, setSelectedRecordKey] = useState(() => searchParams.get('record') || '')
  const [versions, setVersions] = useState([])
  const [diffSelection, setDiffSelection] = useState(null)
  const [diffQuery, setDiffQuery] = useState('')

  const [auditLogsPage, setAuditLogsPage] = useState(() => emptyPage())
  const [jobsPage, setJobsPage] = useState(() => emptyPage())
  const [jobFilters, setJobFilters] = useState(() => ({
    ...DEFAULT_JOB_FILTERS,
    query: searchParams.get('jobQ') || '',
    entityType: searchParams.get('jobEntity') || '',
    status: searchParams.get('jobStatus') || '',
    onlyErrors: ['1', 'true', 'yes'].includes(String(searchParams.get('jobErr') || '').toLowerCase())
  }))
  const [auditFilters, setAuditFilters] = useState(() => ({
    ...DEFAULT_AUDIT_FILTERS,
    query: searchParams.get('auditQ') || '',
    entityType: searchParams.get('auditEntity') || '',
    actionType: searchParams.get('auditAction') || '',
    actionStatus: searchParams.get('auditStatus') || '',
    onlyErrors: ['1', 'true', 'yes'].includes(String(searchParams.get('auditErr') || '').toLowerCase())
  }))

  const [approvalsPage, setApprovalsPage] = useState(() => emptyPage())
  const [approvalsLoading, setApprovalsLoading] = useState(false)
  const [approvalFilters, setApprovalFilters] = useState(() => ({
    ...DEFAULT_APPROVAL_FILTERS,
    query: searchParams.get('apprQ') || '',
    entityType: searchParams.get('apprEntity') || '',
    actionType: searchParams.get('apprAction') || '',
    status: searchParams.get('apprStatus') || DEFAULT_APPROVAL_FILTERS.status
  }))
  const [selectedApprovalId, setSelectedApprovalId] = useState(() => searchParams.get('approvalId') || '')
  const [reviewerNotes, setReviewerNotes] = useState('')

  const [selectedAuditLogId, setSelectedAuditLogId] = useState(() => searchParams.get('auditId') || '')
  const [selectedJobId, setSelectedJobId] = useState(() => searchParams.get('jobId') || '')
  const [selectedIncidentKey, setSelectedIncidentKey] = useState('')

  const [bulkForm, setBulkForm] = useState(DEFAULT_BULK_FORM)
  const [legalHoldUntil, setLegalHoldUntil] = useState('')
  const [operationReason, setOperationReason] = useState('')
  const [operationTicket, setOperationTicket] = useState('')
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(() => {
    const raw = String(searchParams.get('auto') || '').toLowerCase()
    if (!raw) return true
    return ['1', 'true', 'yes', 'on'].includes(raw)
  })

  const [savedViews, setSavedViews] = useState(() => loadSavedViews())
  const [selectedViewId, setSelectedViewId] = useState('')
  const [viewName, setViewName] = useState('')

  useToastFeedback({
    errorMessage: error
  })

  const records = useMemo(() => {
    return (recordsPage.content || []).map((record) => {
      const legalHoldActive = isLegalHoldActive(record)
      const retentionOverdue = isRetentionOverdue(record)
      const riskLevel = getRecordRisk(record)
      return {
        ...record,
        legalHoldActive,
        retentionOverdue,
        riskLevel,
        purgeReady: isPurgeReady(record),
        searchableText: toRecordSearchableText(record)
      }
    })
  }, [recordsPage.content])

  const visibleRecords = useMemo(() => {
    const normalizedQuery = normalizeText(filters.query)
    return records.filter((record) => {
      if (filters.riskLevel && record.riskLevel !== filters.riskLevel) return false
      if (filters.overdueOnly && !record.retentionOverdue) return false
      if (!normalizedQuery) return true
      return record.searchableText.includes(normalizedQuery)
    })
  }, [records, filters.query, filters.riskLevel, filters.overdueOnly])

  const selectedRecord = useMemo(
    () => visibleRecords.find((record) => `${record.entityType}:${record.entityId}` === selectedRecordKey) || null,
    [visibleRecords, selectedRecordKey]
  )

  const diffChanges = useMemo(() => {
    if (!diffSelection?.older || !diffSelection?.newer) return []
    const before = safeParseJson(diffSelection.older.snapshotJson)
    const after = safeParseJson(diffSelection.newer.snapshotJson)
    return computeJsonDiff(before, after, 200)
  }, [diffSelection])

  const filteredDiffChanges = useMemo(() => {
    const q = normalizeText(diffQuery)
    if (!q) return diffChanges
    return diffChanges.filter((change) => String(change.path || '').toLowerCase().includes(q))
  }, [diffChanges, diffQuery])

  const jobEvents = useMemo(
    () =>
      (jobsPage.content || []).map((job) => ({
        ...job,
        isError: hasRecoveryJobError(job),
        searchableText: toJobSearchableText(job)
      })),
    [jobsPage.content]
  )

  const filteredJobEvents = useMemo(() => {
    const normalizedQuery = normalizeText(jobFilters.query)
    if (!normalizedQuery) return jobEvents
    return jobEvents.filter((job) => job.searchableText.includes(normalizedQuery))
  }, [jobEvents, jobFilters.query])

  const selectedJobEvent = useMemo(
    () => filteredJobEvents.find((job) => String(job.id) === String(selectedJobId)) || null,
    [filteredJobEvents, selectedJobId]
  )

  const auditEvents = useMemo(
    () =>
      (auditLogsPage.content || []).map((entry) => ({
        ...entry,
        isError: hasRecoveryAuditError(entry),
        searchableText: toAuditSearchableText(entry)
      })),
    [auditLogsPage.content]
  )

  const filteredAuditEvents = useMemo(() => {
    const normalizedQuery = normalizeText(auditFilters.query)
    if (!normalizedQuery) return auditEvents
    return auditEvents.filter((entry) => entry.searchableText.includes(normalizedQuery))
  }, [auditEvents, auditFilters.query])

  const selectedAuditEvent = useMemo(
    () => filteredAuditEvents.find((entry) => String(entry.id) === String(selectedAuditLogId)) || null,
    [filteredAuditEvents, selectedAuditLogId]
  )

  const approvalEvents = useMemo(
    () =>
      (approvalsPage.content || []).map((approval) => ({
        ...approval,
        isPending: approval.status === 'PENDING',
        isError: approval.status === 'EXECUTION_FAILED',
        searchableText: toApprovalSearchableText(approval)
      })),
    [approvalsPage.content]
  )

  const filteredApprovalEvents = useMemo(() => {
    const normalizedQuery = normalizeText(approvalFilters.query)
    if (!normalizedQuery) return approvalEvents
    return approvalEvents.filter((approval) => approval.searchableText.includes(normalizedQuery))
  }, [approvalEvents, approvalFilters.query])

  const selectedApproval = useMemo(
    () => filteredApprovalEvents.find((approval) => String(approval.id) === String(selectedApprovalId)) || null,
    [filteredApprovalEvents, selectedApprovalId]
  )

  const auditTimelineGroups = useMemo(() => {
    const grouped = new Map()

    filteredAuditEvents.forEach((entry) => {
      const key = entry.occurredAt ? String(entry.occurredAt).slice(0, 10) : 'unknown'
      if (!grouped.has(key)) {
        grouped.set(key, [])
      }
      grouped.get(key).push(entry)
    })

    return Array.from(grouped.entries())
      .sort(([a], [b]) => (a < b ? 1 : -1))
      .map(([key, entries]) => ({
        key,
        label: key === 'unknown' ? 'Unknown date' : formatTimelineDay(`${key}T00:00:00Z`),
        entries: [...entries].sort((left, right) => new Date(right.occurredAt || 0) - new Date(left.occurredAt || 0))
      }))
  }, [filteredAuditEvents])

  const incidentFeed = useMemo(() => {
    const incidents = []

    jobEvents
      .filter((job) => job.isError)
      .forEach((job) => {
        const failedItems = Number(job.failedItems || 0)
        const severity = failedItems > 0 || normalizeText(job.status) === 'failed' ? 'HIGH' : 'MEDIUM'
        incidents.push({
          key: `job:${job.id}`,
          id: job.id,
          source: 'JOB',
          severity,
          title: `${job.actionType} ${job.entityType}`,
          subtitle: `${job.status || 'UNKNOWN'} · FAIL ${failedItems}`,
          occurredAt: job.updatedAt || job.completedAt || job.startedAt,
          correlationId: null
        })
      })

    auditEvents
      .filter((entry) => entry.isError)
      .forEach((entry) => {
        const isHighImpact = HIGH_IMPACT_ACTIONS.has(String(entry.actionType || '').toUpperCase())
        incidents.push({
          key: `audit:${entry.id}`,
          id: entry.id,
          source: 'AUDIT',
          severity: isHighImpact ? 'HIGH' : 'MEDIUM',
          title: `${entry.actionType} ${entry.entityType}`,
          subtitle: `${entry.actionStatus || 'UNKNOWN'} · ${entry.entityId || 'n/a'}`,
          occurredAt: entry.occurredAt,
          correlationId: entry.correlationId || null
        })
      })

    return incidents.sort((left, right) => new Date(right.occurredAt || 0) - new Date(left.occurredAt || 0))
  }, [auditEvents, jobEvents])

  const governanceSummary = useMemo(() => {
    const highRisk = records.filter((record) => record.riskLevel === 'HIGH').length
    const legalHolds = records.filter((record) => record.legalHoldActive).length
    const overdueRetention = records.filter((record) => record.retentionOverdue).length
    const purgeReady = records.filter((record) => record.purgeReady).length
    const openIncidents = incidentFeed.length
    const failedJobs = jobEvents.filter((job) => job.isError).length
    const failedAuditEvents = auditEvents.filter((entry) => entry.isError).length

    return {
      governedCount: recordsPage.totalElements || records.length,
      highRisk,
      legalHolds,
      overdueRetention,
      purgeReady,
      openIncidents,
      failedJobs,
      failedAuditEvents
    }
  }, [recordsPage.totalElements, records, incidentFeed.length, jobEvents, auditEvents])

  async function loadRecoveryAuditLogsData({ nextPage = auditLogsPage.page, nextSize = auditLogsPage.size } = {}) {
    setAuditLoading(true)
    try {
      const auditLogs = await listRecoveryAuditLogs({
        page: nextPage,
        size: nextSize,
        sortBy: 'occurredAt',
        direction: 'desc',
        entityType: auditFilters.entityType || undefined,
        actionType: auditFilters.actionType || undefined,
        actionStatus: auditFilters.actionStatus || undefined,
        query: auditFilters.query.trim() || undefined,
        errorsOnly: auditFilters.onlyErrors || undefined
      })
      setAuditLogsPage(auditLogs || emptyPage({ page: nextPage, size: nextSize }))
      setLastRefreshAt(new Date().toISOString())
    } catch (requestError) {
      setAuditLogsPage(emptyPage({ page: nextPage, size: nextSize }))
      setError(getErrorMessage(requestError, 'Failed to load recovery audit logs.'))
    } finally {
      setAuditLoading(false)
    }
  }

  async function loadRecoveryJobsData({ nextPage = jobsPage.page, nextSize = jobsPage.size } = {}) {
    setJobsLoading(true)
    try {
      const jobs = await listRecoveryJobs({
        page: nextPage,
        size: nextSize,
        sortBy: 'updatedAt',
        direction: 'desc',
        entityType: jobFilters.entityType || undefined,
        status: jobFilters.status || undefined,
        query: jobFilters.query.trim() || undefined,
        errorsOnly: jobFilters.onlyErrors || undefined
      })
      setJobsPage(jobs || emptyPage({ page: nextPage, size: nextSize }))
      setLastRefreshAt(new Date().toISOString())
    } catch (requestError) {
      setJobsPage(emptyPage({ page: nextPage, size: nextSize }))
      setError(getErrorMessage(requestError, 'Failed to load recovery jobs.'))
    } finally {
      setJobsLoading(false)
    }
  }

  async function loadRecoveryApprovalsData({ nextPage = approvalsPage.page, nextSize = approvalsPage.size } = {}) {
    setApprovalsLoading(true)
    try {
      const approvals = await listRecoveryApprovalRequests({
        page: nextPage,
        size: nextSize,
        sortBy: 'requestedAt',
        direction: 'desc',
        entityType: approvalFilters.entityType || undefined,
        actionType: approvalFilters.actionType || undefined,
        status: approvalFilters.status || undefined,
        query: approvalFilters.query.trim() || undefined
      })
      setApprovalsPage(approvals || emptyPage({ page: nextPage, size: nextSize }))
      setLastRefreshAt(new Date().toISOString())
    } catch (requestError) {
      setApprovalsPage(emptyPage({ page: nextPage, size: nextSize }))
      setError(getErrorMessage(requestError, 'Failed to load recovery approvals.'))
    } finally {
      setApprovalsLoading(false)
    }
  }

  async function loadRecoveryRecords(nextSelectedKey = selectedRecordKey) {
    setLoading(true)
    setError('')

    try {
      const [recordsData, auditLogs, jobs] = await Promise.all([
        listRecoveryRecords({
          page: 0,
          size: 100,
          sortBy: 'updatedAt',
          direction: 'desc',
          entityType: filters.entityType || undefined,
          lifecycleState: filters.lifecycleState || undefined,
          query: filters.query || undefined
        }),
        listRecoveryAuditLogs({
          page: auditLogsPage.page,
          size: auditLogsPage.size,
          sortBy: 'occurredAt',
          direction: 'desc',
          entityType: auditFilters.entityType || undefined,
          actionType: auditFilters.actionType || undefined,
          actionStatus: auditFilters.actionStatus || undefined,
          query: auditFilters.query.trim() || undefined,
          errorsOnly: auditFilters.onlyErrors || undefined
        }),
        listRecoveryJobs({
          page: jobsPage.page,
          size: jobsPage.size,
          sortBy: 'updatedAt',
          direction: 'desc',
          entityType: jobFilters.entityType || undefined,
          status: jobFilters.status || undefined,
          query: jobFilters.query.trim() || undefined,
          errorsOnly: jobFilters.onlyErrors || undefined
        })
      ])

      const content = recordsData?.content || []
      const resolvedKey = content.some((record) => `${record.entityType}:${record.entityId}` === nextSelectedKey)
        ? nextSelectedKey
        : content[0]
          ? `${content[0].entityType}:${content[0].entityId}`
          : ''

      setRecordsPage(recordsData || { content: [], totalElements: 0 })
      setAuditLogsPage(auditLogs || emptyPage({ page: auditLogsPage.page, size: auditLogsPage.size }))
      setJobsPage(jobs || emptyPage({ page: jobsPage.page, size: jobsPage.size }))
      void loadRecoveryApprovalsData({ nextPage: approvalsPage.page, nextSize: approvalsPage.size })
      setSelectedRecordKey(resolvedKey)
      setLastRefreshAt(new Date().toISOString())
      hasLoadedRef.current = true
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Failed to load the recovery center.'))
    } finally {
      setLoading(false)
    }
  }

  async function loadVersions(record) {
    if (!record) {
      setVersions([])
      return
    }

    try {
      const data = await listRecoveryVersions(record.entityType, record.entityId)
      setVersions(Array.isArray(data) ? data : [])
    } catch (requestError) {
      setVersions([])
      setError(getErrorMessage(requestError, 'Failed to load recovery versions.'))
    }
  }

  useEffect(() => {
    void loadRecoveryRecords()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const next = new URLSearchParams()
    const setParam = (key, value) => {
      const text = String(value || '').trim()
      if (!text) return
      next.set(key, text)
    }
    const setBool = (key, value) => {
      if (value) next.set(key, '1')
    }

    setParam('entityType', filters.entityType)
    setParam('state', filters.lifecycleState)
    setParam('q', filters.query)
    setParam('risk', filters.riskLevel)
    setBool('overdue', filters.overdueOnly)
    setParam('record', selectedRecordKey)

    setParam('jobQ', jobFilters.query)
    setParam('jobEntity', jobFilters.entityType)
    setParam('jobStatus', jobFilters.status)
    setBool('jobErr', jobFilters.onlyErrors)
    setParam('jobId', selectedJobId)

    setParam('auditQ', auditFilters.query)
    setParam('auditEntity', auditFilters.entityType)
    setParam('auditAction', auditFilters.actionType)
    setParam('auditStatus', auditFilters.actionStatus)
    setBool('auditErr', auditFilters.onlyErrors)
    setParam('auditId', selectedAuditLogId)

    setParam('apprQ', approvalFilters.query)
    setParam('apprEntity', approvalFilters.entityType)
    setParam('apprAction', approvalFilters.actionType)
    setParam('apprStatus', approvalFilters.status)
    setParam('approvalId', selectedApprovalId)

    if (!autoRefreshEnabled) {
      next.set('auto', '0')
    }

    setSearchParams(next, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    filters.entityType,
    filters.lifecycleState,
    filters.query,
    filters.riskLevel,
    filters.overdueOnly,
    selectedRecordKey,
    jobFilters.query,
    jobFilters.entityType,
    jobFilters.status,
    jobFilters.onlyErrors,
    selectedJobId,
    auditFilters.query,
    auditFilters.entityType,
    auditFilters.actionType,
    auditFilters.actionStatus,
    auditFilters.onlyErrors,
    selectedAuditLogId,
    approvalFilters.query,
    approvalFilters.entityType,
    approvalFilters.actionType,
    approvalFilters.status,
    selectedApprovalId,
    autoRefreshEnabled
  ])

  useEffect(() => {
    const token = getAccessToken()
    if (!token) {
      setStreamStatus('disconnected')
      return undefined
    }

    const controller = new AbortController()
    let cancelled = false
    let reconnectTimer = null

    const upsertPageItem = (page, item) => {
      if (!item?.id) return page
      const content = Array.isArray(page?.content) ? [...page.content] : []
      const id = String(item.id)
      const index = content.findIndex((row) => String(row?.id) === id)
      if (index >= 0) {
        content[index] = { ...content[index], ...item }
        return { ...page, content }
      }
      const isFirstPage = Number(page?.page || 0) <= 0
      if (!isFirstPage) {
        return page
      }
      content.unshift(item)
      const size = Number(page?.size || 0)
      return { ...page, content: size ? content.slice(0, size) : content }
    }

    const seenAlerts = streamAlertSeenRef.current

    const handleEnvelope = (envelope) => {
      const type = envelope?.type
      const payload = envelope?.payload
      if (!type || !payload) return
      setLastRefreshAt(new Date().toISOString())

      if (type === 'recovery.job') {
        setJobsPage((current) => upsertPageItem(current, payload))
        const status = String(payload.status || '').toUpperCase()
        const terminal = ['FAILED', 'PARTIAL_SUCCESS', 'CANCELLED'].includes(status)
        if (terminal && hasRecoveryJobError(payload)) {
          const key = `job:${payload.id}:${status}`
          if (!seenAlerts.has(key)) {
            seenAlerts.add(key)
            toast.error(`Recovery job ${String(payload.id).slice(0, 8)} ${status.replaceAll('_', ' ').toLowerCase()}.`)
          }
        }
        return
      }

      if (type === 'recovery.audit') {
        setAuditLogsPage((current) => upsertPageItem(current, payload))
        if (hasRecoveryAuditError(payload)) {
          const key = `audit:${payload.id}`
          if (!seenAlerts.has(key)) {
            seenAlerts.add(key)
            toast.error(`Recovery audit error: ${payload.actionType || 'ACTION'} (${payload.actionStatus || 'UNKNOWN'}).`)
          }
        }
        return
      }

      if (type === 'recovery.approval') {
        setApprovalsPage((current) => upsertPageItem(current, payload))
        if (String(payload.status || '').toUpperCase() === 'EXECUTION_FAILED') {
          const key = `approval:${payload.id}`
          if (!seenAlerts.has(key)) {
            seenAlerts.add(key)
            toast.error(`Approval execution failed (${String(payload.id).slice(0, 8)}).`)
          }
        }
      }
    }

    const streamUrl = `${apiHost.replace(/\/+$/, '')}/api/v1/admin/recovery/stream`

    async function connect() {
      if (cancelled) return
      setStreamStatus('connecting')
      try {
        const response = await fetch(streamUrl, {
          method: 'GET',
          headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${getAccessToken() || token}`
          },
          signal: controller.signal
        })

        if (!response.ok || !response.body) {
          throw new Error(`Stream unavailable (${response.status}).`)
        }

        setStreamStatus('connected')
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (!cancelled) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          buffer = buffer.replace(/\r\n/g, '\n')

          let boundaryIndex = buffer.indexOf('\n\n')
          while (boundaryIndex >= 0) {
            const chunk = buffer.slice(0, boundaryIndex)
            buffer = buffer.slice(boundaryIndex + 2)
            boundaryIndex = buffer.indexOf('\n\n')

            const dataLines = chunk
              .split('\n')
              .map((line) => line.trimEnd())
              .filter((line) => line.startsWith('data:'))
              .map((line) => line.slice(5).trim())
              .filter(Boolean)

            if (!dataLines.length) continue
            const raw = dataLines.join('\n')

            try {
              const envelope = JSON.parse(raw)
              handleEnvelope(envelope)
            } catch {
              // ignore malformed payloads
            }
          }
        }
      } catch {
        if (cancelled) return
        setStreamStatus('disconnected')
        reconnectTimer = window.setTimeout(() => void connect(), 2500)
      }
    }

    void connect()

    return () => {
      cancelled = true
      controller.abort()
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth?.accessToken])

  useEffect(() => {
    if (!visibleRecords.length) {
      if (selectedRecordKey) setSelectedRecordKey('')
      return
    }
    if (!selectedRecordKey || !visibleRecords.some((record) => `${record.entityType}:${record.entityId}` === selectedRecordKey)) {
      setSelectedRecordKey(`${visibleRecords[0].entityType}:${visibleRecords[0].entityId}`)
    }
  }, [visibleRecords, selectedRecordKey])

  useEffect(() => {
    if (!filteredJobEvents.length) {
      if (selectedJobId) setSelectedJobId('')
      return
    }
    if (!selectedJobId || !filteredJobEvents.some((job) => String(job.id) === String(selectedJobId))) {
      setSelectedJobId(filteredJobEvents[0].id)
    }
  }, [filteredJobEvents, selectedJobId])

  useEffect(() => {
    if (!filteredApprovalEvents.length) {
      if (selectedApprovalId) setSelectedApprovalId('')
      return
    }
    if (!selectedApprovalId || !filteredApprovalEvents.some((approval) => String(approval.id) === String(selectedApprovalId))) {
      setSelectedApprovalId(filteredApprovalEvents[0].id)
    }
  }, [filteredApprovalEvents, selectedApprovalId])

  useEffect(() => {
    if (!filteredAuditEvents.length) {
      if (selectedAuditLogId) setSelectedAuditLogId('')
      return
    }
    if (!selectedAuditLogId || !filteredAuditEvents.some((entry) => String(entry.id) === String(selectedAuditLogId))) {
      setSelectedAuditLogId(filteredAuditEvents[0].id)
    }
  }, [filteredAuditEvents, selectedAuditLogId])

  useEffect(() => {
    if (!incidentFeed.length) {
      if (selectedIncidentKey) setSelectedIncidentKey('')
      return
    }
    if (!selectedIncidentKey || !incidentFeed.some((incident) => incident.key === selectedIncidentKey)) {
      setSelectedIncidentKey(incidentFeed[0].key)
    }
  }, [incidentFeed, selectedIncidentKey])

  useEffect(() => {
    setLegalHoldUntil(toDateTimeLocalValue(selectedRecord?.legalHoldUntil))
  }, [selectedRecord?.entityType, selectedRecord?.entityId, selectedRecord?.legalHoldUntil])

  useEffect(() => {
    void loadVersions(selectedRecord)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRecord?.entityType, selectedRecord?.entityId])

  useEffect(() => {
    if (!hasLoadedRef.current) return undefined
    const timeoutId = window.setTimeout(() => {
      void loadRecoveryJobsData({ nextPage: 0, nextSize: jobsPage.size })
    }, 350)
    return () => window.clearTimeout(timeoutId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobFilters.query, jobFilters.entityType, jobFilters.status, jobFilters.onlyErrors])

  useEffect(() => {
    if (!hasLoadedRef.current) return undefined
    const timeoutId = window.setTimeout(() => {
      void loadRecoveryAuditLogsData({ nextPage: 0, nextSize: auditLogsPage.size })
    }, 350)
    return () => window.clearTimeout(timeoutId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auditFilters.query, auditFilters.entityType, auditFilters.actionType, auditFilters.actionStatus, auditFilters.onlyErrors])

  useEffect(() => {
    if (!hasLoadedRef.current) return undefined
    const timeoutId = window.setTimeout(() => {
      void loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })
    }, 350)
    return () => window.clearTimeout(timeoutId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [approvalFilters.query, approvalFilters.entityType, approvalFilters.actionType, approvalFilters.status])

  useEffect(() => {
    if (!autoRefreshEnabled || !hasLoadedRef.current) return undefined
    const intervalId = window.setInterval(() => {
      void Promise.all([loadRecoveryJobsData(), loadRecoveryAuditLogsData(), loadRecoveryApprovalsData()])
    }, 30000)
    return () => window.clearInterval(intervalId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    autoRefreshEnabled,
    jobFilters.query,
    jobFilters.entityType,
    jobFilters.status,
    jobFilters.onlyErrors,
    auditFilters.query,
    auditFilters.entityType,
    auditFilters.actionType,
    auditFilters.actionStatus,
    auditFilters.onlyErrors,
    approvalFilters.query,
    approvalFilters.entityType,
    approvalFilters.actionType,
    approvalFilters.status
  ])

  async function runAction(actionType, overrides = {}) {
    if (!selectedRecord) return

    const reasonInput = String(overrides.reason ?? operationReason).trim()
    const reason = reasonInput || `${actionType} requested from the admin recovery center.`
    const changeTicket = operationTicket.trim()

    if (DESTRUCTIVE_ACTIONS.has(actionType) && reason.length < 12) {
      const message = 'Provide a business reason (at least 12 characters) before applying destructive actions.'
      setError(message)
      toast.error(message)
      return
    }

    if (HIGH_IMPACT_ACTIONS.has(actionType) && !changeTicket) {
      const message = 'Change ticket is required for high-impact actions (trash/anonymize/permanent delete).'
      setError(message)
      toast.error(message)
      return
    }

    const payload = {
      entityType: selectedRecord.entityType,
      entityId: selectedRecord.entityId,
      actionType,
      reason,
      restoreTo: overrides.restoreTo || null,
      legalHoldUntil: overrides.legalHoldUntil || null,
      retentionDays: overrides.retentionDays ?? null,
      metadata: {
        source: 'admin-recovery-center',
        changeTicket: changeTicket || null,
        riskLevel: selectedRecord.riskLevel
      }
    }

    const approvalFlow = HIGH_IMPACT_ACTIONS.has(actionType)

    const confirmed = await confirm({
      title: `${actionType.replaceAll('_', ' ')} ${selectedRecord.displayName || selectedRecord.entityId}?`,
      message: `${describeAction(actionType)}${approvalFlow ? ' This will create an approval request and will not execute until a second admin approves it.' : ''}`,
      description: `${summarizeRecord(selectedRecord)}${changeTicket ? ` · ${changeTicket}` : ''}`,
      confirmLabel: approvalFlow ? 'Request approval' : 'Confirm',
      tone: actionType === 'HARD_DELETE' || actionType === 'ANONYMIZE' ? 'danger' : 'primary'
    })
    if (!confirmed) return

    setWorking(true)
    setError('')

    try {
      if (approvalFlow) {
        const approval = await requestRecoveryActionApproval(payload)
        toast.success(`Approval requested (${String(approval?.id || '').slice(0, 8)}).`)
        void loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })
      } else {
        const result = await applyRecoveryAction(payload)
        toast.success(result?.message || 'Recovery action completed.')
        await loadRecoveryRecords(`${selectedRecord.entityType}:${selectedRecord.entityId}`)
      }
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to apply the recovery action.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function restoreVersion(version) {
    if (!selectedRecord || !version?.capturedAt) return
    await runAction('RESTORE_POINT_IN_TIME', {
      restoreTo: version.capturedAt,
      reason: `Point-in-time restore to version ${version.versionNumber}.`
    })
  }

  async function submitBulk(event) {
    event.preventDefault()
    setWorking(true)
    setError('')

    try {
      const entityIds = bulkForm.entityIds
        .split('\n')
        .map((value) => value.trim())
        .filter(Boolean)

      if (!entityIds.length) {
        throw new Error('Enter at least one entity id for bulk operations.')
      }

      const reason = bulkForm.reason.trim()
      const changeTicket = bulkForm.changeTicket.trim()

      if (!bulkForm.dryRun && DESTRUCTIVE_ACTIONS.has(bulkForm.actionType) && reason.length < 12) {
        throw new Error('Bulk destructive actions require a reason with at least 12 characters.')
      }

      if (!bulkForm.dryRun && !changeTicket) {
        throw new Error('Bulk non-dry-run actions require a change ticket.')
      }

      const payload = {
        entityType: bulkForm.entityType,
        actionType: bulkForm.actionType,
        entityIds,
        reason: reason || null,
        dryRun: bulkForm.dryRun,
        legalHoldUntil: toIsoOrNull(bulkForm.legalHoldUntil),
        retentionDays: bulkForm.retentionDays ? Number(bulkForm.retentionDays) : null,
        metadata: {
          source: 'admin-recovery-center',
          changeTicket: changeTicket || null
        }
      }

      if (!payload.dryRun && HIGH_IMPACT_ACTIONS.has(payload.actionType)) {
        const approval = await requestRecoveryBulkApproval(payload)
        toast.success(`Bulk approval requested (${String(approval?.id || '').slice(0, 8)}).`)
        void loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })
      } else {
        const job = await submitRecoveryBulkAction(payload)
        toast.success(`Recovery job ${job?.id?.slice?.(0, 8) || ''} queued.`)
      }

      setBulkForm(DEFAULT_BULK_FORM)
      await loadRecoveryRecords(selectedRecordKey)
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to queue the bulk recovery job.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function approveApprovalRequest(approval) {
    if (!approval?.id) return
    const confirmed = await confirm({
      title: `Approve ${approval.actionType}?`,
      message: 'This will execute the approved action immediately.',
      description: `${approval.entityType}${approval.entityId ? ` · ${approval.entityId}` : ''}${approval.changeTicket ? ` · ${approval.changeTicket}` : ''}`,
      confirmLabel: 'Approve and execute',
      tone: 'primary'
    })
    if (!confirmed) return

    setWorking(true)
    setError('')

    try {
      const updated = await approveRecoveryApproval(approval.id, { reviewerNotes: reviewerNotes.trim() || null })
      toast.success(`Approval ${String(updated?.status || 'updated').replaceAll('_', ' ')}.`)
      setReviewerNotes('')
      void loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })
      void loadRecoveryJobsData({ nextPage: 0, nextSize: jobsPage.size })
      void loadRecoveryAuditLogsData({ nextPage: 0, nextSize: auditLogsPage.size })
      if (updated?.executedJobId) {
        setSelectedJobId(updated.executedJobId)
      } else if (updated?.entityType && updated?.entityId) {
        await loadRecoveryRecords(`${updated.entityType}:${updated.entityId}`)
      }
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to approve the request.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function rejectApprovalRequest(approval) {
    if (!approval?.id) return
    const confirmed = await confirm({
      title: `Reject ${approval.actionType}?`,
      message: 'This will reject the request and prevent execution.',
      description: `${approval.entityType}${approval.entityId ? ` · ${approval.entityId}` : ''}${approval.changeTicket ? ` · ${approval.changeTicket}` : ''}`,
      confirmLabel: 'Reject request',
      tone: 'danger'
    })
    if (!confirmed) return

    setWorking(true)
    setError('')

    try {
      const updated = await rejectRecoveryApproval(approval.id, { reviewerNotes: reviewerNotes.trim() || null })
      toast.success(`Approval ${String(updated?.status || 'rejected').replaceAll('_', ' ')}.`)
      setReviewerNotes('')
      void loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to reject the request.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function cancelSelectedJob(job) {
    if (!job?.id) return
    const confirmed = await confirm({
      title: 'Cancel job?',
      message: 'This will request cancellation of the job. In-flight items may still finish.',
      description: `${job.actionType} · ${job.entityType} · ${String(job.id).slice(0, 8)}`,
      confirmLabel: 'Request cancel',
      tone: 'danger'
    })
    if (!confirmed) return

    setWorking(true)
    setError('')

    try {
      const updated = await cancelRecoveryJob(job.id)
      toast.success(`Job status: ${updated?.status || 'updated'}.`)
      void loadRecoveryJobsData({ nextPage: jobsPage.page, nextSize: jobsPage.size })
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to request job cancellation.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function retrySelectedJob(job, { failedOnly = true } = {}) {
    if (!job?.id) return
    const confirmed = await confirm({
      title: failedOnly ? 'Retry failed items?' : 'Retry job?',
      message: 'This will create a new job from the stored request payload.',
      description: `${job.actionType} · ${job.entityType} · ${String(job.id).slice(0, 8)}`,
      confirmLabel: 'Queue retry',
      tone: 'primary'
    })
    if (!confirmed) return

    setWorking(true)
    setError('')

    try {
      const queued = await retryRecoveryJob(job.id, failedOnly)
      toast.success(`Retry job queued (${String(queued?.id || '').slice(0, 8)}).`)
      setSelectedJobId(queued?.id || '')
      void loadRecoveryJobsData({ nextPage: 0, nextSize: jobsPage.size })
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to retry the job.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  async function downloadFailureReport(job) {
    if (!job?.id) return
    setWorking(true)
    setError('')
    try {
      const csv = await downloadRecoveryFailureReport(job.id)
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `recovery-job-${String(job.id).slice(0, 8)}-failures.csv`
      anchor.click()
      window.URL.revokeObjectURL(url)
      toast.success('Failure report downloaded.')
    } catch (requestError) {
      const message = getErrorMessage(requestError, 'Failed to download the failure report.')
      setError(message)
      toast.error(message)
    } finally {
      setWorking(false)
    }
  }

  function applySavedView(viewId) {
    const view = savedViews.find((candidate) => String(candidate.id) === String(viewId))
    if (!view?.state) return

    const nextFilters = { ...DEFAULT_FILTERS, ...(view.state.filters || {}) }
    const nextJobFilters = { ...DEFAULT_JOB_FILTERS, ...(view.state.jobFilters || {}) }
    const nextAuditFilters = { ...DEFAULT_AUDIT_FILTERS, ...(view.state.auditFilters || {}) }
    const nextApprovalFilters = { ...DEFAULT_APPROVAL_FILTERS, ...(view.state.approvalFilters || {}) }

    setFilters(nextFilters)
    setJobFilters(nextJobFilters)
    setAuditFilters(nextAuditFilters)
    setApprovalFilters(nextApprovalFilters)
    if (view.state.selectedRecordKey) {
      setSelectedRecordKey(view.state.selectedRecordKey)
    }
    toast.success(`Loaded view: ${view.name || 'Saved view'}.`)
    void loadRecoveryRecords(view.state.selectedRecordKey || selectedRecordKey)
  }

  function saveCurrentView() {
    const name = viewName.trim()
    if (name.length < 2) {
      toast.error('Provide a view name (at least 2 characters).')
      return
    }

    const id = (globalThis.crypto?.randomUUID ? globalThis.crypto.randomUUID() : `${Date.now()}`)
    const next = [
      {
        id,
        name,
        createdAt: new Date().toISOString(),
        state: {
          filters,
          jobFilters,
          auditFilters,
          approvalFilters,
          selectedRecordKey
        }
      },
      ...savedViews.filter((view) => String(view.id) !== String(id))
    ]
      .slice(0, 30)
      .sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))

    setSavedViews(next)
    persistSavedViews(next)
    setSelectedViewId(id)
    setViewName('')
    toast.success('Saved view stored locally.')
  }

  async function deleteSavedView(viewId) {
    const view = savedViews.find((candidate) => String(candidate.id) === String(viewId))
    if (!view) return
    const confirmed = await confirm({
      title: 'Delete saved view?',
      message: 'This will remove the saved view from your browser only.',
      description: view.name,
      confirmLabel: 'Delete view',
      tone: 'danger'
    })
    if (!confirmed) return

    const next = savedViews.filter((candidate) => String(candidate.id) !== String(viewId))
    setSavedViews(next)
    persistSavedViews(next)
    setSelectedViewId('')
    toast.success('Saved view deleted.')
  }

  async function applyLegalHold(event) {
    event.preventDefault()
    if (!selectedRecord) return

    await runAction('APPLY_LEGAL_HOLD', {
      legalHoldUntil: toIsoOrNull(legalHoldUntil),
      reason: operationReason.trim() || 'Legal hold applied from the admin recovery center.'
    })
  }

  async function copyCorrelationId(value) {
    const correlationId = String(value || '').trim()
    if (!correlationId) {
      toast.error('No correlation ID is available for this event.')
      return
    }

    try {
      await navigator.clipboard.writeText(correlationId)
      toast.success('Correlation ID copied to clipboard.')
    } catch {
      toast.error('Clipboard write failed. Copy manually from the details panel.')
    }
  }

  function openIncident(incident) {
    setSelectedIncidentKey(incident.key)
    if (incident.source === 'JOB') {
      setSelectedJobId(incident.id)
      return
    }
    setSelectedAuditLogId(incident.id)
  }

  function exportIncidentsCsv() {
    if (!incidentFeed.length) {
      toast.error('No incident data is available to export.')
      return
    }

    const header = ['occurredAt', 'severity', 'source', 'title', 'subtitle', 'correlationId', 'id']
    const rows = incidentFeed.map((incident) => [
      incident.occurredAt,
      incident.severity,
      incident.source,
      incident.title,
      incident.subtitle,
      incident.correlationId || '',
      incident.id
    ])
    const csvLines = [header.join(','), ...rows.map((row) => row.map(csvCell).join(','))]
    downloadCsv(`recovery-incidents-${fileStamp()}.csv`, csvLines)
    toast.success('Incidents exported.')
  }

  function exportAuditPageCsv() {
    if (!filteredAuditEvents.length) {
      toast.error('No audit log data is available to export.')
      return
    }

    const header = ['occurredAt', 'actionType', 'actionStatus', 'entityType', 'entityId', 'actor', 'correlationId', 'message']
    const rows = filteredAuditEvents.map((entry) => [
      entry.occurredAt,
      entry.actionType,
      entry.actionStatus,
      entry.entityType,
      entry.entityId || '',
      entry.actor || '',
      entry.correlationId || '',
      entry.message || ''
    ])
    const csvLines = [header.join(','), ...rows.map((row) => row.map(csvCell).join(','))]
    downloadCsv(`recovery-audit-page-${fileStamp()}.csv`, csvLines)
    toast.success('Audit page exported.')
  }

  function exportSelectedAuditCsv() {
    if (!selectedAuditEvent) {
      toast.error('Select an audit event to export.')
      return
    }

    const header = ['occurredAt', 'actionType', 'actionStatus', 'entityType', 'entityId', 'actor', 'correlationId', 'message', 'metadataJson']
    const row = [
      selectedAuditEvent.occurredAt,
      selectedAuditEvent.actionType,
      selectedAuditEvent.actionStatus,
      selectedAuditEvent.entityType,
      selectedAuditEvent.entityId || '',
      selectedAuditEvent.actor || '',
      selectedAuditEvent.correlationId || '',
      selectedAuditEvent.message || '',
      selectedAuditEvent.metadataJson || ''
    ]
    const csvLines = [header.join(','), row.map(csvCell).join(',')]
    downloadCsv(`recovery-audit-${String(selectedAuditEvent.id || '').slice(0, 8) || 'event'}-${fileStamp()}.csv`, csvLines)
    toast.success('Audit event exported.')
  }

  if (loading) {
    return <Spinner label="Loading recovery center..." />
  }

  return (
    <div className="page recovery-center-page">
      <div className="page-head recovery-page-head">
        <div>
          <h2>Recovery Center</h2>
          <p>Govern destructive actions, enforce controls, restore versions, and monitor incidents with immutable recovery audit trails.</p>
        </div>
        <div className="recovery-head-meta">
          <span className="subtle-meta">Last sync: {lastRefreshAt ? formatDateTime(lastRefreshAt) : 'Not synced yet'}</span>
          <span className="subtle-meta">Stream: {streamStatus}</span>
          <label className="toggle">
            <input
              type="checkbox"
              checked={autoRefreshEnabled}
              onChange={(event) => setAutoRefreshEnabled(event.target.checked)}
            />
            <span>Auto refresh every 30s</span>
          </label>
        </div>
      </div>

      <section className="recovery-kpi-grid">
        <article className="metric-card recovery-kpi-card">
          <span className="kpi-label">Governed records</span>
          <strong>{governanceSummary.governedCount}</strong>
          <p>Records under governance controls.</p>
        </article>
        <article className="metric-card recovery-kpi-card is-risk">
          <span className="kpi-label">High risk</span>
          <strong>{governanceSummary.highRisk}</strong>
          <p>Overdue retention, trashed, or unverified backup states.</p>
        </article>
        <article className="metric-card recovery-kpi-card">
          <span className="kpi-label">Legal holds</span>
          <strong>{governanceSummary.legalHolds}</strong>
          <p>Records protected from purge by policy hold.</p>
        </article>
        <article className="metric-card recovery-kpi-card">
          <span className="kpi-label">Purge-ready</span>
          <strong>{governanceSummary.purgeReady}</strong>
          <p>Retention expired and backup verification complete.</p>
        </article>
        <article className="metric-card recovery-kpi-card is-incident">
          <span className="kpi-label">Open incidents</span>
          <strong>{governanceSummary.openIncidents}</strong>
          <p>{governanceSummary.failedJobs} job alerts · {governanceSummary.failedAuditEvents} audit alerts.</p>
        </article>
        <article className="metric-card recovery-kpi-card">
          <span className="kpi-label">Retention overdue</span>
          <strong>{governanceSummary.overdueRetention}</strong>
          <p>Records that have crossed retention without legal hold.</p>
        </article>
      </section>

      <section className="panel recovery-incident-panel">
        <div className="section-head">
          <div>
            <h3>Incident queue</h3>
            <p>Unified feed of failed jobs and error audit events. Select an incident to pivot into job or audit detail.</p>
          </div>
          <div className="inline-actions">
            <span className="badge badge-danger">{incidentFeed.length} open</span>
            <button className="btn btn-outline btn-sm" type="button" onClick={exportIncidentsCsv}>
              Export CSV
            </button>
          </div>
        </div>

        {incidentFeed.length ? (
          <div className="recovery-incident-list">
            {incidentFeed.slice(0, 12).map((incident) => (
              <button
                key={incident.key}
                type="button"
                className={`recovery-incident-item ${selectedIncidentKey === incident.key ? 'active' : ''}`}
                onClick={() => openIncident(incident)}
              >
                <span className={`badge ${severityBadgeClass(incident.severity)}`}>{incident.severity}</span>
                <span className="recovery-incident-title">{incident.title}</span>
                <span className="recovery-incident-subtitle">{incident.subtitle}</span>
                <span className="recovery-incident-meta">
                  {incident.source} · {formatDateTime(incident.occurredAt)}
                  {incident.correlationId ? ` · ${incident.correlationId}` : ''}
                </span>
              </button>
            ))}
          </div>
        ) : (
          <div className="recovery-empty">No active incidents. Recovery governance is currently healthy.</div>
        )}
      </section>

      <section className="panel recovery-approval-panel">
        <div className="section-head">
          <div>
            <h3>Approvals</h3>
            <p>4-eyes approval queue for high-impact actions and bulk destructive jobs.</p>
          </div>
          <div className="inline-actions">
            <span className="badge badge-warning">{approvalEvents.filter((item) => item.isPending).length} pending</span>
            <button className="btn btn-outline btn-sm" type="button" onClick={() => loadRecoveryApprovalsData({ nextPage: 0, nextSize: approvalsPage.size })}>
              Refresh
            </button>
          </div>
        </div>

        <div className="recovery-log-toolbar">
          <label className="recovery-log-search">
            Search approvals
            <input
              type="search"
              value={approvalFilters.query}
              onChange={(event) => setApprovalFilters((current) => ({ ...current, query: event.target.value }))}
              placeholder="Search entity, ticket, actor, reason, status"
            />
          </label>
          <label>
            Entity
            <select
              value={approvalFilters.entityType}
              onChange={(event) => setApprovalFilters((current) => ({ ...current, entityType: event.target.value }))}
            >
              <option value="">All</option>
              {ENTITY_TYPES.map((entityType) => (
                <option key={entityType} value={entityType}>{entityType}</option>
              ))}
            </select>
          </label>
          <label>
            Action
            <select
              value={approvalFilters.actionType}
              onChange={(event) => setApprovalFilters((current) => ({ ...current, actionType: event.target.value }))}
            >
              <option value="">All</option>
              {RECOVERY_ACTION_TYPES.map((actionType) => (
                <option key={actionType} value={actionType}>{actionType}</option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select
              value={approvalFilters.status}
              onChange={(event) => setApprovalFilters((current) => ({ ...current, status: event.target.value }))}
            >
              <option value="">All</option>
              {APPROVAL_STATUSES.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          </label>
        </div>

        <PaginationControls
          page={approvalsPage.page}
          totalPages={approvalsPage.totalPages}
          totalElements={approvalsPage.totalElements}
          first={approvalsPage.first}
          last={approvalsPage.last}
          pageSize={approvalsPage.size}
          pageSizeOptions={[10, 25, 50, 100]}
          onPageSizeChange={(nextSize) => {
            if (approvalsLoading) return
            void loadRecoveryApprovalsData({ nextPage: 0, nextSize })
          }}
          onPrev={() => {
            if (approvalsLoading) return
            void loadRecoveryApprovalsData({ nextPage: Math.max(0, Number(approvalsPage.page || 0) - 1), nextSize: approvalsPage.size })
          }}
          onNext={() => {
            if (approvalsLoading) return
            void loadRecoveryApprovalsData({ nextPage: Number(approvalsPage.page || 0) + 1, nextSize: approvalsPage.size })
          }}
          noun="approvals"
        />
        {approvalsLoading ? <p className="subtle-meta">Loading approvals...</p> : null}

        <div className="table-wrap">
          <table className="recovery-approval-table">
            <thead>
              <tr>
                <th>Requested</th>
                <th>Kind</th>
                <th>Entity</th>
                <th>Action</th>
                <th>Status</th>
                <th>Requested by</th>
                <th>Ticket</th>
              </tr>
            </thead>
            <tbody>
              {filteredApprovalEvents.length ? (
                filteredApprovalEvents.map((approval) => (
                  <tr
                    key={approval.id}
                    className={[
                      approval.isError ? 'row-error' : '',
                      String(approval.id) === String(selectedApprovalId) ? 'row-active' : ''
                    ].filter(Boolean).join(' ')}
                    onClick={() => setSelectedApprovalId(approval.id)}
                  >
                    <td>{formatDateTime(approval.requestedAt)}</td>
                    <td className="mono">{approval.requestKind === 'BULK_ACTION' ? `BULK (${approval.requestedItems || 0})` : 'ACTION'}</td>
                    <td className="mono">{approval.entityType}{approval.entityId ? `:${approval.entityId}` : ''}</td>
                    <td className="mono">{approval.actionType}</td>
                    <td>
                      <span className={`badge ${approval.isPending ? 'badge-warning' : approval.isError ? 'badge-danger' : 'badge-muted'}`}>{approval.status}</span>
                    </td>
                    <td>{approval.requestedBy || 'Unknown actor'}</td>
                    <td className="mono">{approval.changeTicket || '—'}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7" className="empty-row">No approval requests match the current filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {selectedApproval ? (
          <div className="recovery-approval-detail">
            <div className="recovery-meta-row">
              <strong>{selectedApproval.actionType} · {selectedApproval.entityType}{selectedApproval.entityId ? `:${selectedApproval.entityId}` : ''}</strong>
              <span className={`badge ${selectedApproval.isPending ? 'badge-warning' : selectedApproval.isError ? 'badge-danger' : 'badge-muted'}`}>{selectedApproval.status}</span>
            </div>
            <div className="subtle-meta">
              Requested {formatDateTime(selectedApproval.requestedAt)} · {selectedApproval.requestedBy || 'Unknown actor'}
              {selectedApproval.reviewedBy ? ` · Reviewed by ${selectedApproval.reviewedBy}` : ''}
            </div>
            {selectedApproval.reason ? <div className="recovery-approval-reason">{selectedApproval.reason}</div> : null}
            {selectedApproval.executionError ? <div className="recovery-approval-error">{selectedApproval.executionError}</div> : null}
            {selectedApproval.executedJobId ? (
              <div className="inline-actions">
                <button className="btn btn-outline btn-sm" type="button" onClick={() => setSelectedJobId(selectedApproval.executedJobId)}>
                  Open job
                </button>
              </div>
            ) : null}
            <label>
              Reviewer notes
              <textarea
                value={reviewerNotes}
                onChange={(event) => setReviewerNotes(event.target.value)}
                placeholder="Optional notes for compliance and audit."
              />
            </label>
            <div className="inline-actions">
              <button
                className="btn btn-primary btn-sm"
                type="button"
                onClick={() => approveApprovalRequest(selectedApproval)}
                disabled={working || selectedApproval.status !== 'PENDING'}
              >
                Approve & execute
              </button>
              <button
                className="btn btn-danger btn-sm"
                type="button"
                onClick={() => rejectApprovalRequest(selectedApproval)}
                disabled={working || selectedApproval.status !== 'PENDING'}
              >
                Reject
              </button>
            </div>
          </div>
        ) : null}
      </section>

      <div className="panel recovery-toolbar">
        <div className="filters">
          <label>
            Entity type
            <select
              value={filters.entityType}
              onChange={(event) => setFilters((current) => ({ ...current, entityType: event.target.value }))}
            >
              <option value="">All</option>
              {ENTITY_TYPES.map((entityType) => (
                <option key={entityType} value={entityType}>{entityType}</option>
              ))}
            </select>
          </label>
          <label>
            Lifecycle state
            <select
              value={filters.lifecycleState}
              onChange={(event) => setFilters((current) => ({ ...current, lifecycleState: event.target.value }))}
            >
              <option value="">All</option>
              {RECORD_STATES.map((state) => (
                <option key={state} value={state}>{state}</option>
              ))}
            </select>
          </label>
          <label>
            Risk
            <select
              value={filters.riskLevel}
              onChange={(event) => setFilters((current) => ({ ...current, riskLevel: event.target.value }))}
            >
              <option value="">All</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </label>
          <label>
            Search
            <input
              value={filters.query}
              onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))}
              placeholder="Search by id, display name, actor, or reason"
            />
          </label>
          <label className="toggle recovery-toggle">
            <input
              type="checkbox"
              checked={filters.overdueOnly}
              onChange={(event) => setFilters((current) => ({ ...current, overdueOnly: event.target.checked }))}
            />
            <span>Overdue retention only</span>
          </label>
        </div>
        <div className="inline-actions wrap">
          <button className="btn btn-outline" type="button" onClick={() => loadRecoveryRecords()}>
            Refresh
          </button>
          <button className="btn btn-primary" type="button" onClick={() => loadRecoveryRecords(selectedRecordKey)}>
            Apply filters
          </button>
          <label className="recovery-view-select">
            Saved view
            <select
              value={selectedViewId}
              onChange={(event) => {
                const id = event.target.value
                setSelectedViewId(id)
                if (id) applySavedView(id)
              }}
            >
              <option value="">None</option>
              {savedViews.map((view) => (
                <option key={view.id} value={view.id}>{view.name}</option>
              ))}
            </select>
          </label>
          <label className="recovery-view-name">
            Save as
            <input
              value={viewName}
              onChange={(event) => setViewName(event.target.value)}
              placeholder="My view"
            />
          </label>
          <button className="btn btn-outline" type="button" onClick={saveCurrentView}>
            Save view
          </button>
          <button
            className="btn btn-outline"
            type="button"
            onClick={() => deleteSavedView(selectedViewId)}
            disabled={!selectedViewId}
          >
            Delete view
          </button>
        </div>
      </div>

      <div className="recovery-layout">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Governed records</h3>
              <p>Showing {visibleRecords.length} of {recordsPage.totalElements || 0} governed records.</p>
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Entity</th>
                  <th>Lifecycle</th>
                  <th>Risk</th>
                  <th>Retention</th>
                  <th>Legal hold</th>
                  <th>Compliance</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {visibleRecords.length ? (
                  visibleRecords.map((record) => {
                    const key = `${record.entityType}:${record.entityId}`
                    const isSelected = selectedRecordKey === key
                    return (
                      <tr
                        key={key}
                        className={[
                          isSelected ? 'row-selected' : '',
                          record.riskLevel === 'HIGH' ? 'row-risk-high' : '',
                          record.retentionOverdue ? 'row-retention-overdue' : ''
                        ].filter(Boolean).join(' ')}
                        onClick={() => setSelectedRecordKey(key)}
                        role="button"
                        tabIndex={0}
                      >
                        <td>
                          <strong>{record.displayName || record.entityId}</strong>
                          <div className="subtle-meta recovery-code">{record.entityType} · {record.entityId}</div>
                        </td>
                        <td><span className="badge badge-muted">{record.lifecycleState}</span></td>
                        <td><span className={`badge ${riskBadgeClass(record.riskLevel)}`}>{record.riskLevel}</span></td>
                        <td>{formatDateTime(record.retentionUntil)}</td>
                        <td>
                          {record.legalHoldActive
                            ? <span className="badge badge-warning">Active hold</span>
                            : formatDateTime(record.legalHoldUntil)}
                        </td>
                        <td>
                          <div className="recovery-compliance-badges">
                            <span className={`badge ${record.backupVerified ? 'badge-success' : 'badge-warning'}`}>
                              {record.backupVerified ? 'Backup verified' : 'Backup pending'}
                            </span>
                            {record.anonymized ? <span className="badge badge-muted">Anonymized</span> : null}
                            {record.purgeReady ? <span className="badge badge-info">Purge-ready</span> : null}
                          </div>
                        </td>
                        <td>{formatDateTime(record.updatedAt)}</td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td colSpan="7" className="empty-row">No governed records match the current filters.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>{selectedRecord ? 'Record controls' : 'Select a record'}</h3>
              <p>{selectedRecord ? summarizeRecord(selectedRecord) : 'Choose a governed record to review versions and apply recovery actions.'}</p>
            </div>
            {selectedRecord ? <span className={`badge ${riskBadgeClass(selectedRecord.riskLevel)}`}>{selectedRecord.riskLevel}</span> : null}
          </div>

          {selectedRecord ? (
            <>
              <div className="recovery-control-grid">
                <label>
                  Operation reason
                  <textarea
                    value={operationReason}
                    onChange={(event) => setOperationReason(event.target.value)}
                    placeholder="Document why this operation is required (minimum 12 chars for destructive actions)"
                  />
                </label>
                <label>
                  Change ticket
                  <input
                    value={operationTicket}
                    onChange={(event) => setOperationTicket(event.target.value)}
                    placeholder="INC-2026-0042"
                  />
                </label>
              </div>

              <p className="subtle-meta recovery-control-hint">
                High-impact actions require a change ticket. Permanent delete is restricted to users with purge capability.
              </p>

              <div className="recovery-actions">
                <button className="btn btn-outline btn-sm" type="button" onClick={() => runAction('DEACTIVATE')} disabled={working}>
                  Inactivate
                </button>
                <button className="btn btn-outline btn-sm" type="button" onClick={() => runAction('ARCHIVE')} disabled={working}>
                  Archive
                </button>
                <button className="btn btn-outline btn-sm" type="button" onClick={() => runAction('TRASH')} disabled={working}>
                  Trash
                </button>
                <button className="btn btn-primary btn-sm" type="button" onClick={() => runAction('RESTORE')} disabled={working}>
                  Restore
                </button>
                <button className="btn btn-outline btn-sm" type="button" onClick={() => runAction('UNDO_TRASH')} disabled={working}>
                  Undo trash
                </button>
                <button className="btn btn-outline btn-sm" type="button" onClick={() => runAction('RELEASE_LEGAL_HOLD')} disabled={working}>
                  Release legal hold
                </button>
                <button className="btn btn-danger btn-sm" type="button" onClick={() => runAction('ANONYMIZE')} disabled={working}>
                  GDPR anonymize
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  type="button"
                  onClick={() => runAction('HARD_DELETE')}
                  disabled={working || !canPurge}
                  title={canPurge ? 'Permanently delete after verified backup.' : 'Your role cannot permanently delete records.'}
                >
                  Permanent delete
                </button>
              </div>

              <form className="stack-form recovery-legalhold-form" onSubmit={applyLegalHold}>
                <div className="section-head compact">
                  <div>
                    <h3>Legal hold</h3>
                    <p>Protect the record from retention-based purge until the specified timestamp.</p>
                  </div>
                </div>
                <label>
                  Hold until
                  <input
                    type="datetime-local"
                    value={legalHoldUntil}
                    onChange={(event) => setLegalHoldUntil(event.target.value)}
                  />
                </label>
                <div className="inline-actions">
                  <button className="btn btn-primary btn-sm" type="submit" disabled={working}>
                    Apply legal hold
                  </button>
                </div>
              </form>

              <div className="section-head compact recovery-version-head">
                <div>
                  <h3>Version history</h3>
                  <p>Use point-in-time restore to replay the exact captured snapshot.</p>
                </div>
              </div>

              {diffSelection ? (
                <div className="recovery-diff-panel">
                  <div className="recovery-meta-row">
                    <div>
                      <strong>
                        Diff: v{diffSelection.newer?.versionNumber} vs v{diffSelection.older?.versionNumber}
                      </strong>
                      <div className="subtle-meta">
                        {filteredDiffChanges.length} change(s){diffChanges.length >= 200 ? ' (truncated)' : ''} · Filter paths to narrow results
                      </div>
                    </div>
                    <div className="inline-actions">
                      <button
                        className="btn btn-outline btn-sm"
                        type="button"
                        onClick={() => { setDiffSelection(null); setDiffQuery('') }}
                      >
                        Close diff
                      </button>
                    </div>
                  </div>

                  <div className="recovery-log-toolbar">
                    <label className="recovery-log-search">
                      Filter paths
                      <input
                        type="search"
                        value={diffQuery}
                        onChange={(event) => setDiffQuery(event.target.value)}
                        placeholder="e.g. pricing.amount"
                      />
                    </label>
                  </div>

                  <div className="table-wrap">
                    <table className="recovery-diff-table">
                      <thead>
                        <tr>
                          <th>Path</th>
                          <th>Before</th>
                          <th>After</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredDiffChanges.length ? (
                          filteredDiffChanges.slice(0, 200).map((change) => (
                            <tr key={`${change.path}:${formatDiffValue(change.before)}:${formatDiffValue(change.after)}`}>
                              <td className="mono">{change.path || '(root)'}</td>
                              <td className="mono">{formatDiffValue(change.before)}</td>
                              <td className="mono">{formatDiffValue(change.after)}</td>
                            </tr>
                          ))
                        ) : (
                          <tr>
                            <td colSpan="3" className="empty-row">No changes match the current diff filter.</td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : null}

              <div className="recovery-version-list">
                {versions.length ? (
                  versions.map((version, index) => {
                    const older = versions[index + 1] || null
                    return (
                    <article key={version.id} className="recovery-version-item">
                      <div className="recovery-meta-row">
                        <div>
                          <strong>Version {version.versionNumber}</strong>
                          <div className="subtle-meta">{version.actionType} · {version.lifecycleStateAfter}</div>
                        </div>
                        <div className="inline-actions">
                          <span className={`badge ${version.backupSnapshot ? 'badge-warning' : 'badge-muted'}`}>
                            {version.backupSnapshot ? 'Backup' : 'Version'}
                          </span>
                          <button
                            className="btn btn-outline btn-sm"
                            type="button"
                            onClick={() => {
                              if (!older) return
                              setDiffSelection({ newer: version, older })
                              setDiffQuery('')
                            }}
                            disabled={!older}
                            title={older ? 'Compare this version with the next older snapshot.' : 'No older snapshot available.'}
                          >
                            Diff
                          </button>
                          <button className="btn btn-outline btn-sm" type="button" onClick={() => restoreVersion(version)} disabled={working}>
                            Restore point
                          </button>
                        </div>
                      </div>
                      <div className="subtle-meta">
                        Captured {formatDateTime(version.capturedAt)}{version.actor ? ` · ${version.actor}` : ''}
                      </div>
                      {version.reason ? <div>{version.reason}</div> : null}
                      <pre>{formatJson(version.snapshotJson)}</pre>
                    </article>
                    )
                  })
                ) : (
                  <div className="recovery-empty">No versions are available for the selected record yet.</div>
                )}
              </div>
            </>
          ) : (
            <div className="recovery-empty">Select a governed record to view version history and destructive-action controls.</div>
          )}
        </section>
      </div>

      <div className="recovery-grid">
        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Bulk actions</h3>
              <p>Validate or queue governed lifecycle actions for multiple entity ids.</p>
            </div>
          </div>

          <form className="stack-form recovery-bulk-form" onSubmit={submitBulk}>
            <div className="filters">
              <label>
                Entity type
                <select
                  value={bulkForm.entityType}
                  onChange={(event) => setBulkForm((current) => ({ ...current, entityType: event.target.value }))}
                >
                  {ENTITY_TYPES.map((entityType) => (
                    <option key={entityType} value={entityType}>{entityType}</option>
                  ))}
                </select>
              </label>
              <label>
                Action
                <select
                  value={bulkForm.actionType}
                  onChange={(event) => setBulkForm((current) => ({ ...current, actionType: event.target.value }))}
                >
                  {BULK_ACTIONS.map((action) => (
                    <option key={action} value={action}>{action}</option>
                  ))}
                </select>
              </label>
              <label>
                Retention days
                <input
                  value={bulkForm.retentionDays}
                  onChange={(event) => setBulkForm((current) => ({ ...current, retentionDays: event.target.value }))}
                />
              </label>
            </div>
            <label>
              Entity ids
              <textarea
                value={bulkForm.entityIds}
                onChange={(event) => setBulkForm((current) => ({ ...current, entityIds: event.target.value }))}
                placeholder="Enter one entity id per line"
              />
            </label>
            <label>
              Reason
              <textarea
                value={bulkForm.reason}
                onChange={(event) => setBulkForm((current) => ({ ...current, reason: event.target.value }))}
                placeholder="Document why this bulk action is required"
              />
            </label>
            <label>
              Change ticket
              <input
                value={bulkForm.changeTicket}
                onChange={(event) => setBulkForm((current) => ({ ...current, changeTicket: event.target.value }))}
                placeholder="INC-2026-0042"
              />
            </label>
            <label>
              Legal hold until
              <input
                type="datetime-local"
                value={bulkForm.legalHoldUntil}
                onChange={(event) => setBulkForm((current) => ({ ...current, legalHoldUntil: event.target.value }))}
              />
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={bulkForm.dryRun}
                onChange={(event) => setBulkForm((current) => ({ ...current, dryRun: event.target.checked }))}
              />
              <span>Dry run only</span>
            </label>
            <div className="inline-actions">
              <button className="btn btn-primary" type="submit" disabled={working}>
                Queue bulk job
              </button>
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="section-head">
            <div>
              <h3>Bulk jobs</h3>
              <p>Monitor orchestration pipelines for destructive actions and run-time validation.</p>
            </div>
          </div>

          <div className="recovery-log-toolbar">
            <label className="recovery-log-search">
              Search jobs
              <input
                type="search"
                value={jobFilters.query}
                onChange={(event) => setJobFilters((current) => ({ ...current, query: event.target.value }))}
                placeholder="Search status, action, actor, summary"
              />
            </label>
            <label>
              Entity
              <select
                value={jobFilters.entityType}
                onChange={(event) => setJobFilters((current) => ({ ...current, entityType: event.target.value }))}
              >
                <option value="">All</option>
                {ENTITY_TYPES.map((entityType) => (
                  <option key={entityType} value={entityType}>{entityType}</option>
                ))}
              </select>
            </label>
            <label>
              Status
              <select
                value={jobFilters.status}
                onChange={(event) => setJobFilters((current) => ({ ...current, status: event.target.value }))}
              >
                <option value="">All</option>
                {JOB_STATUSES.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
            <label className="checkbox-row recovery-errors-toggle">
              <input
                type="checkbox"
                checked={jobFilters.onlyErrors}
                onChange={(event) => setJobFilters((current) => ({ ...current, onlyErrors: event.target.checked }))}
              />
              Errors only
            </label>
          </div>

          <PaginationControls
            page={jobsPage.page}
            totalPages={jobsPage.totalPages}
            totalElements={jobsPage.totalElements}
            first={jobsPage.first}
            last={jobsPage.last}
            pageSize={jobsPage.size}
            pageSizeOptions={[10, 25, 50, 100]}
            onPageSizeChange={(nextSize) => {
              if (jobsLoading) return
              void loadRecoveryJobsData({ nextPage: 0, nextSize })
            }}
            onPrev={() => {
              if (jobsLoading) return
              void loadRecoveryJobsData({ nextPage: Math.max(0, Number(jobsPage.page || 0) - 1), nextSize: jobsPage.size })
            }}
            onNext={() => {
              if (jobsLoading) return
              void loadRecoveryJobsData({ nextPage: Number(jobsPage.page || 0) + 1, nextSize: jobsPage.size })
            }}
            noun="jobs"
          />
          {jobsLoading ? <p className="subtle-meta">Loading jobs...</p> : null}

          <div className="table-wrap">
            <table className="recovery-job-table">
              <thead>
                <tr>
                  <th>Updated</th>
                  <th>Entity</th>
                  <th>Action</th>
                  <th>Status</th>
                  <th>Requested by</th>
                  <th>Progress</th>
                </tr>
              </thead>
              <tbody>
                {filteredJobEvents.length ? (
                  filteredJobEvents.map((job) => (
                    <tr
                      key={job.id}
                      className={[
                        job.isError ? 'row-error' : '',
                        String(job.id) === String(selectedJobId) ? 'row-active' : ''
                      ].filter(Boolean).join(' ')}
                      onClick={() => setSelectedJobId(job.id)}
                    >
                      <td>{formatDateTime(job.updatedAt)}</td>
                      <td className="mono">{job.entityType}</td>
                      <td className="mono">{job.actionType}</td>
                      <td>
                        <span className={`badge ${job.isError ? 'badge-danger' : 'badge-muted'}`}>{job.status}</span>
                      </td>
                      <td>{job.requestedBy || 'Unknown actor'}</td>
                      <td className="mono">
                        {job.processedItems}/{job.totalItems} ({asProgressPercent(job)}%) | OK {job.successItems} | FAIL {job.failedItems}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" className="empty-row">No bulk jobs match the current filters.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {selectedJobEvent ? (() => {
            const status = String(selectedJobEvent.status || '').toUpperCase()
            const canCancel = ['VALIDATING', 'QUEUED', 'RUNNING'].includes(status)
            const canRetry = ['FAILED', 'PARTIAL_SUCCESS', 'CANCELLED'].includes(status)
            const items = parseJsonArray(selectedJobEvent.resultSummaryJson)
            const failedItems = items.filter((item) => String(item?.status || '').toUpperCase() === 'FAILED')

            return (
              <div className="recovery-job-detail">
                <div className="recovery-meta-row">
                  <strong>{selectedJobEvent.actionType} · {selectedJobEvent.entityType}</strong>
                  <span className={`badge ${selectedJobEvent.isError ? 'badge-danger' : 'badge-success'}`}>
                    {selectedJobEvent.status || 'UNKNOWN'}
                  </span>
                </div>
                <div className="subtle-meta">
                  Requested by {selectedJobEvent.requestedBy || 'Unknown actor'} · Updated {formatDateTime(selectedJobEvent.updatedAt)}
                </div>

                <div className="inline-actions wrap">
                  <button
                    className="btn btn-outline btn-sm"
                    type="button"
                    onClick={() => cancelSelectedJob(selectedJobEvent)}
                    disabled={working || !canCancel}
                    title={canCancel ? 'Request cancellation for this job.' : 'Only queued or running jobs can be cancelled.'}
                  >
                    Cancel job
                  </button>
                  <button
                    className="btn btn-outline btn-sm"
                    type="button"
                    onClick={() => retrySelectedJob(selectedJobEvent, { failedOnly: true })}
                    disabled={working || !canRetry}
                    title={canRetry ? 'Retry failed items from this job.' : 'Retry is available after completion.'}
                  >
                    Retry failed
                  </button>
                  <button
                    className="btn btn-outline btn-sm"
                    type="button"
                    onClick={() => retrySelectedJob(selectedJobEvent, { failedOnly: false })}
                    disabled={working || !canRetry}
                  >
                    Retry all
                  </button>
                  <button
                    className="btn btn-outline btn-sm"
                    type="button"
                    onClick={() => downloadFailureReport(selectedJobEvent)}
                    disabled={working || !failedItems.length}
                    title={failedItems.length ? 'Download a CSV report for failed items.' : 'No failed items to export.'}
                  >
                    Failure report
                  </button>
                </div>

                {selectedJobEvent.errorSummary ? <div className="recovery-job-error">{selectedJobEvent.errorSummary}</div> : null}

                {failedItems.length ? (
                  <div className="recovery-failure-block">
                    <div className="section-head compact">
                      <div>
                        <h3>Failed items</h3>
                        <p>{failedItems.length} items failed in this job run.</p>
                      </div>
                    </div>
                    <div className="table-wrap">
                      <table className="recovery-failure-table">
                        <thead>
                          <tr>
                            <th>Entity id</th>
                            <th>Error</th>
                          </tr>
                        </thead>
                        <tbody>
                          {failedItems.slice(0, 50).map((item) => (
                            <tr key={`${item.entityId}:${item.error || ''}`}>
                              <td className="mono">{item.entityId}</td>
                              <td>{item.error || 'Unknown error'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {failedItems.length > 50 ? <p className="subtle-meta">Showing first 50 failures. Download the report for the full list.</p> : null}
                  </div>
                ) : null}

                <pre className="recovery-json">
                  {formatJson(selectedJobEvent.resultSummaryJson || selectedJobEvent.validationSummaryJson)}
                </pre>
              </div>
            )
          })() : null}
        </section>
      </div>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>Audit logs</h3>
            <p>Immutable audit entries for destructive and recovery actions.</p>
          </div>
        </div>

        <div className="recovery-log-toolbar">
          <label className="recovery-log-search">
            Search audit events
            <input
              type="search"
              value={auditFilters.query}
              onChange={(event) => setAuditFilters((current) => ({ ...current, query: event.target.value }))}
              placeholder="Search action, entity, actor, message, correlation"
            />
          </label>
          <label>
            Entity
            <select
              value={auditFilters.entityType}
              onChange={(event) => setAuditFilters((current) => ({ ...current, entityType: event.target.value }))}
            >
              <option value="">All</option>
              {ENTITY_TYPES.map((entityType) => (
                <option key={entityType} value={entityType}>{entityType}</option>
              ))}
            </select>
          </label>
          <label>
            Action
            <select
              value={auditFilters.actionType}
              onChange={(event) => setAuditFilters((current) => ({ ...current, actionType: event.target.value }))}
            >
              <option value="">All</option>
              {RECOVERY_ACTION_TYPES.map((actionType) => (
                <option key={actionType} value={actionType}>{actionType}</option>
              ))}
            </select>
          </label>
          <label>
            Status
            <input
              value={auditFilters.actionStatus}
              onChange={(event) => setAuditFilters((current) => ({ ...current, actionStatus: event.target.value }))}
              placeholder="SUCCESS / FAILED / REJECTED"
            />
          </label>
          <label className="checkbox-row recovery-errors-toggle">
            <input
              type="checkbox"
              checked={auditFilters.onlyErrors}
              onChange={(event) => setAuditFilters((current) => ({ ...current, onlyErrors: event.target.checked }))}
            />
            Errors only
          </label>
          <div className="inline-actions">
            <button
              className="btn btn-outline btn-sm"
              type="button"
              onClick={exportAuditPageCsv}
              disabled={auditLoading || !filteredAuditEvents.length}
              title={filteredAuditEvents.length ? 'Export current audit page to CSV.' : 'No audit data available.'}
            >
              Export page
            </button>
            <button
              className="btn btn-outline btn-sm"
              type="button"
              onClick={exportSelectedAuditCsv}
              disabled={auditLoading || !selectedAuditEvent}
              title={selectedAuditEvent ? 'Export selected audit event to CSV.' : 'Select an audit event first.'}
            >
              Export selected
            </button>
          </div>
        </div>

        <PaginationControls
          page={auditLogsPage.page}
          totalPages={auditLogsPage.totalPages}
          totalElements={auditLogsPage.totalElements}
          first={auditLogsPage.first}
          last={auditLogsPage.last}
          pageSize={auditLogsPage.size}
          pageSizeOptions={[10, 25, 50, 100]}
          onPageSizeChange={(nextSize) => {
            if (auditLoading) return
            void loadRecoveryAuditLogsData({ nextPage: 0, nextSize })
          }}
          onPrev={() => {
            if (auditLoading) return
            void loadRecoveryAuditLogsData({ nextPage: Math.max(0, Number(auditLogsPage.page || 0) - 1), nextSize: auditLogsPage.size })
          }}
          onNext={() => {
            if (auditLoading) return
            void loadRecoveryAuditLogsData({ nextPage: Number(auditLogsPage.page || 0) + 1, nextSize: auditLogsPage.size })
          }}
          noun="events"
        />
        {auditLoading ? <p className="subtle-meta">Loading audit logs...</p> : null}

        <div className="recovery-audit-workspace">
          <aside className="recovery-audit-timeline">
            {auditTimelineGroups.length ? (
              auditTimelineGroups.map((group) => (
                <div key={group.key} className="recovery-timeline-group">
                  <p className="recovery-timeline-date">{group.label}</p>
                  <ul className="recovery-timeline-list">
                    {group.entries.map((entry) => (
                      <li key={entry.id}>
                        <button
                          type="button"
                          className={`recovery-timeline-item ${String(selectedAuditLogId) === String(entry.id) ? 'active' : ''} ${entry.isError ? 'is-error' : ''}`}
                          onClick={() => setSelectedAuditLogId(entry.id)}
                        >
                          <span className="recovery-timeline-time">{formatTimelineTime(entry.occurredAt)}</span>
                          <span className="recovery-timeline-action">{entry.actionType}</span>
                          <span className="recovery-timeline-entity">{entry.entityType} | {entry.entityId || 'n/a'}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              ))
            ) : (
              <div className="recovery-empty">No recovery audit logs match the current filters.</div>
            )}
          </aside>

          <div className="recovery-audit-main">
            <div className="table-wrap">
              <table className="recovery-audit-table">
                <thead>
                  <tr>
                    <th>Occurred</th>
                    <th>Action</th>
                    <th>Entity</th>
                    <th>Actor</th>
                    <th>Status</th>
                    <th>Correlation</th>
                    <th>Message</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAuditEvents.length ? (
                    filteredAuditEvents.map((entry) => (
                      <tr
                        key={entry.id}
                        className={[
                          String(selectedAuditLogId) === String(entry.id) ? 'row-active' : '',
                          entry.isError ? 'row-error' : ''
                        ].filter(Boolean).join(' ')}
                        onClick={() => setSelectedAuditLogId(entry.id)}
                      >
                        <td>{formatDateTime(entry.occurredAt)}</td>
                        <td className="mono">{entry.actionType}</td>
                        <td className="mono">{entry.entityType} | {entry.entityId || 'n/a'}</td>
                        <td>{entry.actor || 'Unknown actor'}</td>
                        <td>
                          <span className={`badge ${entry.isError ? 'badge-danger' : 'badge-success'}`}>{entry.actionStatus || 'UNKNOWN'}</span>
                        </td>
                        <td className="mono">{entry.correlationId || '—'}</td>
                        <td>{entry.message || 'No audit message recorded.'}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="7" className="empty-row">No recovery audit logs are available yet.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {selectedAuditEvent ? (
              <div className="recovery-audit-detail">
                <div className="recovery-meta-row">
                  <strong>{selectedAuditEvent.actionType}</strong>
                  <div className="inline-actions">
                    <span className={`badge ${selectedAuditEvent.isError ? 'badge-danger' : 'badge-success'}`}>
                      {selectedAuditEvent.actionStatus || 'UNKNOWN'}
                    </span>
                    {selectedAuditEvent.correlationId ? (
                      <>
                        <button className="btn btn-outline btn-sm" type="button" onClick={() => copyCorrelationId(selectedAuditEvent.correlationId)}>
                          Copy correlation
                        </button>
                        {resolveLogsUrl(selectedAuditEvent.correlationId) ? (
                          <a
                            className="btn btn-outline btn-sm"
                            href={resolveLogsUrl(selectedAuditEvent.correlationId)}
                            target="_blank"
                            rel="noreferrer"
                          >
                            View in logs
                          </a>
                        ) : null}
                      </>
                    ) : null}
                  </div>
                </div>
                <div className="subtle-meta">
                  {selectedAuditEvent.entityType} | {selectedAuditEvent.entityId || 'n/a'} | {selectedAuditEvent.actor || 'Unknown actor'}
                </div>
                <div>{selectedAuditEvent.message || 'No audit message recorded.'}</div>
                <pre className="recovery-json">{formatJson(selectedAuditEvent.metadataJson)}</pre>
              </div>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  )
}
