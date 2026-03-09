import { createContext, useContext, useEffect, useState } from 'react'
import {
  loginPassword as loginPasswordRequest,
  me,
  registerUser
} from '../../shared/api/endpoints/authApi'
import {
  clearAuthSnapshot,
  loadAuthSnapshot,
  saveAuthSnapshot
} from '../../shared/auth/tokenStorage'
import { normalizeRoles } from '../../shared/auth/roles'

const AuthContext = createContext(null)

function toSnapshot(payload) {
  const user = payload || {}
  return {
    accessToken: user.accessToken,
    refreshToken: user.refreshToken,
    userId: user.userId,
    email: user.email,
    fullName: user.fullName,
    roles: normalizeRoles(user.roles),
    enabled: user.enabled,
    permissions: []
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
        const profile = await me()
        if (!active) return
        const merged = {
          ...snapshot,
          userId: profile.id || snapshot.userId,
          email: profile.email || snapshot.email,
          fullName: profile.fullName || snapshot.fullName,
          roles: normalizeRoles(profile.roles || snapshot.roles),
          enabled: profile.enabled ?? snapshot.enabled
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
        const profile = await me()
        const merged = {
          ...snapshot,
          userId: profile.id || snapshot.userId,
          email: profile.email || snapshot.email,
          fullName: profile.fullName || snapshot.fullName,
          roles: normalizeRoles(profile.roles || snapshot.roles),
          enabled: profile.enabled ?? snapshot.enabled
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
