import { createContext, useContext, useEffect, useMemo, useState } from 'react'

const ThemeContext = createContext(null)
const THEME_STORAGE_KEY = 'noura.admin.theme'

function safeReadTheme() {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch (_) {
    // ignore
  }
  return 'system'
}

function safeWriteTheme(value) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, value)
  } catch (_) {
    // ignore
  }
}

function systemTheme() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme
}

export function ThemeProvider({ children }) {
  const [preference, setPreference] = useState(() => safeReadTheme())
  const resolved = preference === 'system' ? systemTheme() : preference

  useEffect(() => {
    applyTheme(resolved)
  }, [resolved])

  useEffect(() => {
    if (preference !== 'system') return undefined
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = () => applyTheme(systemTheme())
    media.addEventListener?.('change', handler)
    return () => media.removeEventListener?.('change', handler)
  }, [preference])

  const value = useMemo(() => {
    function update(next) {
      const normalized = next === 'light' || next === 'dark' ? next : 'system'
      safeWriteTheme(normalized)
      setPreference(normalized)
    }

    function toggle() {
      update(resolved === 'dark' ? 'light' : 'dark')
    }

    return {
      preference,
      theme: resolved,
      setThemePreference: update,
      toggleTheme: toggle
    }
  }, [preference, resolved])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used inside ThemeProvider')
  }
  return context
}

