import { createAsyncThunk, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { notificationsApi } from '@/api/notificationsApi'
import { RootState } from '@/app/store'
import { NotificationMessage } from '@/types'

interface NotificationsState {
  items: NotificationMessage[]
  unreadCount: number
  isOpen: boolean
  status: 'idle' | 'loading' | 'failed'
  error: string | null
}

const initialState: NotificationsState = {
  items: [],
  unreadCount: 0,
  isOpen: false,
  status: 'idle',
  error: null,
}

export const fetchNotifications = createAsyncThunk('notifications/fetchNotifications', async () =>
  notificationsApi.myNotifications(),
)
export const fetchUnreadCount = createAsyncThunk('notifications/fetchUnreadCount', async () =>
  notificationsApi.unreadCount(),
)
export const markNotificationRead = createAsyncThunk('notifications/markNotificationRead', async (notificationId: string) =>
  notificationsApi.markAsRead(notificationId),
)
export const markAllNotificationsReadRemote = createAsyncThunk(
  'notifications/markAllNotificationsReadRemote',
  async () => notificationsApi.markAllAsRead(),
)

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    pushNotification: (state, { payload }: PayloadAction<NotificationMessage>) => {
      state.items = [payload, ...state.items].slice(0, 20)
      state.unreadCount += 1
    },
    markAllNotificationsRead: (state) => {
      state.items = state.items.map((item) => ({ ...item, read: true }))
      state.unreadCount = 0
    },
    toggleNotificationsPanel: (state) => {
      state.isOpen = !state.isOpen
    },
    closeNotificationsPanel: (state) => {
      state.isOpen = false
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotifications.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.status = 'idle'
        state.items = action.payload
        state.unreadCount = action.payload.filter((item) => !item.read).length
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.error.message ?? 'Failed to fetch notifications'
      })
      .addCase(fetchUnreadCount.fulfilled, (state, action) => {
        state.unreadCount = action.payload
      })
      .addCase(markNotificationRead.fulfilled, (state, action) => {
        state.items = state.items.map((item) =>
          item.id === action.payload.id ? { ...item, read: true } : item,
        )
        state.unreadCount = Math.max(0, state.unreadCount - 1)
      })
      .addCase(markAllNotificationsReadRemote.fulfilled, (state) => {
        state.items = state.items.map((item) => ({ ...item, read: true }))
        state.unreadCount = 0
      })
  },
})

export const {
  pushNotification,
  markAllNotificationsRead,
  toggleNotificationsPanel,
  closeNotificationsPanel,
} = notificationsSlice.actions

export const selectNotifications = (state: RootState): NotificationsState => state.notifications
export const notificationsReducer = notificationsSlice.reducer
