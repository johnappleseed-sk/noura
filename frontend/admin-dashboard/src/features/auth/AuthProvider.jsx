import { createContext, useContext, useEffect, useState } from 'react'
import {
  loginPassword as loginPasswordRequest,
  me,
  registerUser
} from '../../shared/api/endpoints/authApi'
import { getAdminCapabilities } from '../../shared/api/endpoints/adminCapabilitiesApi'
import {
  clearAuthSnapshot,
  loadAuthSnapshot,
  saveAuthSnapshot
} from '../../shared/auth/tokenStorage'
import { deriveCapabilities, normalizeRoles } from '../../shared/auth/roles'

const AuthContext = createContext(null)

function toSnapshot(payload) {
  const user = payload || {}
  const roles = normalizeRoles(user.roles)
  return {
    accessToken: user.accessToken,
    refreshToken: user.refreshToken,
    userId: user.userId,
    email: user.email,
    fullName: user.fullName,
    roles,
    enabled: user.enabled,
    permissions: [],
    capabilities: deriveCapabilities(roles)
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => loadAuthSnapshot())
  const [initializing, setInitializing] = useState(true)

  useEffect(() => {
    let active = true

    async function bootstrap() {
      const snapshot = loadAuthSnapshot()
      if (!snapshot?.accessToken) {
        if (active) setInitializing(false)
        return
      }

      try {
        const [profile, capabilitiesResponse] = await Promise.all([
          me(),
          getAdminCapabilities().catch(() => null)
        ])
        if (!active) return
        const roles = normalizeRoles(profile.roles || snapshot.roles)
        const merged = {
          ...snapshot,
          userId: profile.id || snapshot.userId,
          email: profile.email || snapshot.email,
          fullName: profile.fullName || snapshot.fullName,
          roles,
          enabled: profile.enabled ?? snapshot.enabled,
          capabilities: capabilitiesResponse?.capabilities || deriveCapabilities(roles)
        }
        saveAuthSnapshot(merged)
        setAuth(merged)
      } catch (_) {
        clearAuthSnapshot()
        if (active) setAuth(null)
      } finally {
        if (active) setInitializing(false)
      }
    }

    bootstrap()

    return () => {
      active = false
    }
  }, [])

  const value = {
    auth,
    initializing,
    isAuthenticated: Boolean(auth?.accessToken),
    async loginPassword(credentials) {
      const tokens = await loginPasswordRequest(credentials)
      const snapshot = toSnapshot(tokens)
      saveAuthSnapshot(snapshot)
      setAuth(snapshot)
      try {
        const [profile, capabilitiesResponse] = await Promise.all([
          me(),
          getAdminCapabilities().catch(() => null)
        ])
        const roles = normalizeRoles(profile.roles || snapshot.roles)
        const merged = {
          ...snapshot,
          userId: profile.id || snapshot.userId,
          email: profile.email || snapshot.email,
          fullName: profile.fullName || snapshot.fullName,
          roles,
          enabled: profile.enabled ?? snapshot.enabled,
          capabilities: capabilitiesResponse?.capabilities || deriveCapabilities(roles)
        }
        saveAuthSnapshot(merged)
        setAuth(merged)
        return merged
      } catch (_) {
        return snapshot
      }
    },
    async register(credentials) {
      const tokens = await registerUser(credentials)
      const snapshot = toSnapshot(tokens)
      saveAuthSnapshot(snapshot)
      setAuth(snapshot)
      return snapshot
    },
    logout() {
      clearAuthSnapshot()
      setAuth(null)
    }
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
