import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { Product } from '@/types'
import { RootState } from '@/app/store'

export interface CartItem {
  productId: string
  name: string
  image: string
  price: number
  quantity: number
  storeId: string | null
  storeName?: string
}

interface CartState {
  items: CartItem[]
  couponCode: string | null
  discountPercent: number
  lastSyncedAt: number
}

interface CartLinePayload {
  productId: string
  name: string
  image: string
  price: number
  quantity: number
  storeId?: string | null
  storeName?: string
}

interface AddToCartPayload {
  product: Product
  storeId: string | null
  storeName?: string
}

const couponMap: Record<string, number> = {
  SAVE10: 10,
  PREMIUM15: 15,
  FREESHIP5: 5,
}

const initialState: CartState = {
  items: [],
  couponCode: null,
  discountPercent: 0,
  lastSyncedAt: 0,
}

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    addToCart: (state, { payload }: PayloadAction<AddToCartPayload>) => {
      const { product, storeId, storeName } = payload
      const existing = state.items.find((item) => item.productId === product.id)
      if (existing) {
        existing.quantity += 1
        existing.storeId = storeId
        existing.storeName = storeName
      } else {
        state.items.push({
          productId: product.id,
          name: product.name,
          image: product.images[0],
          price: product.price,
          quantity: 1,
          storeId,
          storeName,
        })
      }
      state.lastSyncedAt = Date.now()
    },
    removeFromCart: (state, { payload }: PayloadAction<string>) => {
      state.items = state.items.filter((item) => item.productId !== payload)
      state.lastSyncedAt = Date.now()
    },
    updateQuantity: (state, { payload }: PayloadAction<{ productId: string; quantity: number }>) => {
      const existing = state.items.find((item) => item.productId === payload.productId)
      if (!existing) {
        return
      }
      existing.quantity = Math.max(1, payload.quantity)
      state.lastSyncedAt = Date.now()
    },
    clearCart: (state) => {
      state.items = []
      state.couponCode = null
      state.discountPercent = 0
      state.lastSyncedAt = Date.now()
    },
    applyCoupon: (state, { payload }: PayloadAction<string>) => {
      const normalizedCode = payload.trim().toUpperCase()
      state.couponCode = normalizedCode
      state.discountPercent = couponMap[normalizedCode] ?? 0
      state.lastSyncedAt = Date.now()
    },
    addBulkItems: (state, { payload }: PayloadAction<CartLinePayload[]>) => {
      for (const line of payload) {
        const existing = state.items.find((item) => item.productId === line.productId)
        if (existing) {
          existing.quantity += Math.max(1, line.quantity)
        } else {
          state.items.push({
            productId: line.productId,
            name: line.name,
            image: line.image,
            price: line.price,
            quantity: Math.max(1, line.quantity),
            storeId: line.storeId ?? null,
            storeName: line.storeName,
          })
        }
      }
      state.lastSyncedAt = Date.now()
    },
    hydrateCartState: (
      state,
      { payload }: PayloadAction<{ items: CartItem[]; couponCode: string | null; discountPercent: number; updatedAt: number }>,
    ) => {
      if (payload.updatedAt <= state.lastSyncedAt) {
        return
      }
      state.items = payload.items
      state.couponCode = payload.couponCode
      state.discountPercent = payload.discountPercent
      state.lastSyncedAt = payload.updatedAt
    },
  },
})

export const {
  addToCart,
  removeFromCart,
  updateQuantity,
  clearCart,
  applyCoupon,
  addBulkItems,
  hydrateCartState,
} = cartSlice.actions

export const selectCartItems = (state: RootState): CartItem[] => state.cart.items
export const selectCartCount = createSelector([selectCartItems], (items) =>
  items.reduce((acc, item) => acc + item.quantity, 0),
)

export const selectCartTotals = createSelector(
  [
    (state: RootState) => state.cart.items,
    (state: RootState) => state.cart.discountPercent,
    (state: RootState) => state.stores.selectedStoreDetails?.freeShippingThreshold ?? 300,
    (state: RootState) => state.stores.selectedStoreDetails?.shippingFee ?? 14,
  ],
  (items, discountPercent, freeShippingThreshold, shippingFee) => {
    const subtotal = items.reduce((acc, item) => acc + item.price * item.quantity, 0)
    const discount = (subtotal * discountPercent) / 100
    const shipping = subtotal >= freeShippingThreshold || subtotal === 0 ? 0 : shippingFee
    const total = subtotal - discount + shipping
    return { subtotal, discount, shipping, total, freeShippingThreshold }
  },
)

export const cartReducer = cartSlice.reducer
