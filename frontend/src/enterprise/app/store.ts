import { configureStore } from '@reduxjs/toolkit'
import { authReducer } from '@/features/auth/authSlice'
import { cartReducer } from '@/features/cart/cartSlice'
import { cmsReducer } from '@/features/cms/cmsSlice'
import { geoReducer } from '@/features/geo/geoSlice'
import { notificationsReducer } from '@/features/notifications/notificationsSlice'
import { ordersReducer } from '@/features/orders/ordersSlice'
import { personalizationReducer } from '@/features/personalization/personalizationSlice'
import { productsReducer } from '@/features/products/productsSlice'
import { recommendationsReducer } from '@/features/recommendations/recommendationsSlice'
import { storesReducer } from '@/features/stores/storesSlice'
import { uiReducer } from '@/features/ui/uiSlice'
import { wishlistReducer } from '@/features/wishlist/wishlistSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    cart: cartReducer,
    cms: cmsReducer,
    geo: geoReducer,
    notifications: notificationsReducer,
    orders: ordersReducer,
    personalization: personalizationReducer,
    products: productsReducer,
    recommendations: recommendationsReducer,
    stores: storesReducer,
    ui: uiReducer,
    wishlist: wishlistReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
