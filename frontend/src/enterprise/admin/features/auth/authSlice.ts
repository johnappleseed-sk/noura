import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { authApi } from '@/admin/api/authApi'
import { ADMIN_REFRESH_KEY, ADMIN_TOKEN_KEY, ADMIN_USER_KEY } from '@/admin/api/client'
import { AuthTokensResponse, AuthUser } from '@/admin/types'

interface LoginRequest {
  email: string
  password: string
}

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: AuthUser | null
  loading: boolean
  error: string | null
}

/**
 * Executes read stored user.
 *
 * @returns The result of read stored user.
 */
const readStoredUser = (): AuthUser | null => {
  const raw = localStorage.getItem(ADMIN_USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

/**
 * Executes to auth user.
 *
 * @param tokens The tokens value.
 * @returns The result of to auth user.
 */
const toAuthUser = (tokens: AuthTokensResponse): AuthUser => ({
  userId: tokens.userId,
  email: tokens.email,
  fullName: tokens.fullName,
  roles: tokens.roles,
})

export const login = createAsyncThunk<AuthTokensResponse, LoginRequest>(
  'adminAuth/login',
  async (payload, thunkApi) => {
    try {
      return await authApi.login(payload)
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to login')
    }
  },
)

const initialState: AuthState = {
  token: localStorage.getItem(ADMIN_TOKEN_KEY),
  refreshToken: localStorage.getItem(ADMIN_REFRESH_KEY),
  user: readStoredUser(),
  loading: false,
  error: null,
}

const authSlice = createSlice({
  name: 'adminAuth',
  initialState,
  reducers: {
    logout: (state) => {
      state.token = null
      state.refreshToken = null
      state.user = null
      state.loading = false
      state.error = null
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_REFRESH_KEY)
      localStorage.removeItem(ADMIN_USER_KEY)
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false
        state.token = action.payload.accessToken
        state.refreshToken = action.payload.refreshToken
        state.user = toAuthUser(action.payload)
        localStorage.setItem(ADMIN_TOKEN_KEY, action.payload.accessToken)
        localStorage.setItem(ADMIN_REFRESH_KEY, action.payload.refreshToken)
        localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(state.user))
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to login'
      })
  },
})

export const { logout } = authSlice.actions
export const authReducer = authSlice.reducer
