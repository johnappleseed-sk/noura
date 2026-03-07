import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi } from '@/admin/api/adminApi'
import { DashboardSummary } from '@/admin/types'

interface DashboardState {
  summary: DashboardSummary | null
  loading: boolean
  error: string | null
}

const initialState: DashboardState = {
  summary: null,
  loading: false,
  error: null,
}

export const fetchDashboardSummary = createAsyncThunk('dashboard/fetchSummary', async (_, thunkApi) => {
  try {
    return await adminApi.getDashboardSummary()
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load summary')
  }
})

const dashboardSlice = createSlice({
  name: 'dashboard',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchDashboardSummary.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchDashboardSummary.fulfilled, (state, action) => {
        state.loading = false
        state.summary = action.payload as DashboardSummary
      })
      .addCase(fetchDashboardSummary.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load summary'
      })
  },
})

export const dashboardReducer = dashboardSlice.reducer
