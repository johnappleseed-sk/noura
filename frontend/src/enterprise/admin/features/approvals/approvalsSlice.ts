import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi } from '@/admin/api/adminApi'
import { Approval } from '@/admin/types'

interface ApprovalsState {
  items: Approval[]
  loading: boolean
  saving: boolean
  error: string | null
}

const initialState: ApprovalsState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
}

export const fetchApprovals = createAsyncThunk('approvals/fetch', async (_, thunkApi) => {
  try {
    return await adminApi.getApprovals()
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load approvals')
  }
})

export const updateApproval = createAsyncThunk(
  'approvals/update',
  async ({ id, status, notes }: { id: string; status: Approval['status']; notes: string }, thunkApi) => {
    try {
      await adminApi.updateApproval(id, status, notes)
      return await adminApi.getApprovals()
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to update approval')
    }
  },
)

const approvalsSlice = createSlice({
  name: 'adminApprovals',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchApprovals.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchApprovals.fulfilled, (state, action) => {
        state.loading = false
        state.items = action.payload as Approval[]
      })
      .addCase(fetchApprovals.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load approvals'
      })
      .addCase(updateApproval.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(updateApproval.fulfilled, (state, action) => {
        state.saving = false
        state.items = action.payload as Approval[]
      })
      .addCase(updateApproval.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to update approval'
      })
  },
})

export const approvalsReducer = approvalsSlice.reducer
