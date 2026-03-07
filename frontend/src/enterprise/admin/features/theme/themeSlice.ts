import { PayloadAction, createSlice } from '@reduxjs/toolkit'

export type ThemeMode = 'light' | 'dark'

interface ThemeState {
  mode: ThemeMode
}

const initialState: ThemeState = {
  mode: (localStorage.getItem('admin_theme_mode') as ThemeMode | null) ?? 'light',
}

const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    setThemeMode: (state, action: PayloadAction<ThemeMode>) => {
      state.mode = action.payload
      localStorage.setItem('admin_theme_mode', action.payload)
    },
    toggleThemeMode: (state) => {
      state.mode = state.mode === 'light' ? 'dark' : 'light'
      localStorage.setItem('admin_theme_mode', state.mode)
    },
  },
})

export const { setThemeMode, toggleThemeMode } = themeSlice.actions
export const themeReducer = themeSlice.reducer
