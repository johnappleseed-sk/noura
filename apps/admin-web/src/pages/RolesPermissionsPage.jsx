/**
 * Module: Roles & Permissions Page
 * Purpose: Provide enterprise RBAC governance workflows for role management and permission assignment.
 * Responsibilities:
 * - Render grouped permission cards with module and global select-all controls.
 * - Create/update/deactivate roles and replace role grants.
 * - Apply permission presets to target roles.
 * - Assign roles to users (single and bulk) and review RBAC audit logs.
 * Related modules:
 * - shared/api/endpoints/adminAuthorizationApi.js
 * - shared/api/endpoints/adminApi.js
 * - backend AdminAuthorizationController
 */

import { useEffect, useMemo, useRef, useState } from 'react'
import { listAdminUsers } from '../shared/api/endpoints/adminApi'
import {
  applyAdminRolePermissionPreset,
  bulkReplaceAdminUserRoleAssignments,
  createAdminRole,
  deleteAdminBulkUserRoleView,
  deactivateAdminRole,
  getAdminAuthorizationMatrix,
  getAdminUserRoleAssignments,
  listAdminAuthorizationAuditLogs,
  listAdminBulkUserRoleViews,
  listAdminPermissionPresets,
  listAdminPermissions,
  listAdminRoles,
  previewBulkAdminUserRoleAssignments,
  replaceAdminRolePermissions,
  replaceAdminUserRoleAssignments,
  upsertAdminBulkUserRoleView,
  updateAdminRole
} from '../shared/api/endpoints/adminAuthorizationApi'
import { Spinner } from '../shared/ui/Spinner'
import { useConfirmDialog } from '../shared/ui/ConfirmDialogProvider'
import { useToastFeedback } from '../shared/ui/useToastFeedback'
import '../styles/pages/RolesPermissionsPage.css'

const DEFAULT_ROLE_FORM = {
  code: '',
  label: '',
  description: '',
  assignable: true,
  active: true
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function permissionKey(scope, action) {
  return `${normalizeText(scope)}.${normalizeText(action)}`
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function formatOccurredAt(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString()
}

function truncate(value, max = 160) {
  const text = String(value || '')
  if (!text) return ''
  if (text.length <= max) return text
  return `${text.slice(0, max)}...`
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

function isAuthorizationAuditError(event) {
  const outcome = normalizeText(event?.outcome)
  if (outcome && !['success', 'ok', 'completed'].includes(outcome)) {
    return true
  }
  const actionType = normalizeText(event?.actionType)
  return ['fail', 'error', 'deny', 'reject', 'timeout', 'abort'].some((token) => actionType.includes(token))
}

function humanizeToken(value) {
  const normalized = normalizeText(value)
  if (!normalized) return ''
  return normalized
    .replaceAll('.', ' ')
    .replaceAll('_', ' ')
    .split(' ')
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}

function roleScopeActions(role, scopeCode) {
  const grants = role?.grants
  if (!grants || typeof grants !== 'object') {
    return []
  }
  const actions = grants[scopeCode]
  return Array.isArray(actions) ? actions : []
}

function buildPermissionDraft(role, permissions) {
  if (!role || !Array.isArray(permissions)) {
    return {}
  }
  const draft = {}
  for (const permission of permissions) {
    const key = permissionKey(permission.scope, permission.action)
    const actions = roleScopeActions(role, permission.scope)
    draft[key] = actions.map((action) => normalizeText(action)).includes(normalizeText(permission.action))
  }
  return draft
}

function draftToGrants(permissionDraft, permissions) {
  const grants = {}
  for (const permission of permissions) {
    const key = permissionKey(permission.scope, permission.action)
    if (!permissionDraft[key]) continue
    if (!grants[permission.scope]) {
      grants[permission.scope] = []
    }
    grants[permission.scope].push(permission.action)
  }
  return grants
}

function moduleGroupLabel(moduleGroup) {
  const group = normalizeText(moduleGroup)
  if (!group) return 'Other'
  if (group === 'core') return 'Core'
  if (group === 'commerce') return 'Commerce'
  if (group === 'operations') return 'Operations'
  if (group === 'finance') return 'Finance'
  if (group === 'growth') return 'Marketing & Growth'
  if (group === 'insights') return 'Analytics & Reports'
  if (group === 'supply_chain') return 'Supply Chain'
  if (group === 'governance') return 'Governance'
  return humanizeToken(group)
}

function csvCell(value) {
  if (value === null || value === undefined) return ''
  const text = String(value)
  return `"${text.replaceAll('"', '""')}"`
}

function parseCsvLine(line) {
  const values = []
  let current = ''
  let inQuotes = false
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    const next = line[index + 1]
    if (char === '"' && inQuotes && next === '"') {
      current += '"'
      index += 1
      continue
    }
    if (char === '"') {
      inQuotes = !inQuotes
      continue
    }
    if (char === ',' && !inQuotes) {
      values.push(current.trim())
      current = ''
      continue
    }
    current += char
  }
  values.push(current.trim())
  return values
}

export function RolesPermissionsPage() {
  const confirm = useConfirmDialog()

  const [loading, setLoading] = useState(true)
  const [loadingUserRoles, setLoadingUserRoles] = useState(false)
  const [savingRole, setSavingRole] = useState(false)
  const [savingPermissions, setSavingPermissions] = useState(false)
  const [applyingPreset, setApplyingPreset] = useState(false)
  const [assigningUserRoles, setAssigningUserRoles] = useState(false)
  const [bulkAssigningUserRoles, setBulkAssigningUserRoles] = useState(false)
  const [previewingBulkAssignments, setPreviewingBulkAssignments] = useState(false)
  const [savingBulkView, setSavingBulkView] = useState(false)
  const [deletingBulkView, setDeletingBulkView] = useState(false)
  const [importingBulkCsv, setImportingBulkCsv] = useState(false)
  const [loadingAuditLogs, setLoadingAuditLogs] = useState(false)

  const [error, setError] = useState('')
  const [flash, setFlash] = useState('')
  useToastFeedback({ successMessage: flash, errorMessage: error })

  const [matrix, setMatrix] = useState(null)
  const [permissions, setPermissions] = useState([])
  const [permissionPresets, setPermissionPresets] = useState([])
  const [roles, setRoles] = useState([])
  const [users, setUsers] = useState([])
  const [auditLogsPage, setAuditLogsPage] = useState({ content: [], totalElements: 0 })
  const [auditQuery, setAuditQuery] = useState('')
  const [auditOutcomeFilter, setAuditOutcomeFilter] = useState('')
  const [auditOnlyErrors, setAuditOnlyErrors] = useState(false)
  const [selectedAuditEventId, setSelectedAuditEventId] = useState('')

  const [query, setQuery] = useState('')
  const [moduleGroupFilter, setModuleGroupFilter] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)

  const [selectedRoleId, setSelectedRoleId] = useState('')
  const [roleForm, setRoleForm] = useState(DEFAULT_ROLE_FORM)
  const [permissionDraft, setPermissionDraft] = useState({})
  const [selectedPresetCode, setSelectedPresetCode] = useState('')

  const [selectedUserId, setSelectedUserId] = useState('')
  const [userRoleDraft, setUserRoleDraft] = useState([])
  const [bulkUserQuery, setBulkUserQuery] = useState('')
  const [bulkSelectedUserIds, setBulkSelectedUserIds] = useState([])
  const [bulkRoleDraft, setBulkRoleDraft] = useState([])
  const [bulkPreview, setBulkPreview] = useState(null)
  const [bulkImportReport, setBulkImportReport] = useState(null)
  const [savedBulkViews, setSavedBulkViews] = useState([])
  const [bulkViewNameInput, setBulkViewNameInput] = useState('')
  const [selectedBulkViewId, setSelectedBulkViewId] = useState('')

  const bulkImportInputRef = useRef(null)
  const hasLoadedAuditRef = useRef(false)

  async function loadAuditLogs() {
    setLoadingAuditLogs(true)
    try {
      const page = await listAdminAuthorizationAuditLogs({
        page: 0,
        size: 20,
        sortBy: 'occurredAt',
        direction: 'desc',
        query: auditQuery.trim() || undefined,
        outcome: auditOutcomeFilter || undefined,
        errorsOnly: auditOnlyErrors || undefined
      })
      setAuditLogsPage(page || { content: [], totalElements: 0 })
    } catch (_) {
      setAuditLogsPage({ content: [], totalElements: 0 })
    } finally {
      setLoadingAuditLogs(false)
    }
  }

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [nextMatrix, nextPermissions, nextPresets, nextRoles, userPage, nextAuditLogs, nextBulkViews] = await Promise.all([
        getAdminAuthorizationMatrix(),
        listAdminPermissions(),
        listAdminPermissionPresets().catch(() => []),
        listAdminRoles(),
        listAdminUsers({ page: 0, size: 100, sortBy: 'email', direction: 'asc' }).catch(() => ({ content: [] })),
        listAdminAuthorizationAuditLogs({
          page: 0,
          size: 20,
          sortBy: 'occurredAt',
          direction: 'desc',
          query: auditQuery.trim() || undefined,
          outcome: auditOutcomeFilter || undefined,
          errorsOnly: auditOnlyErrors || undefined
        }).catch(() => ({ content: [] })),
        listAdminBulkUserRoleViews().catch(() => [])
      ])

      setMatrix(nextMatrix || null)
      setPermissions(Array.isArray(nextPermissions) ? nextPermissions : [])
      setPermissionPresets(Array.isArray(nextPresets) ? nextPresets : [])
      setRoles(Array.isArray(nextRoles) ? nextRoles : [])
      setUsers(Array.isArray(userPage?.content) ? userPage.content : [])
      setAuditLogsPage(nextAuditLogs || { content: [], totalElements: 0 })

      const normalizedViews = Array.isArray(nextBulkViews) ? nextBulkViews : []
      setSavedBulkViews(normalizedViews)
      setSelectedBulkViewId((current) =>
        normalizedViews.some((view) => String(view.id) === String(current)) ? current : ''
      )
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load authorization management data.'))
      setMatrix(null)
      setPermissions([])
      setPermissionPresets([])
      setRoles([])
      setUsers([])
      setAuditLogsPage({ content: [], totalElements: 0 })
      setSavedBulkViews([])
      setSelectedBulkViewId('')
    } finally {
      hasLoadedAuditRef.current = true
      setLoading(false)
    }
  }

  async function refreshRoleData() {
    const [nextMatrix, nextPresets, nextRoles] = await Promise.all([
      getAdminAuthorizationMatrix().catch(() => null),
      listAdminPermissionPresets().catch(() => []),
      listAdminRoles()
    ])

    if (nextMatrix) {
      setMatrix(nextMatrix)
    }
    setPermissionPresets(Array.isArray(nextPresets) ? nextPresets : [])
    setRoles(Array.isArray(nextRoles) ? nextRoles : [])
    await loadAuditLogs()
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selectedRole = useMemo(
    () => roles.find((role) => String(role.id) === String(selectedRoleId)) || null,
    [roles, selectedRoleId]
  )

  const scopes = useMemo(() => {
    if (Array.isArray(matrix?.scopes) && matrix.scopes.length) {
      return matrix.scopes
    }
    const uniqueScopes = Array.from(new Set(permissions.map((permission) => permission.scope)))
    return uniqueScopes.map((scope) => ({ scope, label: humanizeToken(scope), description: '' }))
  }, [matrix, permissions])

  const scopeMetaByCode = useMemo(() => {
    const map = new Map()
    for (const scope of scopes) {
      map.set(normalizeText(scope.scope), scope)
    }
    return map
  }, [scopes])

  const permissionModules = useMemo(() => {
    const modulesByScope = new Map()

    for (const permission of permissions) {
      const scopeCode = normalizeText(permission.scope)
      if (!scopeCode) continue

      const existing = modulesByScope.get(scopeCode)
      const scopeMeta = scopeMetaByCode.get(scopeCode)

      if (!existing) {
        modulesByScope.set(scopeCode, {
          scope: permission.scope,
          label: scopeMeta?.label || humanizeToken(permission.scope),
          description: scopeMeta?.description || permission.description || '',
          moduleGroup: permission.moduleGroup || 'other',
          displayOrder: Number.isFinite(permission.displayOrder) ? permission.displayOrder : 1000,
          permissions: [permission]
        })
      } else {
        existing.permissions.push(permission)
        if (!existing.description && permission.description) {
          existing.description = permission.description
        }
        if (!existing.moduleGroup && permission.moduleGroup) {
          existing.moduleGroup = permission.moduleGroup
        }
        if (
          Number.isFinite(permission.displayOrder) &&
          (existing.displayOrder === undefined || permission.displayOrder < existing.displayOrder)
        ) {
          existing.displayOrder = permission.displayOrder
        }
      }
    }

    return Array.from(modulesByScope.values())
      .map((module) => ({
        ...module,
        permissions: [...module.permissions].sort((left, right) => {
          const leftOrder = Number.isFinite(left.displayOrder) ? left.displayOrder : 1000
          const rightOrder = Number.isFinite(right.displayOrder) ? right.displayOrder : 1000
          if (leftOrder !== rightOrder) return leftOrder - rightOrder
          const leftLabel = left.label || left.action
          const rightLabel = right.label || right.action
          return String(leftLabel).localeCompare(String(rightLabel))
        })
      }))
      .sort((left, right) => {
        if (left.displayOrder !== right.displayOrder) {
          return left.displayOrder - right.displayOrder
        }
        return String(left.label).localeCompare(String(right.label))
      })
  }, [permissions, scopeMetaByCode])

  const moduleGroupOptions = useMemo(() => {
    const values = Array.from(new Set(permissionModules.map((module) => module.moduleGroup).filter(Boolean)))
    return values
      .sort((left, right) => moduleGroupLabel(left).localeCompare(moduleGroupLabel(right)))
      .map((value) => ({ value, label: moduleGroupLabel(value) }))
  }, [permissionModules])

  const filteredRoles = useMemo(() => {
    const q = normalizeText(query)
    return roles.filter((role) => {
      if (activeOnly && !role?.activeInRuntime) {
        return false
      }
      if (!q) {
        return true
      }
      const haystack = [
        role?.role,
        role?.label,
        role?.description,
        ...(Array.isArray(role?.capabilities) ? role.capabilities : [])
      ]
      return haystack.some((value) => normalizeText(value).includes(q))
    })
  }, [activeOnly, query, roles])

  const filteredModules = useMemo(() => {
    const q = normalizeText(query)
    return permissionModules.filter((module) => {
      if (moduleGroupFilter && normalizeText(module.moduleGroup) !== normalizeText(moduleGroupFilter)) {
        return false
      }
      if (!q) {
        return true
      }
      const moduleMatches = [module.scope, module.label, module.description, module.moduleGroup]
        .some((value) => normalizeText(value).includes(q))
      if (moduleMatches) {
        return true
      }
      return module.permissions.some((permission) =>
        [permission.action, permission.label, permission.description].some((value) => normalizeText(value).includes(q))
      )
    })
  }, [moduleGroupFilter, permissionModules, query])

  const groupedModules = useMemo(() => {
    const map = new Map()
    for (const module of filteredModules) {
      const group = normalizeText(module.moduleGroup) || 'other'
      const bucket = map.get(group)
      if (!bucket) {
        map.set(group, [module])
      } else {
        bucket.push(module)
      }
    }
    return Array.from(map.entries()).sort((left, right) => moduleGroupLabel(left[0]).localeCompare(moduleGroupLabel(right[0])))
  }, [filteredModules])

  const assignableRoles = useMemo(
    () => roles.filter((role) => role.activeInRuntime && role.assignable),
    [roles]
  )

  const selectedPreset = useMemo(
    () => permissionPresets.find((preset) => preset.code === selectedPresetCode) || null,
    [permissionPresets, selectedPresetCode]
  )

  const filteredBulkUsers = useMemo(() => {
    const queryText = normalizeText(bulkUserQuery)
    if (!queryText) {
      return users
    }
    return users.filter((user) =>
      [user.fullName, user.email].some((value) => normalizeText(value).includes(queryText))
    )
  }, [users, bulkUserQuery])

  const allVisibleBulkUsersSelected = useMemo(
    () => filteredBulkUsers.length > 0 && filteredBulkUsers.every((user) => bulkSelectedUserIds.includes(user.id)),
    [filteredBulkUsers, bulkSelectedUserIds]
  )

  const auditEvents = useMemo(
    () => (auditLogsPage.content || []).map((event) => ({ ...event, isError: isAuthorizationAuditError(event) })),
    [auditLogsPage.content]
  )

  const auditOutcomeOptions = useMemo(
    () => Array.from(new Set([...auditEvents.map((event) => event.outcome).filter(Boolean), auditOutcomeFilter].filter(Boolean))).sort(),
    [auditEvents, auditOutcomeFilter]
  )

  const filteredAuditEvents = auditEvents

  const selectedAuditEvent = useMemo(
    () => filteredAuditEvents.find((event) => String(event.id) === String(selectedAuditEventId)) || null,
    [filteredAuditEvents, selectedAuditEventId]
  )

  const auditTimelineGroups = useMemo(() => {
    const grouped = new Map()
    filteredAuditEvents.forEach((event) => {
      const key = event.occurredAt ? String(event.occurredAt).slice(0, 10) : 'unknown'
      if (!grouped.has(key)) {
        grouped.set(key, [])
      }
      grouped.get(key).push(event)
    })

    return Array.from(grouped.entries())
      .sort(([a], [b]) => (a < b ? 1 : -1))
      .map(([key, events]) => ({
        key,
        label: key === 'unknown' ? 'Unknown date' : formatTimelineDay(`${key}T00:00:00Z`),
        events: [...events].sort((left, right) => new Date(right.occurredAt || 0) - new Date(left.occurredAt || 0))
      }))
  }, [filteredAuditEvents])

  useEffect(() => {
    if (!selectedRole) {
      setRoleForm(DEFAULT_ROLE_FORM)
      setPermissionDraft({})
      return
    }

    setRoleForm({
      code: selectedRole.role || '',
      label: selectedRole.label || '',
      description: selectedRole.description || '',
      assignable: Boolean(selectedRole.assignable),
      active: Boolean(selectedRole.activeInRuntime)
    })
    setPermissionDraft(buildPermissionDraft(selectedRole, permissions))
  }, [selectedRole, permissions])

  useEffect(() => {
    if (!selectedUserId) {
      setUserRoleDraft([])
      return
    }

    let active = true

    async function loadUserRoles() {
      setLoadingUserRoles(true)
      try {
        const response = await getAdminUserRoleAssignments(selectedUserId)
        if (!active) return
        setUserRoleDraft(Array.isArray(response?.adminRoleCodes) ? response.adminRoleCodes : [])
      } catch (err) {
        if (!active) return
        setError(getErrorMessage(err, 'Failed to load user role assignments.'))
        setUserRoleDraft([])
      } finally {
        if (active) setLoadingUserRoles(false)
      }
    }

    loadUserRoles()

    return () => {
      active = false
    }
  }, [selectedUserId])

  useEffect(() => {
    setSelectedPresetCode('')
  }, [selectedRoleId])

  useEffect(() => {
    if (!hasLoadedAuditRef.current) return undefined
    const timeoutId = window.setTimeout(() => {
      void loadAuditLogs()
    }, 300)
    return () => window.clearTimeout(timeoutId)
  }, [auditQuery, auditOutcomeFilter, auditOnlyErrors])

  useEffect(() => {
    if (!filteredAuditEvents.length) {
      if (selectedAuditEventId) setSelectedAuditEventId('')
      return
    }
    if (!selectedAuditEventId || !filteredAuditEvents.some((event) => String(event.id) === String(selectedAuditEventId))) {
      setSelectedAuditEventId(filteredAuditEvents[0].id)
    }
  }, [filteredAuditEvents, selectedAuditEventId])

  useEffect(() => {
    setBulkPreview(null)
  }, [bulkSelectedUserIds, bulkRoleDraft])

  const selectedPermissionCount = useMemo(
    () => permissions.reduce((count, permission) => count + (permissionDraft[permissionKey(permission.scope, permission.action)] ? 1 : 0), 0),
    [permissionDraft, permissions]
  )

  const totalPermissionCount = permissions.length
  const allPermissionsSelected = totalPermissionCount > 0 && selectedPermissionCount === totalPermissionCount
  const partiallySelected = selectedPermissionCount > 0 && !allPermissionsSelected

  function setAllPermissions(nextChecked) {
    const next = {}
    for (const permission of permissions) {
      next[permissionKey(permission.scope, permission.action)] = nextChecked
    }
    setPermissionDraft(next)
  }

  function setModulePermissions(scope, nextChecked) {
    const targetScope = normalizeText(scope)
    setPermissionDraft((current) => {
      const next = { ...current }
      for (const permission of permissions) {
        if (normalizeText(permission.scope) !== targetScope) continue
        next[permissionKey(permission.scope, permission.action)] = nextChecked
      }
      return next
    })
  }

  function moduleSelectionState(module) {
    const keys = module.permissions.map((permission) => permissionKey(permission.scope, permission.action))
    const selected = keys.filter((key) => permissionDraft[key]).length
    const total = keys.length
    return {
      selected,
      total,
      checked: total > 0 && selected === total,
      indeterminate: selected > 0 && selected < total
    }
  }

  async function saveRole() {
    setSavingRole(true)
    setError('')
    setFlash('')
    try {
      let savedRole

      if (selectedRole) {
        const payload = {
          label: roleForm.label.trim(),
          description: roleForm.description?.trim() || null
        }
        if (!selectedRole.systemRole) {
          payload.assignable = Boolean(roleForm.assignable)
          payload.active = Boolean(roleForm.active)
        }
        savedRole = await updateAdminRole(selectedRole.id, payload)
        setFlash('Role updated.')
      } else {
        savedRole = await createAdminRole({
          code: roleForm.code.trim().toUpperCase(),
          label: roleForm.label.trim(),
          description: roleForm.description?.trim() || null,
          assignable: Boolean(roleForm.assignable),
          active: Boolean(roleForm.active),
          grants: {}
        })
        setFlash('Role created.')
      }

      await refreshRoleData()
      setSelectedRoleId(savedRole?.id || '')
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to save role.'))
    } finally {
      setSavingRole(false)
    }
  }

  async function removeRole() {
    if (!selectedRole) return

    const confirmed = await confirm({
      title: 'Deactivate role?',
      message: `Deactivate "${selectedRole.label || selectedRole.role}" and remove its user assignments?`,
      description: selectedRole.role,
      confirmLabel: 'Deactivate role'
    })

    if (!confirmed) {
      return
    }

    setSavingRole(true)
    setError('')
    setFlash('')
    try {
      await deactivateAdminRole(selectedRole.id)
      setFlash('Role deactivated.')
      await refreshRoleData()
      setSelectedRoleId('')
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to deactivate role.'))
    } finally {
      setSavingRole(false)
    }
  }

  async function savePermissions() {
    if (!selectedRole) return
    setSavingPermissions(true)
    setError('')
    setFlash('')
    try {
      const grants = draftToGrants(permissionDraft, permissions)
      const updated = await replaceAdminRolePermissions(selectedRole.id, { grants })
      setRoles((current) =>
        current.map((role) => (String(role.id) === String(selectedRole.id) ? updated : role))
      )
      await loadAuditLogs()
      setFlash('Role permissions updated.')
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to update role permissions.'))
    } finally {
      setSavingPermissions(false)
    }
  }

  async function applyPermissionPreset() {
    if (!selectedRole || !selectedPresetCode) return

    const confirmed = await confirm({
      title: 'Apply permission preset?',
      message: `Replace all grants on "${selectedRole.label || selectedRole.role}" with preset "${selectedPresetCode}"?`,
      description: 'Existing role-specific grant edits will be overwritten.',
      confirmLabel: 'Apply preset'
    })

    if (!confirmed) {
      return
    }

    setApplyingPreset(true)
    setError('')
    setFlash('')
    try {
      const updated = await applyAdminRolePermissionPreset(selectedRole.id, selectedPresetCode)
      setRoles((current) =>
        current.map((role) => (String(role.id) === String(selectedRole.id) ? updated : role))
      )
      setPermissionDraft(buildPermissionDraft(updated, permissions))
      await loadAuditLogs()
      setFlash(`Applied preset ${selectedPresetCode}.`)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to apply permission preset.'))
    } finally {
      setApplyingPreset(false)
    }
  }

  async function saveUserRoleAssignments() {
    if (!selectedUserId) return
    setAssigningUserRoles(true)
    setError('')
    setFlash('')
    try {
      const updated = await replaceAdminUserRoleAssignments(selectedUserId, {
        roleCodes: userRoleDraft
      })
      setUserRoleDraft(Array.isArray(updated?.adminRoleCodes) ? updated.adminRoleCodes : [])
      await loadAuditLogs()
      setFlash('User role assignments updated.')
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to update user role assignments.'))
    } finally {
      setAssigningUserRoles(false)
    }
  }

  function buildBulkPayload() {
    return {
      userIds: bulkSelectedUserIds,
      roleCodes: bulkRoleDraft
    }
  }

  async function previewBulkUserRoleAssignments() {
    if (!bulkSelectedUserIds.length) {
      setError('Select at least one user for bulk assignment.')
      return null
    }

    setPreviewingBulkAssignments(true)
    setError('')
    try {
      const preview = await previewBulkAdminUserRoleAssignments(buildBulkPayload())
      setBulkPreview(preview || null)
      return preview
    } catch (err) {
      setBulkPreview(null)
      setError(getErrorMessage(err, 'Unable to preview bulk role assignment changes.'))
      return null
    } finally {
      setPreviewingBulkAssignments(false)
    }
  }

  async function saveBulkUserRoleAssignments() {
    if (!bulkSelectedUserIds.length) {
      setError('Select at least one user for bulk assignment.')
      return
    }

    const preview = await previewBulkUserRoleAssignments()
    if (!preview) {
      return
    }

    if (Number(preview.missingUsers || 0) > 0) {
      setError('Bulk preview contains missing users. Fix the selection or CSV import and try again.')
      return
    }

    const confirmed = await confirm({
      title: 'Apply bulk user role assignment?',
      message: `Apply to ${Number(preview.resolvableUsers || 0)} users with ${Number(preview.changedUsers || 0)} changed assignments?`,
      description: 'Current admin role assignments for those users will be replaced.',
      confirmLabel: 'Apply bulk'
    })

    if (!confirmed) {
      return
    }

    setBulkAssigningUserRoles(true)
    setError('')
    setFlash('')
    try {
      const result = await bulkReplaceAdminUserRoleAssignments(buildBulkPayload())
      if (selectedUserId) {
        const currentUserResult = result?.assignments?.find((item) => String(item.userId) === String(selectedUserId))
        if (currentUserResult) {
          setUserRoleDraft(Array.isArray(currentUserResult.adminRoleCodes) ? currentUserResult.adminRoleCodes : [])
        }
      }
      await loadAuditLogs()
      setBulkPreview(preview)
      setFlash(`Bulk role assignments updated for ${Number(result?.updatedUsers || 0)} users.`)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to apply bulk role assignments.'))
    } finally {
      setBulkAssigningUserRoles(false)
    }
  }

  function togglePermission(scope, action) {
    const key = permissionKey(scope, action)
    setPermissionDraft((current) => ({ ...current, [key]: !current[key] }))
  }

  function toggleUserRole(roleCode) {
    setUserRoleDraft((current) => {
      const hasRole = current.includes(roleCode)
      if (hasRole) {
        return current.filter((code) => code !== roleCode)
      }
      return [...current, roleCode]
    })
  }

  function toggleBulkRole(roleCode) {
    setBulkRoleDraft((current) => {
      const hasRole = current.includes(roleCode)
      if (hasRole) {
        return current.filter((code) => code !== roleCode)
      }
      return [...current, roleCode]
    })
  }

  function toggleBulkUser(userId) {
    setBulkSelectedUserIds((current) => {
      const hasUser = current.includes(userId)
      if (hasUser) {
        return current.filter((id) => id !== userId)
      }
      return [...current, userId]
    })
  }

  function toggleSelectAllVisibleBulkUsers(nextChecked) {
    const visibleIds = filteredBulkUsers.map((user) => user.id)
    setBulkSelectedUserIds((current) => {
      if (nextChecked) {
        return Array.from(new Set([...current, ...visibleIds]))
      }
      return current.filter((id) => !visibleIds.includes(id))
    })
  }

  async function saveCurrentBulkView() {
    const name = bulkViewNameInput.trim()
    if (!name) {
      setError('Provide a view name before saving.')
      return
    }

    setSavingBulkView(true)
    setError('')
    setFlash('')
    try {
      const saved = await upsertAdminBulkUserRoleView({
        name,
        query: bulkUserQuery,
        userIds: bulkSelectedUserIds,
        roleCodes: bulkRoleDraft
      })

      if (!saved?.id) {
        throw new Error('Saved view response is missing id.')
      }

      setSavedBulkViews((current) => {
        const withoutSame = current.filter((view) => String(view.id) !== String(saved.id))
        return [...withoutSame, saved].sort((left, right) => String(left.name || '').localeCompare(String(right.name || '')))
      })
      setSelectedBulkViewId(saved.id)
      setBulkViewNameInput(saved.name || name)
      setFlash(`Saved bulk view "${saved.name || name}".`)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to save bulk assignment view.'))
    } finally {
      setSavingBulkView(false)
    }
  }

  function applySelectedBulkView() {
    if (!selectedBulkViewId) return
    const view = savedBulkViews.find((item) => String(item.id) === String(selectedBulkViewId))
    if (!view) return
    setBulkUserQuery(view.query || '')
    setBulkSelectedUserIds(Array.isArray(view.userIds) ? view.userIds : [])
    setBulkRoleDraft(Array.isArray(view.roleCodes) ? view.roleCodes : [])
    setBulkImportReport(null)
    setFlash(`Applied bulk view "${view.name}".`)
  }

  async function deleteSelectedBulkView() {
    if (!selectedBulkViewId) return
    const view = savedBulkViews.find((item) => String(item.id) === String(selectedBulkViewId))
    if (!view) return

    const confirmed = await confirm({
      title: 'Delete saved view?',
      message: `Delete "${view.name}" from saved bulk assignment views?`,
      description: 'This only deletes the saved view metadata.',
      confirmLabel: 'Delete view'
    })

    if (!confirmed) {
      return
    }

    setDeletingBulkView(true)
    setError('')
    setFlash('')
    try {
      await deleteAdminBulkUserRoleView(selectedBulkViewId)
      setSavedBulkViews((current) => current.filter((item) => String(item.id) !== String(selectedBulkViewId)))
      setSelectedBulkViewId('')
      setBulkImportReport(null)
      setFlash(`Deleted bulk view "${view.name}".`)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to delete bulk assignment view.'))
    } finally {
      setDeletingBulkView(false)
    }
  }

  function exportBulkCsv() {
    const selectedUsers = users.filter((user) => bulkSelectedUserIds.includes(user.id))
    if (!selectedUsers.length) {
      setError('Select at least one user before exporting CSV.')
      return
    }

    const csvLines = [
      'user_id,email,full_name,role_codes',
      ...selectedUsers.map((user) => [
        csvCell(user.id),
        csvCell(user.email || ''),
        csvCell(user.fullName || ''),
        csvCell(bulkRoleDraft.join('|'))
      ].join(','))
    ]

    const blob = new Blob([csvLines.join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'rbac-bulk-assignment.csv'
    anchor.click()
    URL.revokeObjectURL(url)
    setFlash(`Exported ${selectedUsers.length} users to CSV.`)
  }

  function downloadBulkCsvTemplate() {
    const csvLines = [
      'user_id,email,full_name,role_codes',
      '00000000-0000-0000-0000-000000000000,user@example.com,Example User,ORDER_MANAGER|SUPPORT_AGENT'
    ]
    const blob = new Blob([csvLines.join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'rbac-bulk-assignment-template.csv'
    anchor.click()
    URL.revokeObjectURL(url)
    setFlash('Downloaded bulk assignment CSV template.')
  }

  function triggerBulkCsvImport() {
    bulkImportInputRef.current?.click()
  }

  async function handleBulkCsvImport(event) {
    const file = event.target?.files?.[0]
    if (!file) return

    setImportingBulkCsv(true)
    setError('')
    setFlash('')
    setBulkImportReport(null)

    try {
      const content = await file.text()
      const lines = content.split(/\r?\n/).filter((line) => line.trim().length > 0)
      if (!lines.length) {
        throw new Error('CSV file is empty.')
      }

      const header = parseCsvLine(lines[0]).map((cell) => normalizeText(cell))
      const userIdIndex = header.indexOf('user_id')
      const roleCodesIndex = header.indexOf('role_codes')

      if (userIdIndex < 0) {
        throw new Error('CSV must include a user_id column.')
      }

      const knownUserIds = new Set(users.map((user) => String(user.id)))
      const validRoleCodes = new Set(assignableRoles.map((role) => String(role.role || '').trim().toUpperCase()))
      const seenUserIds = new Set()
      const importedUserIds = []
      const importedRoleCodes = new Set()
      const reportRows = []

      for (const [lineIndex, line] of lines.slice(1).entries()) {
        const rowNumber = lineIndex + 2
        const values = parseCsvLine(line)
        const userId = String(values[userIdIndex] || '').trim()

        if (!userId) {
          reportRows.push({ row: rowNumber, status: 'error', message: 'Missing user_id.' })
          continue
        }

        if (seenUserIds.has(userId)) {
          reportRows.push({ row: rowNumber, status: 'warning', message: `Duplicate user_id "${userId}" skipped.` })
          continue
        }
        seenUserIds.add(userId)

        if (!knownUserIds.has(userId)) {
          reportRows.push({ row: rowNumber, status: 'error', message: `Unknown user_id "${userId}".` })
          continue
        }

        const rowRoleCodes = []
        if (roleCodesIndex >= 0) {
          const roleCodesText = String(values[roleCodesIndex] || '')
          const parsedRoleCodes = roleCodesText
            .split('|')
            .map((code) => code.trim())
            .map((code) => code.toUpperCase())
            .filter(Boolean)

          if (parsedRoleCodes.length) {
            const invalidRoleCodes = parsedRoleCodes.filter((code) => !validRoleCodes.has(code))
            if (invalidRoleCodes.length) {
              reportRows.push({
                row: rowNumber,
                status: 'error',
                message: `Invalid role_codes: ${invalidRoleCodes.join(', ')}.`
              })
              continue
            }
            rowRoleCodes.push(...parsedRoleCodes)
            parsedRoleCodes.forEach((code) => importedRoleCodes.add(code))
          }
        }

        importedUserIds.push(userId)
        reportRows.push({
          row: rowNumber,
          status: rowRoleCodes.length ? 'ok' : 'warning',
          message: rowRoleCodes.length
            ? `Accepted user_id "${userId}" with ${rowRoleCodes.length} role code(s).`
            : `Accepted user_id "${userId}" with no role_codes value.`
        })
      }

      if (!importedUserIds.length) {
        throw new Error('CSV import did not contain any valid rows.')
      }

      const uniqueImportedUsers = Array.from(new Set(importedUserIds))
      const nextImportedRoles = Array.from(importedRoleCodes).sort()
      const errorsCount = reportRows.filter((row) => row.status === 'error').length
      const warningsCount = reportRows.filter((row) => row.status === 'warning').length

      setBulkSelectedUserIds(uniqueImportedUsers)
      if (nextImportedRoles.length) {
        setBulkRoleDraft(nextImportedRoles)
      }

      setBulkImportReport({
        totalRows: Math.max(lines.length - 1, 0),
        appliedUsers: uniqueImportedUsers.length,
        appliedRoles: nextImportedRoles.length,
        errorsCount,
        warningsCount,
        rows: reportRows
      })

      if (errorsCount > 0) {
        setError(`Imported ${uniqueImportedUsers.length} users with ${errorsCount} row errors and ${warningsCount} warnings.`)
      } else if (warningsCount > 0) {
        setFlash(`Imported ${uniqueImportedUsers.length} users with ${warningsCount} warnings.`)
      } else {
        setFlash(`Imported ${uniqueImportedUsers.length} users from CSV with no validation issues.`)
      }
    } catch (err) {
      setBulkImportReport(null)
      setError(getErrorMessage(err, 'Unable to import bulk assignment CSV.'))
    } finally {
      setImportingBulkCsv(false)
      if (event.target) {
        event.target.value = ''
      }
    }
  }

  function resetRoleEditor() {
    setSelectedRoleId('')
    setRoleForm(DEFAULT_ROLE_FORM)
    setPermissionDraft({})
    setSelectedPresetCode('')
  }

  if (loading) {
    return <Spinner label="Loading roles & permissions..." />
  }

  return (
    <div className="page roles-permissions-page">
      <div className="roles-page-hero">
        <div className="roles-page-hero__content">
          <div className="eyebrow">Admin governance</div>
          <h1>Roles &amp; Permissions</h1>
          <p>
            Centralized RBAC governance for role lifecycle, permission orchestration,
            user assignment workflows, and audit visibility.
          </p>
        </div>

        <div className="roles-page-hero__stats">
          <article className="hero-stat-card">
            <span className="hero-stat-card__label">Policy version</span>
            <strong className="mono">{matrix?.version || 'n/a'}</strong>
          </article>
          <article className="hero-stat-card">
            <span className="hero-stat-card__label">Runtime roles</span>
            <strong>{roles.filter((role) => role.activeInRuntime).length}</strong>
          </article>
          <article className="hero-stat-card">
            <span className="hero-stat-card__label">Permissions</span>
            <strong>{permissions.length}</strong>
          </article>
          <article className="hero-stat-card">
            <span className="hero-stat-card__label">Audit events</span>
            <strong>{auditLogsPage.totalElements || 0}</strong>
          </article>
        </div>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      <section className="panel panel-elevated">
        <div className="section-head section-head-enterprise">
          <div>
            <h3>Role catalog</h3>
            <p>Create custom roles or select an existing role to manage metadata.</p>
          </div>
          <div className="inline-actions">
            <button className="btn btn-ghost btn-sm" type="button" onClick={load} disabled={savingRole || savingPermissions}>
              Refresh
            </button>
            <button className="btn btn-primary btn-sm" type="button" onClick={resetRoleEditor} disabled={savingRole}>
              Create role
            </button>
          </div>
        </div>

        <div className="filters">
          <label>
            Search
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Role, module, permission..." />
          </label>
          <label>
            Module group
            <select value={moduleGroupFilter} onChange={(event) => setModuleGroupFilter(event.target.value)}>
              <option value="">All groups</option>
              {moduleGroupOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="toggle">
            <input type="checkbox" checked={activeOnly} onChange={(event) => setActiveOnly(event.target.checked)} />
            Active roles only
          </label>
        </div>

        <div className="panel-grid">
          <div className="table-wrap table-wrap-elevated">
            <table className="enterprise-table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Flags</th>
                  <th>Users</th>
                </tr>
              </thead>
              <tbody>
                {filteredRoles.length ? (
                  filteredRoles.map((role) => (
                    <tr
                      key={role.id}
                      className={String(role.id) === String(selectedRoleId) ? 'row-selected' : ''}
                      onClick={() => setSelectedRoleId(role.id)}
                      role="button"
                      tabIndex={0}
                    >
                      <td>
                        <strong>{role.label || role.role}</strong>
                        <div className="subtle-meta mono">{role.role}</div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                          <span className={`badge ${role.activeInRuntime ? 'badge-success' : 'badge-muted'}`}>
                            {role.activeInRuntime ? 'Active' : 'Inactive'}
                          </span>
                          <span className={`badge ${role.assignable ? 'badge-primary' : 'badge-outline'}`}>
                            {role.assignable ? 'Assignable' : 'Locked'}
                          </span>
                          {role.systemRole ? <span className="badge badge-warning">System</span> : null}
                        </div>
                      </td>
                      <td>{Number(role.assignedUsers || 0)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="3" className="empty-row">No roles found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="panel panel-nested">
            <h4 style={{ marginTop: 0 }}>{selectedRole ? 'Edit role' : 'Create role'}</h4>
            <div className="form-grid">
              <label>
                Role code
                <input
                  value={roleForm.code}
                  onChange={(event) => setRoleForm((current) => ({ ...current, code: event.target.value.toUpperCase() }))}
                  placeholder="STORE_MANAGER"
                  disabled={Boolean(selectedRole)}
                />
              </label>
              <label>
                Label
                <input
                  value={roleForm.label}
                  onChange={(event) => setRoleForm((current) => ({ ...current, label: event.target.value }))}
                  placeholder="Store Manager"
                />
              </label>
              <label className="span-2">
                Description
                <textarea
                  rows="3"
                  value={roleForm.description}
                  onChange={(event) => setRoleForm((current) => ({ ...current, description: event.target.value }))}
                  placeholder="Role responsibility summary"
                />
              </label>
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={Boolean(roleForm.assignable)}
                  onChange={(event) => setRoleForm((current) => ({ ...current, assignable: event.target.checked }))}
                  disabled={Boolean(selectedRole?.systemRole)}
                />
                Assignable
              </label>
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={Boolean(roleForm.active)}
                  onChange={(event) => setRoleForm((current) => ({ ...current, active: event.target.checked }))}
                  disabled={Boolean(selectedRole?.systemRole)}
                />
                Active
              </label>
            </div>
            <div className="inline-actions wrap">
              <button
                className="btn btn-primary"
                type="button"
                onClick={saveRole}
                disabled={savingRole || !roleForm.label.trim() || (!selectedRole && !roleForm.code.trim())}
              >
                {savingRole ? 'Saving...' : selectedRole ? 'Update role' : 'Create role'}
              </button>
              {selectedRole ? (
                <button
                  className="btn btn-outline btn-danger"
                  type="button"
                  onClick={removeRole}
                  disabled={savingRole || selectedRole.systemRole}
                  title={selectedRole.systemRole ? 'System roles cannot be deactivated.' : 'Deactivate role'}
                >
                  Deactivate role
                </button>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="panel panel-elevated rbac-workspace">
        <div className="section-head section-head-enterprise">
          <div>
            <h3>Permission workspace</h3>
            <p>Manage module-level permissions for the selected role using grouped permission cards.</p>
          </div>
          <div className="rbac-workspace-actions">
            <label>
              Preset
              <select
                value={selectedPresetCode}
                onChange={(event) => setSelectedPresetCode(event.target.value)}
                disabled={!selectedRole || applyingPreset || savingPermissions}
              >
                <option value="">Select preset...</option>
                {permissionPresets.map((preset) => (
                  <option key={preset.code} value={preset.code}>
                    {preset.label || preset.code}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="btn btn-outline"
              type="button"
              onClick={applyPermissionPreset}
              disabled={!selectedRole || !selectedPresetCode || applyingPreset || savingPermissions}
            >
              {applyingPreset ? 'Applying...' : 'Apply preset'}
            </button>
            <label className={`toggle ${partiallySelected ? 'toggle-partial' : ''}`}>
              <input
                type="checkbox"
                checked={allPermissionsSelected}
                onChange={(event) => setAllPermissions(event.target.checked)}
                disabled={!selectedRole || savingPermissions || totalPermissionCount === 0}
              />
              Select all
            </label>
            <button
              className="btn btn-primary"
              type="button"
              onClick={savePermissions}
              disabled={!selectedRole || savingPermissions}
            >
              {savingPermissions ? 'Saving...' : 'Save permissions'}
            </button>
          </div>
        </div>

        {!selectedRole ? (
          <p className="empty-copy">Select or create a role to manage permissions.</p>
        ) : (
          <>
            <div className="rbac-selection-summary">
              <div>
                <strong>{selectedRole.label || selectedRole.role}</strong>
                <span className="badge badge-outline" style={{ marginLeft: 8 }}>{selectedRole.role}</span>
                {selectedRole.systemRole ? <span className="badge badge-warning" style={{ marginLeft: 8 }}>System</span> : null}
              </div>
              <div className="subtle-meta">
                {selectedPermissionCount} / {totalPermissionCount} permissions selected
              </div>
            </div>

            {selectedPreset ? (
              <div className="rbac-preset-summary">
                <strong>{selectedPreset.label || selectedPreset.code}</strong>
                <span className="badge badge-outline" style={{ marginLeft: 8 }}>{selectedPreset.code}</span>
                <p className="subtle-meta" style={{ margin: '6px 0 0' }}>
                  {selectedPreset.description || 'Preset derived from active system policy role.'}
                </p>
                <p className="subtle-meta" style={{ margin: '6px 0 0' }}>
                  {selectedPreset.permissionCount} permissions across {selectedPreset.moduleCount} modules.
                </p>
              </div>
            ) : null}

            <div className="rbac-module-summary-grid">
              {filteredModules.map((module) => {
                const state = moduleSelectionState(module)
                return (
                  <button
                    key={`summary-${module.scope}`}
                    type="button"
                    className={`rbac-module-summary ${state.checked ? 'is-selected' : ''}`}
                    onClick={() => setModulePermissions(module.scope, !state.checked)}
                    disabled={savingPermissions}
                  >
                    <div className="rbac-module-summary-title">{module.label}</div>
                    <div className="rbac-module-summary-count">{state.selected}/{state.total}</div>
                  </button>
                )
              })}
            </div>

            {groupedModules.length === 0 ? (
              <p className="empty-copy">No permission modules match the current filters.</p>
            ) : (
              groupedModules.map(([group, modules]) => (
                <div className="rbac-group" key={`group-${group}`}>
                  <h4>{moduleGroupLabel(group)}</h4>
                  <div className="rbac-module-grid">
                    {modules.map((module) => {
                      const state = moduleSelectionState(module)
                      return (
                        <article className="rbac-module-card" key={module.scope}>
                          <header className="rbac-module-card-header">
                            <div className="rbac-module-card-title">
                              <div className="rbac-module-card-icon">{module.label?.charAt(0) || 'M'}</div>
                              <div>
                                <h5>{module.label}</h5>
                                <p>{module.description || humanizeToken(module.scope)}</p>
                              </div>
                            </div>

                            <label className={`toggle ${state.indeterminate ? 'toggle-partial' : ''}`}>
                              <input
                                type="checkbox"
                                checked={state.checked}
                                onChange={(event) => setModulePermissions(module.scope, event.target.checked)}
                                disabled={savingPermissions}
                              />
                              All
                            </label>
                          </header>

                          <ul className="rbac-permission-list">
                            {module.permissions.map((permission) => {
                              const key = permissionKey(permission.scope, permission.action)
                              const checked = Boolean(permissionDraft[key])
                              return (
                                <li key={key}>
                                  <label className={`rbac-permission-item ${checked ? 'is-selected' : ''}`}>
                                    <input
                                      type="checkbox"
                                      checked={checked}
                                      onChange={() => togglePermission(permission.scope, permission.action)}
                                      disabled={savingPermissions}
                                    />
                                    <span>{permission.label || humanizeToken(permission.action)}</span>
                                    {permission.sensitive ? <em>Sensitive</em> : null}
                                  </label>
                                </li>
                              )
                            })}
                          </ul>
                        </article>
                      )
                    })}
                  </div>
                </div>
              ))
            )}
          </>
        )}
      </section>

      <section className="panel panel-elevated">
        <div className="section-head section-head-enterprise">
          <div>
            <h3>User role assignments</h3>
            <p>Assign active and assignable admin roles to user accounts.</p>
          </div>
          <button
            className="btn btn-primary btn-sm"
            type="button"
            onClick={saveUserRoleAssignments}
            disabled={!selectedUserId || assigningUserRoles || loadingUserRoles}
          >
            {assigningUserRoles ? 'Saving...' : 'Save user assignments'}
          </button>
        </div>

        <div className="filters">
          <label>
            User
            <select value={selectedUserId} onChange={(event) => setSelectedUserId(event.target.value)}>
              <option value="">Select user...</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.fullName || user.email} ({user.email})
                </option>
              ))}
            </select>
          </label>
        </div>

        {!selectedUserId ? (
          <p className="empty-copy">Select a user to manage role assignments.</p>
        ) : loadingUserRoles ? (
          <Spinner label="Loading user roles..." />
        ) : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
            {assignableRoles.length ? (
              assignableRoles.map((role) => (
                <label key={`assign-${role.id}`} className="toggle">
                  <input
                    type="checkbox"
                    checked={userRoleDraft.includes(role.role)}
                    onChange={() => toggleUserRole(role.role)}
                    disabled={assigningUserRoles}
                  />
                  {role.label || role.role}
                </label>
              ))
            ) : (
              <p className="empty-copy">No assignable active roles available.</p>
            )}
          </div>
        )}

        <div className="divider" />

        <div className="section-head section-head-enterprise">
          <div>
            <h3>Bulk user role assignments</h3>
            <p>Apply the same role set to many users in one operation.</p>
          </div>
          <div className="inline-actions">
            <button
              className="btn btn-outline btn-sm"
              type="button"
              onClick={previewBulkUserRoleAssignments}
              disabled={previewingBulkAssignments || bulkSelectedUserIds.length === 0}
            >
              {previewingBulkAssignments ? 'Previewing...' : 'Preview changes'}
            </button>
            <button
              className="btn btn-primary btn-sm"
              type="button"
              onClick={saveBulkUserRoleAssignments}
              disabled={bulkAssigningUserRoles || previewingBulkAssignments || bulkSelectedUserIds.length === 0}
            >
              {bulkAssigningUserRoles ? 'Applying...' : 'Apply to selected users'}
            </button>
          </div>
        </div>

        <div className="filters">
          <label>
            Search users
            <input
              value={bulkUserQuery}
              onChange={(event) => setBulkUserQuery(event.target.value)}
              placeholder="Name or email..."
            />
          </label>
          <label className="toggle">
            <input
              type="checkbox"
              checked={allVisibleBulkUsersSelected}
              onChange={(event) => toggleSelectAllVisibleBulkUsers(event.target.checked)}
            />
            Select all visible
          </label>
        </div>

        <div className="bulk-assignment-toolbar">
          <label>
            Save current view
            <input
              value={bulkViewNameInput}
              onChange={(event) => setBulkViewNameInput(event.target.value)}
              placeholder="e.g. Ops managers - APAC"
            />
          </label>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={saveCurrentBulkView}
            disabled={savingBulkView || deletingBulkView}
          >
            {savingBulkView ? 'Saving...' : 'Save view'}
          </button>
          <label>
            Saved views
            <select
              value={selectedBulkViewId}
              onChange={(event) => setSelectedBulkViewId(event.target.value)}
              disabled={savingBulkView || deletingBulkView}
            >
              <option value="">Select view...</option>
              {savedBulkViews.map((view) => (
                <option key={view.id} value={view.id}>
                  {view.name}
                </option>
              ))}
            </select>
          </label>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={applySelectedBulkView}
            disabled={!selectedBulkViewId || savingBulkView || deletingBulkView}
          >
            Apply view
          </button>
          <button
            className="btn btn-outline btn-sm btn-danger"
            type="button"
            onClick={deleteSelectedBulkView}
            disabled={!selectedBulkViewId || savingBulkView || deletingBulkView}
          >
            {deletingBulkView ? 'Deleting...' : 'Delete view'}
          </button>
        </div>

        <div className="inline-actions wrap" style={{ marginBottom: 12 }}>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={downloadBulkCsvTemplate}
          >
            Download template
          </button>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={exportBulkCsv}
            disabled={bulkSelectedUserIds.length === 0}
          >
            Export CSV
          </button>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={triggerBulkCsvImport}
            disabled={importingBulkCsv}
          >
            {importingBulkCsv ? 'Importing...' : 'Import CSV'}
          </button>
          <input
            ref={bulkImportInputRef}
            type="file"
            accept=".csv,text/csv"
            onChange={handleBulkCsvImport}
            style={{ display: 'none' }}
          />
        </div>

        {bulkImportReport ? (
          <div className="bulk-import-report">
            <div className="bulk-import-report-summary">
              <span className="badge badge-outline">Rows: {bulkImportReport.totalRows}</span>
              <span className="badge badge-success">Applied users: {bulkImportReport.appliedUsers}</span>
              <span className="badge badge-primary">Applied roles: {bulkImportReport.appliedRoles}</span>
              <span className={`badge ${bulkImportReport.warningsCount ? 'badge-warning' : 'badge-muted'}`}>
                Warnings: {bulkImportReport.warningsCount}
              </span>
              <span className={`badge ${bulkImportReport.errorsCount ? 'badge-danger' : 'badge-muted'}`}>
                Errors: {bulkImportReport.errorsCount}
              </span>
            </div>
            <div className="table-wrap">
              <table className="bulk-import-report-table">
                <thead>
                  <tr>
                    <th>Row</th>
                    <th>Status</th>
                    <th>Message</th>
                  </tr>
                </thead>
                <tbody>
                  {bulkImportReport.rows?.length ? (
                    bulkImportReport.rows.map((row) => (
                      <tr key={`bulk-import-row-${row.row}-${row.message}`}>
                        <td>{row.row}</td>
                        <td>
                          <span
                            className={`badge ${
                              row.status === 'error'
                                ? 'badge-danger'
                                : row.status === 'warning'
                                  ? 'badge-warning'
                                  : 'badge-success'
                            }`}
                          >
                            {row.status}
                          </span>
                        </td>
                        <td>{row.message}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="3" className="empty-row">No CSV import validation rows.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th style={{ width: 44 }} />
                <th>User</th>
                <th>Email</th>
              </tr>
            </thead>
            <tbody>
              {filteredBulkUsers.length ? (
                filteredBulkUsers.map((user) => (
                  <tr key={`bulk-user-${user.id}`}>
                    <td>
                      <input
                        type="checkbox"
                        checked={bulkSelectedUserIds.includes(user.id)}
                        onChange={() => toggleBulkUser(user.id)}
                      />
                    </td>
                    <td>{user.fullName || '—'}</td>
                    <td>{user.email}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="3" className="empty-row">No users match your filter.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="subtle-meta" style={{ marginTop: 10, marginBottom: 8 }}>
          Selected users: {bulkSelectedUserIds.length}
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
          {assignableRoles.length ? (
            assignableRoles.map((role) => (
              <label key={`bulk-role-${role.id}`} className="toggle">
                <input
                  type="checkbox"
                  checked={bulkRoleDraft.includes(role.role)}
                  onChange={() => toggleBulkRole(role.role)}
                  disabled={bulkAssigningUserRoles}
                />
                {role.label || role.role}
              </label>
            ))
          ) : (
            <p className="empty-copy">No assignable active roles available.</p>
          )}
        </div>

        {bulkPreview ? (
          <div className="bulk-assignment-preview">
            <div className="bulk-assignment-preview-summary">
              <span className="badge badge-outline">Requested: {Number(bulkPreview.requestedUsers || 0)}</span>
              <span className="badge badge-success">Resolvable: {Number(bulkPreview.resolvableUsers || 0)}</span>
              <span className={`badge ${Number(bulkPreview.changedUsers || 0) > 0 ? 'badge-warning' : 'badge-muted'}`}>
                Changed: {Number(bulkPreview.changedUsers || 0)}
              </span>
              <span className={`badge ${Number(bulkPreview.missingUsers || 0) > 0 ? 'badge-danger' : 'badge-muted'}`}>
                Missing: {Number(bulkPreview.missingUsers || 0)}
              </span>
            </div>
            {bulkPreview.missingUsers ? (
              <p className="subtle-meta">
                Missing user IDs: {(bulkPreview.missingUserIds || []).join(', ') || 'None'}
              </p>
            ) : null}

            <div className="table-wrap">
              <table className="bulk-preview-table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Current roles</th>
                    <th>Proposed roles</th>
                    <th>Add</th>
                    <th>Remove</th>
                    <th>Changed</th>
                  </tr>
                </thead>
                <tbody>
                  {bulkPreview.items?.length ? (
                    bulkPreview.items.map((item) => (
                      <tr key={item.userId}>
                        <td>
                          <strong>{item.fullName || item.email || item.userId}</strong>
                          <div className="subtle-meta mono">{item.userId}</div>
                        </td>
                        <td>{(item.currentRoleCodes || []).join(', ') || '—'}</td>
                        <td>{(item.proposedRoleCodes || []).join(', ') || '—'}</td>
                        <td>{(item.rolesToAdd || []).join(', ') || '—'}</td>
                        <td>{(item.rolesToRemove || []).join(', ') || '—'}</td>
                        <td>
                          <span className={`badge ${item.changed ? 'badge-warning' : 'badge-success'}`}>
                            {item.changed ? 'Yes' : 'No'}
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="6" className="empty-row">No resolvable users in preview.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </section>

      <section className="panel panel-elevated">
        <div className="section-head section-head-enterprise">
          <div>
            <h3>Authorization audit logs</h3>
            <p>Recent RBAC role and assignment mutation events.</p>
          </div>
          <button
            className="btn btn-outline btn-sm"
            type="button"
            onClick={loadAuditLogs}
            disabled={loadingAuditLogs}
          >
            {loadingAuditLogs ? 'Refreshing...' : 'Refresh audit logs'}
          </button>
        </div>

        {loadingAuditLogs ? (
          <Spinner label="Loading authorization audit logs..." />
        ) : (
          <div className="authorization-audit-workspace">
            <div className="authorization-audit-toolbar">
              <label className="authorization-audit-search">
                Search
                <input
                  type="search"
                  value={auditQuery}
                  onChange={(event) => setAuditQuery(event.target.value)}
                  placeholder="Search action, actor, entity, correlation, hash"
                />
              </label>
              <label>
                Outcome
                <select value={auditOutcomeFilter} onChange={(event) => setAuditOutcomeFilter(event.target.value)}>
                  <option value="">All outcomes</option>
                  {auditOutcomeOptions.map((outcome) => (
                    <option key={outcome} value={outcome}>{outcome}</option>
                  ))}
                </select>
              </label>
              <label className="checkbox-row authorization-audit-errors">
                <input
                  type="checkbox"
                  checked={auditOnlyErrors}
                  onChange={(event) => setAuditOnlyErrors(event.target.checked)}
                />
                Errors only
              </label>
              <p className="authorization-audit-meta">
                Showing {auditLogsPage.content?.length || 0} of {auditLogsPage.totalElements || 0} events
              </p>
            </div>

            <div className="authorization-audit-content">
              <aside className="authorization-audit-timeline">
                {auditTimelineGroups.length ? (
                  auditTimelineGroups.map((group) => (
                    <div key={group.key} className="authorization-audit-group">
                      <p className="authorization-audit-date">{group.label}</p>
                      <ul className="authorization-audit-list">
                        {group.events.map((event) => (
                          <li key={event.id}>
                            <button
                              type="button"
                              className={`authorization-audit-item ${String(selectedAuditEventId) === String(event.id) ? 'active' : ''} ${event.isError ? 'is-error' : ''}`}
                              onClick={() => setSelectedAuditEventId(event.id)}
                            >
                              <span className="authorization-audit-time">{formatTimelineTime(event.occurredAt)}</span>
                              <span className="authorization-audit-action">{event.actionType}</span>
                              <span className="authorization-audit-entity">{event.entityType} | {event.entityId || '—'}</span>
                            </button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))
                ) : (
                  <div className="empty-copy">No authorization audit logs match the current filters.</div>
                )}
              </aside>

              <div className="authorization-audit-main">
                <div className="table-wrap">
                  <table className="authorization-audit-table">
                    <thead>
                      <tr>
                        <th>Time</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Actor</th>
                        <th>Outcome</th>
                        <th>Correlation</th>
                        <th>Details</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAuditEvents.length ? (
                        filteredAuditEvents.map((event) => (
                          <tr
                            key={event.id}
                            className={[
                              String(selectedAuditEventId) === String(event.id) ? 'row-active' : '',
                              event.isError ? 'row-error' : ''
                            ].filter(Boolean).join(' ')}
                            onClick={() => setSelectedAuditEventId(event.id)}
                          >
                            <td>{formatOccurredAt(event.occurredAt)}</td>
                            <td><span className={`badge ${event.isError ? 'badge-danger' : 'badge-outline'}`}>{event.actionType}</span></td>
                            <td>
                              <div className="subtle-meta">{event.entityType}</div>
                              <div className="mono">{event.entityId || '—'}</div>
                            </td>
                            <td>{event.actorEmail || <span className="subtle-meta">System</span>}</td>
                            <td>
                              <span className={`badge ${event.isError ? 'badge-danger' : 'badge-success'}`}>
                                {event.outcome || 'UNKNOWN'}
                              </span>
                            </td>
                            <td className="mono">{event.correlationId || '—'}</td>
                            <td className="mono" title={event.detailsJson || ''}>{truncate(event.detailsJson || '', 160) || '—'}</td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="7" className="empty-row">No authorization audit logs recorded yet.</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {selectedAuditEvent ? (
                  <div className="authorization-audit-detail">
                    <div className="authorization-audit-detail-head">
                      <strong>{selectedAuditEvent.actionType}</strong>
                      <span className={`badge ${selectedAuditEvent.isError ? 'badge-danger' : 'badge-success'}`}>
                        {selectedAuditEvent.outcome || 'UNKNOWN'}
                      </span>
                    </div>
                    <div className="subtle-meta">
                      {selectedAuditEvent.entityType} | {selectedAuditEvent.entityId || '—'} | {selectedAuditEvent.actorEmail || 'System'}
                    </div>
                    <div className="subtle-meta mono">Correlation: {selectedAuditEvent.correlationId || '—'}</div>
                    <pre className="authorization-audit-json">{selectedAuditEvent.detailsJson || 'No details payload.'}</pre>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default RolesPermissionsPage