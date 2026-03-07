import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/app/store'

interface WishlistState {
  productIds: string[]
}

const initialState: WishlistState = {
  productIds: [],
}

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    toggleWishlist: (state, { payload }: PayloadAction<string>) => {
      if (state.productIds.includes(payload)) {
        state.productIds = state.productIds.filter((id) => id !== payload)
      } else {
        state.productIds.push(payload)
      }
    },
    clearWishlist: (state) => {
      state.productIds = []
    },
  },
})

export const { toggleWishlist, clearWishlist } = wishlistSlice.actions
export const selectWishlistCount = (state: RootState): number => state.wishlist.productIds.length
/**
 * Executes select is wishlisted.
 *
 * @param state The state value.
 * @param productId The product id used to locate the target record.
 * @returns The result of select is wishlisted.
 */
export const selectIsWishlisted = (state: RootState, productId: string): boolean =>
  state.wishlist.productIds.includes(productId)
export const wishlistReducer = wishlistSlice.reducer
