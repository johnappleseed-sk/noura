import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi } from '@/admin/api/adminApi'
import { NotificationPayload } from '@/admin/types'

interface NotificationsState {
  sending: boolean
  successMessage: string | null
  error: string | null
}

const initialState: NotificationsState = {
  sending: false,
  successMessage: null,
  error: null,
}

export const sendBroadcastNotification = createAsyncThunk(
  'notifications/sendBroadcast',
  async (payload: NotificationPayload, thunkApi) => {
    try {
      await adminApi.sendBroadcast(payload)
      return 'Broadcast sent successfully'
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to send broadcast')
    }
  },
)

export const sendUserNotification = createAsyncThunk(
  'notifications/sendUser',
  async ({ userId, payload }: { userId: string; payload: NotificationPayload }, thunkApi) => {
    try {
      await adminApi.sendUserNotification(userId, payload)
      return 'User notification sent successfully'
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to send user notification')
    }
  },
)

const notificationsSlice = createSlice({
  name: 'adminNotifications',
  initialState,
  reducers: {
    clearNotificationStatus: (state) => {
      state.successMessage = null
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(sendBroadcastNotification.pending, (state) => {
        state.sending = true
        state.successMessage = null
        state.error = null
      })
      .addCase(sendBroadcastNotification.fulfilled, (state, action) => {
        state.sending = false
        state.successMessage = action.payload as string
      })
      .addCase(sendBroadcastNotification.rejected, (state, action) => {
        state.sending = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to send broadcast'
      })
      .addCase(sendUserNotification.pending, (state) => {
        state.sending = true
        state.successMessage = null
        state.error = null
      })
      .addCase(sendUserNotification.fulfilled, (state, action) => {
        state.sending = false
        state.successMessage = action.payload as string
      })
      .addCase(sendUserNotification.rejected, (state, action) => {
        state.sending = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to send user notification'
      })
  },
})

export const { clearNotificationStatus } = notificationsSlice.actions
export const notificationsReducer = notificationsSlice.reducer
