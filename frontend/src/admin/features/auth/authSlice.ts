import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import { authApi } from '@/api/authApi';
import { AUTH_STORAGE_KEY } from '@/api/client';
import type { RootState } from '@/app/store';
import type { AuthTokensResponse, RoleType } from '@/types/models';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  roles: RoleType[];
  status: 'idle' | 'loading' | 'failed';
  error: string | null;
}

interface LoginPayload {
  email: string;
  password: string;
}

/**
 * Executes read persisted auth.
 *
 * @returns The result of read persisted auth.
 */
const readPersistedAuth = (): AuthState => {
  const defaultState: AuthState = {
    accessToken: null,
    refreshToken: null,
    userId: null,
    email: null,
    fullName: null,
    roles: [],
    status: 'idle',
    error: null,
  };

  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return defaultState;
  }

  try {
    const parsed = JSON.parse(raw) as Omit<AuthState, 'status' | 'error'>;
    return { ...defaultState, ...parsed };
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return defaultState;
  }
};

/**
 * Executes persist auth.
 *
 * @param payload The payload value.
 * @returns No value.
 */
const persistAuth = (payload: AuthState): void => {
  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      userId: payload.userId,
      email: payload.email,
      fullName: payload.fullName,
      roles: payload.roles,
    }),
  );
};

/**
 * Removes clear auth.
 *
 * @returns No value.
 */
const clearAuth = (): void => {
  localStorage.removeItem(AUTH_STORAGE_KEY);
};

export const login = createAsyncThunk<AuthTokensResponse, LoginPayload, { rejectValue: string }>(
  'auth/login',
  async (payload, thunkApi) => {
    try {
      return await authApi.login(payload);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Login failed';
      return thunkApi.rejectWithValue(message);
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState: readPersistedAuth(),
  reducers: {
    logout: (state) => {
      state.accessToken = null;
      state.refreshToken = null;
      state.userId = null;
      state.email = null;
      state.fullName = null;
      state.roles = [];
      state.status = 'idle';
      state.error = null;
      clearAuth();
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action: PayloadAction<AuthTokensResponse>) => {
        state.status = 'idle';
        state.accessToken = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.userId = action.payload.userId;
        state.email = action.payload.email;
        state.fullName = action.payload.fullName;
        state.roles = action.payload.roles;
        persistAuth(state);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Login failed';
      });
  },
});

export const { logout } = authSlice.actions;
export const authReducer = authSlice.reducer;

export const selectIsAuthenticated = (state: RootState): boolean => Boolean(state.auth.accessToken);
export const selectRoles = (state: RootState): RoleType[] => state.auth.roles;
export const selectHasRole = (state: RootState, role: RoleType): boolean => state.auth.roles.includes(role);
