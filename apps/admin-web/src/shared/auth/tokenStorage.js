const AUTH_KEY = 'noura_admin_auth_v2'
const LEGACY_KEYS = ['noura_admin_auth_v1']
const TOKEN_STORAGE_CHANNEL = 'noura_admin_auth_channel'
const memorySnapshot = {
  value: null
}

function safeGet(storage, key) {
  try {
    return storage.getItem(key)
  } catch (_) {
    return null
  }
}

function safeSet(storage, key, value) {
  try {
    storage.setItem(key, value)
    return true
  } catch (_) {
    return false
  }
}

function safeRemove(storage, key) {
  try {
    storage.removeItem(key)
  } catch (_) {
    // ignore storage access failures
  }
}

function safeParse(raw) {
  try {
    return JSON.parse(raw)
  } catch (_) {
    return null
  }
}

function normalizeSnapshot(snapshot) {
  if (!snapshot || typeof snapshot !== 'object') {
    return null
  }
  if (!snapshot.accessToken) {
    return null
  }
  return {
    accessToken: snapshot.accessToken,
    refreshToken: snapshot.refreshToken || null,
    userId: snapshot.userId || null,
    email: snapshot.email || null,
    fullName: snapshot.fullName || null,
    roles: Array.isArray(snapshot.roles) ? snapshot.roles : [],
    enabled: snapshot.enabled !== false,
    permissions: Array.isArray(snapshot.permissions) ? snapshot.permissions : [],
    capabilities: snapshot.capabilities && typeof snapshot.capabilities === 'object'
      ? snapshot.capabilities
      : {},
    issuedAt: snapshot.issuedAt || new Date().toISOString()
  }
}

function removeLegacyKeys() {
  for (const key of LEGACY_KEYS) {
    safeRemove(localStorage, key)
    safeRemove(sessionStorage, key)
  }
}

function persistSnapshot(snapshot, storage = localStorage) {
  safeSet(storage, AUTH_KEY, JSON.stringify(snapshot))
  safeSet(localStorage, TOKEN_STORAGE_CHANNEL, String(Date.now()))
  removeLegacyKeys()
}

function loadFromStorage() {
  const currentLocal = normalizeSnapshot(safeParse(safeGet(localStorage, AUTH_KEY)))
  if (currentLocal) {
    return currentLocal
  }
  const currentSession = normalizeSnapshot(safeParse(safeGet(sessionStorage, AUTH_KEY)))
  if (currentSession) {
    return currentSession
  }

  for (const legacyKey of LEGACY_KEYS) {
    const legacy = normalizeSnapshot(
      safeParse(safeGet(localStorage, legacyKey)) || safeParse(safeGet(sessionStorage, legacyKey))
    )
    if (legacy) {
      persistSnapshot(legacy, localStorage)
      return legacy
    }
  }

  return null
}

export function loadAuthSnapshot() {
  if (memorySnapshot.value) {
    return memorySnapshot.value
  }
  const snapshot = loadFromStorage()
  memorySnapshot.value = snapshot
  return snapshot
}

export function saveAuthSnapshot(snapshot, options = {}) {
  const normalized = normalizeSnapshot(snapshot)
  if (!normalized) {
    clearAuthSnapshot()
    return
  }

  const useSession = Boolean(options?.sessionOnly)
  memorySnapshot.value = normalized
  persistSnapshot(normalized, useSession ? sessionStorage : localStorage)
  if (!useSession) {
    safeRemove(sessionStorage, AUTH_KEY)
  } else {
    safeRemove(localStorage, AUTH_KEY)
  }
}

export function clearAuthSnapshot() {
  memorySnapshot.value = null
  safeRemove(localStorage, AUTH_KEY)
  safeRemove(sessionStorage, AUTH_KEY)
  removeLegacyKeys()
  safeSet(localStorage, TOKEN_STORAGE_CHANNEL, String(Date.now()))
}

export function getAccessToken() {
  return loadAuthSnapshot()?.accessToken || null
}

export function getRefreshToken() {
  return loadAuthSnapshot()?.refreshToken || null
}

export function updateTokens(accessToken, refreshToken) {
  const current = loadAuthSnapshot()
  if (!current) return
  saveAuthSnapshot({
    ...current,
    accessToken,
    refreshToken: refreshToken || current.refreshToken
  })
}
