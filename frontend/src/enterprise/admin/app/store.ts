import { configureStore } from '@reduxjs/toolkit'
import { approvalsReducer } from '@/admin/features/approvals/approvalsSlice'
import { authReducer } from '@/admin/features/auth/authSlice'
import { dashboardReducer } from '@/admin/features/dashboard/dashboardSlice'
import { notificationsReducer } from '@/admin/features/notifications/notificationsSlice'
import { ordersReducer } from '@/admin/features/orders/ordersSlice'
import { productsReducer } from '@/admin/features/products/productsSlice'
import { storesReducer } from '@/admin/features/stores/storesSlice'
import { themeReducer } from '@/admin/features/theme/themeSlice'
import { usersReducer } from '@/admin/features/users/usersSlice'

export const adminStore = configureStore({
  reducer: {
    auth: authReducer,
    theme: themeReducer,
    dashboard: dashboardReducer,
    products: productsReducer,
    orders: ordersReducer,
    stores: storesReducer,
    users: usersReducer,
    approvals: approvalsReducer,
    notifications: notificationsReducer,
  },
})

export type RootState = ReturnType<typeof adminStore.getState>
export type AppDispatch = typeof adminStore.dispatch
