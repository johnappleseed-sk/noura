import { UserProfile } from '@/types'

const TOKEN_KEY = 'enterprise_commerce_token'
const USER_KEY = 'enterprise_commerce_user'
const HISTORY_KEY = 'enterprise_commerce_browsing_history'
const STORE_KEY = 'enterprise_selected_store_id'
const STORE_SUGGESTION_DISMISSED_KEY = 'enterprise_store_suggestion_dismissed'

export const storage = {
  getToken: (): string | null => localStorage.getItem(TOKEN_KEY),
  setToken: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  clearToken: (): void => localStorage.removeItem(TOKEN_KEY),

  getUser: (): UserProfile | null => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserProfile) : null
  },
  setUser: (user: UserProfile): void => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clearUser: (): void => localStorage.removeItem(USER_KEY),

  getHistory: (): string[] => {
    const raw = localStorage.getItem(HISTORY_KEY)
    return raw ? (JSON.parse(raw) as string[]) : []
  },
  pushHistory: (productId: string): string[] => {
    const current = storage.getHistory().filter((id) => id !== productId)
    const next = [productId, ...current].slice(0, 20)
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
    return next
  },
  clearHistory: (): void => localStorage.removeItem(HISTORY_KEY),

  getSelectedStoreId: (): string | null => localStorage.getItem(STORE_KEY),
  setSelectedStoreId: (storeId: string): void => localStorage.setItem(STORE_KEY, storeId),
  clearSelectedStoreId: (): void => localStorage.removeItem(STORE_KEY),

  getStoreSuggestionDismissed: (): boolean => localStorage.getItem(STORE_SUGGESTION_DISMISSED_KEY) === '1',
  setStoreSuggestionDismissed: (dismissed: boolean): void => {
    if (dismissed) {
      localStorage.setItem(STORE_SUGGESTION_DISMISSED_KEY, '1')
      return
    }
    localStorage.removeItem(STORE_SUGGESTION_DISMISSED_KEY)
  },
}
