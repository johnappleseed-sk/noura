import { useEffect, useRef } from 'react'
import { AppProviders } from '@/app/providers'
import { AppRouter } from '@/app/router'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { ErrorBoundary } from '@/components/common/ErrorBoundary'
import { CartItem, hydrateCartState } from '@/features/cart/cartSlice'
import { fetchCmsMenu } from '@/features/cms/cmsSlice'
import { setGeoContext } from '@/features/geo/geoSlice'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { clearOrders, fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'
import { updatePersonalizationSegment } from '@/features/personalization/personalizationSlice'
import { fetchProducts } from '@/features/products/productsSlice'
import { refreshRecommendations } from '@/features/recommendations/recommendationsSlice'
import {
  fetchStores,
  hydrateSelectedStoreId,
  setSuggestedStoreId,
} from '@/features/stores/storesSlice'
import { cartRealtimeSync } from '@/lib/cartRealtimeSync'
import { detectGeoContext } from '@/lib/geoRouting'
import { RealtimeNotificationsClient } from '@/lib/realtimeNotifications'
import { geoContextToCoordinates, getNearestStore } from '@/lib/storeLocator'
import { storage } from '@/lib/storage'

/**
 * Renders the Bootstrap component.
 *
 * @returns The result of bootstrap.
 */
const Bootstrap = (): null => {
  const dispatch = useAppDispatch()
  const status = useAppSelector((state) => state.products.status)
  const items = useAppSelector((state) => state.products.items)
  const history = useAppSelector((state) => state.products.browsingHistory)
  const user = useAppSelector((state) => state.auth.user)
  const userId = user?.id ?? null
  const userRole = user?.role ?? null
  const geoContext = useAppSelector((state) => state.geo.context)
  const stores = useAppSelector((state) => state.stores.availableStores)
  const storesLoading = useAppSelector((state) => state.stores.loading)
  const selectedStoreId = useAppSelector((state) => state.stores.selectedStoreId)
  const cartItems = useAppSelector((state) => state.cart.items)
  const cartCouponCode = useAppSelector((state) => state.cart.couponCode)
  const cartDiscountPercent = useAppSelector((state) => state.cart.discountPercent)
  const cartLastSyncedAt = useAppSelector((state) => state.cart.lastSyncedAt)
  const realtimeClientRef = useRef<RealtimeNotificationsClient | null>(null)

  useEffect(() => {
    if (status === 'idle') {
      void dispatch(fetchProducts())
    }
  }, [dispatch, status])

  useEffect(() => {
    if (!userId) {
      dispatch(clearOrders())
      return
    }

    if (userRole === 'admin') {
      void dispatch(fetchAdminOrders())
      return
    }

    void dispatch(fetchMyOrders())
  }, [dispatch, userId, userRole])

  useEffect(() => {
    void dispatch(fetchCmsMenu())
  }, [dispatch])

  useEffect(() => {
    if (!storesLoading && stores.length === 0) {
      void dispatch(fetchStores())
    }
  }, [dispatch, stores.length, storesLoading])

  useEffect(() => {
    const context = detectGeoContext()
    dispatch(setGeoContext(context))
    dispatch(
      pushNotification({
        id: `geo-${Date.now()}`,
        title: 'Geo-routing context detected',
        description: `Region ${context.region.toUpperCase()} resolved via ${context.source}.`,
        category: 'system',
        createdAt: new Date().toISOString(),
        read: false,
      }),
    )
  }, [dispatch])

  useEffect(() => {
    if (items.length > 0) {
      void dispatch(refreshRecommendations())
    }
  }, [dispatch, items, history, selectedStoreId])

  useEffect(() => {
    if (stores.length === 0) {
      return
    }

    const persistedStoreId = storage.getSelectedStoreId()
    if (persistedStoreId && stores.some((store) => store.id === persistedStoreId)) {
      dispatch(hydrateSelectedStoreId(persistedStoreId))
      return
    }

    if (selectedStoreId && stores.some((store) => store.id === selectedStoreId)) {
      return
    }

    const nearestStore = getNearestStore(stores, geoContextToCoordinates(geoContext))
    dispatch(setSuggestedStoreId(nearestStore?.id ?? null))
  }, [dispatch, geoContext, selectedStoreId, stores])

  useEffect(() => {
    if (user?.role === 'b2b') {
      dispatch(updatePersonalizationSegment('b2b'))
      return
    }
    if (cartItems.length >= 3) {
      dispatch(updatePersonalizationSegment('buyer'))
      return
    }
    if (history.length > 0) {
      dispatch(updatePersonalizationSegment('explorer'))
      return
    }
    dispatch(updatePersonalizationSegment('new'))
  }, [dispatch, user, history, cartItems])

  useEffect(() => {
    if (!selectedStoreId || stores.length === 0) {
      return
    }

    const selected = stores.find((store) => store.id === selectedStoreId)
    if (!selected) {
      return
    }

    dispatch(
      pushNotification({
        id: `store-selected-${selected.id}-${Date.now()}`,
        title: 'Store context updated',
        description: `Prices and inventory now reflect ${selected.name}.`,
        category: 'store',
        createdAt: new Date().toISOString(),
        read: false,
      }),
    )
  }, [dispatch, selectedStoreId, stores])

  useEffect(() => {
    const client = new RealtimeNotificationsClient()
    realtimeClientRef.current = client
    client.start((message) => dispatch(pushNotification(message)))

    return () => client.stop()
  }, [dispatch])

  useEffect(() => {
    const snapshot = cartRealtimeSync.read()
    if (snapshot) {
      dispatch(
        hydrateCartState({
          items: snapshot.items,
          couponCode: snapshot.couponCode,
          discountPercent: snapshot.discountPercent,
          updatedAt: snapshot.updatedAt,
        }),
      )
    }
  }, [dispatch])

  useEffect(() => {
    cartRealtimeSync.write({
      items: cartItems,
      couponCode: cartCouponCode,
      discountPercent: cartDiscountPercent,
      updatedAt: cartLastSyncedAt || Date.now(),
    })
  }, [cartItems, cartCouponCode, cartDiscountPercent, cartLastSyncedAt])

  useEffect(() => {
    /**
     * Executes on storage.
     *
     * @param event The event value.
     * @returns No value.
     */
    const onStorage = (event: StorageEvent): void => {
      if (event.key !== cartRealtimeSync.key || !event.newValue) {
        return
      }
      try {
        const snapshot = JSON.parse(event.newValue) as {
          items: CartItem[]
          couponCode: string | null
          discountPercent: number
          updatedAt: number
        }
        dispatch(
          hydrateCartState({
            items: snapshot.items,
            couponCode: snapshot.couponCode,
            discountPercent: snapshot.discountPercent,
            updatedAt: snapshot.updatedAt,
          }),
        )
      } catch {
        // ignore invalid storage payload
      }
    }

    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [dispatch])

  return null
}

/**
 * Renders the App component.
 *
 * @returns The rendered component tree.
 */
export const App = (): JSX.Element => (
  <AppProviders>
    <ErrorBoundary>
      <Bootstrap />
      <AppRouter />
    </ErrorBoundary>
  </AppProviders>
)
