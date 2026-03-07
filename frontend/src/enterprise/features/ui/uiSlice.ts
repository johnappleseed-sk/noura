import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/app/store'

type ThemeMode = 'light' | 'dark'

interface UiState {
  theme: ThemeMode
  chatbotOpen: boolean
}

/**
 * Executes detect theme.
 *
 * @returns The result of detect theme.
 */
const detectTheme = (): ThemeMode => {
  const storedTheme = localStorage.getItem('enterprise_theme') as ThemeMode | null
  if (storedTheme) {
    return storedTheme
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const initialState: UiState = {
  theme: detectTheme(),
  chatbotOpen: false,
}

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setTheme: (state, { payload }: PayloadAction<ThemeMode>) => {
      state.theme = payload
      localStorage.setItem('enterprise_theme', payload)
    },
    toggleTheme: (state) => {
      const nextTheme = state.theme === 'dark' ? 'light' : 'dark'
      state.theme = nextTheme
      localStorage.setItem('enterprise_theme', nextTheme)
    },
    toggleChatbot: (state) => {
      state.chatbotOpen = !state.chatbotOpen
    },
    setChatbotOpen: (state, { payload }: PayloadAction<boolean>) => {
      state.chatbotOpen = payload
    },
  },
})

export const { setTheme, toggleTheme, toggleChatbot, setChatbotOpen } = uiSlice.actions
export const selectTheme = (state: RootState): ThemeMode => state.ui.theme
export const selectChatbotOpen = (state: RootState): boolean => state.ui.chatbotOpen
export const uiReducer = uiSlice.reducer
