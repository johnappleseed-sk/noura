import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { authApi } from '@/api/authApi'
import { storage } from '@/lib/storage'
import { AuthState, UserProfile } from '@/types'

interface Credentials {
  email: string
  password: string
}

interface RegisterPayload extends Credentials {
  fullName: string
}

const initialState: AuthState = {
  user: storage.getUser(),
  token: storage.getToken(),
  status: storage.getToken() ? 'authenticated' : 'idle',
  error: null,
}

export const login = createAsyncThunk('auth/login', async (payload: Credentials) => authApi.login(payload))

export const register = createAsyncThunk(
  'auth/register',
  async (payload: RegisterPayload) => authApi.register(payload),
)

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null
      state.token = null
      state.status = 'idle'
      state.error = null
      storage.clearToken()
      storage.clearUser()
    },
    updateProfile: (state, { payload }: { payload: Partial<UserProfile> }) => {
      if (!state.user) {
        return
      }
      state.user = { ...state.user, ...payload }
      storage.setUser(state.user)
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.user = action.payload.user
        state.token = action.payload.token
        storage.setUser(action.payload.user)
        storage.setToken(action.payload.token)
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'error'
        state.error = action.error.message ?? 'Unable to login'
      })
      .addCase(register.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(register.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.user = action.payload.user
        state.token = action.payload.token
        storage.setUser(action.payload.user)
        storage.setToken(action.payload.token)
      })
      .addCase(register.rejected, (state, action) => {
        state.status = 'error'
        state.error = action.error.message ?? 'Unable to register'
      })
  },
})

export const { logout, updateProfile } = authSlice.actions
export const authReducer = authSlice.reducer
