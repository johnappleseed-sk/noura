import { createSlice } from '@reduxjs/toolkit';
import type { RootState } from '@/app/store';

export type ThemeMode = 'light' | 'dark';

const THEME_STORAGE_KEY = 'noura_admin_theme';

/**
 * Executes read theme.
 *
 * @returns The result of read theme.
 */
const readTheme = (): ThemeMode => {
  const persisted = localStorage.getItem(THEME_STORAGE_KEY);
  return persisted === 'dark' ? 'dark' : 'light';
};

interface ThemeState {
  mode: ThemeMode;
}

const themeSlice = createSlice({
  name: 'theme',
  initialState: { mode: readTheme() } as ThemeState,
  reducers: {
    toggleTheme: (state) => {
      state.mode = state.mode === 'light' ? 'dark' : 'light';
      localStorage.setItem(THEME_STORAGE_KEY, state.mode);
    },
    setTheme: (state, action: { payload: ThemeMode }) => {
      state.mode = action.payload;
      localStorage.setItem(THEME_STORAGE_KEY, state.mode);
    },
  },
});

export const { toggleTheme, setTheme } = themeSlice.actions;
export const themeReducer = themeSlice.reducer;
export const selectThemeMode = (state: RootState): ThemeMode => state.theme.mode;
