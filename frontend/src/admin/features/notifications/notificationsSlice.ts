import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import { adminApi } from '@/api/adminApi';
import type { RootState } from '@/app/store';
import type { NotificationItem } from '@/types/models';

interface NotificationsState {
  items: NotificationItem[];
  unreadCount: number;
  panelOpen: boolean;
  status: 'idle' | 'loading' | 'failed';
  error: string | null;
  lastRefreshedAt: number | null;
}

const initialState: NotificationsState = {
  items: [],
  unreadCount: 0,
  panelOpen: false,
  status: 'idle',
  error: null,
  lastRefreshedAt: null,
};

const normalizeError = (error: unknown): string =>
  error instanceof Error ? error.message : 'Notification request failed';

export const fetchNotifications = createAsyncThunk<NotificationItem[], void, { rejectValue: string }>(
  'notifications/fetchNotifications',
  async (_, thunkApi) => {
    try {
      return await adminApi.getMyNotifications();
    } catch (error) {
      return thunkApi.rejectWithValue(normalizeError(error));
    }
  },
);

export const fetchUnreadCount = createAsyncThunk<number, void, { rejectValue: string }>(
  'notifications/fetchUnreadCount',
  async (_, thunkApi) => {
    try {
      return await adminApi.getUnreadNotificationsCount();
    } catch (error) {
      return thunkApi.rejectWithValue(normalizeError(error));
    }
  },
);

export const markNotificationRead = createAsyncThunk<NotificationItem, string, { rejectValue: string }>(
  'notifications/markNotificationRead',
  async (notificationId, thunkApi) => {
    try {
      return await adminApi.markNotificationRead(notificationId);
    } catch (error) {
      return thunkApi.rejectWithValue(normalizeError(error));
    }
  },
);

export const markAllNotificationsRead = createAsyncThunk<number, void, { rejectValue: string }>(
  'notifications/markAllNotificationsRead',
  async (_, thunkApi) => {
    try {
      return await adminApi.markAllNotificationsRead();
    } catch (error) {
      return thunkApi.rejectWithValue(normalizeError(error));
    }
  },
);

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setPanelOpen: (state, action: PayloadAction<boolean>) => {
      state.panelOpen = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotifications.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.status = 'idle';
        state.items = action.payload;
        state.unreadCount = action.payload.filter((item) => !item.read).length;
        state.lastRefreshedAt = Date.now();
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Failed to fetch notifications';
      })
      .addCase(fetchUnreadCount.fulfilled, (state, action) => {
        state.unreadCount = action.payload;
      })
      .addCase(markNotificationRead.fulfilled, (state, action) => {
        state.items = state.items.map((item) =>
          item.id === action.payload.id ? { ...item, read: true } : item,
        );
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      })
      .addCase(markAllNotificationsRead.fulfilled, (state) => {
        state.items = state.items.map((item) => ({ ...item, read: true }));
        state.unreadCount = 0;
      });
  },
});

export const { setPanelOpen } = notificationsSlice.actions;
export const notificationsReducer = notificationsSlice.reducer;

export const selectNotificationsState = (state: RootState): NotificationsState => state.notifications;
export const selectNotifications = (state: RootState): NotificationItem[] => state.notifications.items;
export const selectUnreadCount = (state: RootState): number => state.notifications.unreadCount;
