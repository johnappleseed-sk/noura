import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi } from '@/admin/api/adminApi'
import { PagePayload, UserProfile } from '@/admin/types'

interface UsersState {
  items: UserProfile[]
  page: number
  size: number
  totalElements: number
  loading: boolean
  error: string | null
}

const initialState: UsersState = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  loading: false,
  error: null,
}

export const fetchUsers = createAsyncThunk('users/fetch', async (_, thunkApi) => {
  try {
    return await adminApi.getUsers({ page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load users')
  }
})

const usersSlice = createSlice({
  name: 'adminUsers',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchUsers.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchUsers.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<UserProfile>
        state.loading = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(fetchUsers.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load users'
      })
  },
})

export const usersReducer = usersSlice.reducer
