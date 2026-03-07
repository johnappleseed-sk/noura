import { applyCoupon, cartReducer, clearCart, addToCart } from '@/features/cart/cartSlice'
import { mockProducts } from '@/data'

describe('cartSlice', () => {
  it('applies coupons and clears cart', () => {
    const initialState = cartReducer(undefined, { type: 'unknown' })
    const withItem = cartReducer(
      initialState,
      addToCart({ product: mockProducts[0], storeId: 'store-nyc-01', storeName: 'Noura Manhattan Flagship' }),
    )
    const withCoupon = cartReducer(withItem, applyCoupon('SAVE10'))

    expect(withCoupon.discountPercent).toBe(10)
    expect(withCoupon.items).toHaveLength(1)

    const cleared = cartReducer(withCoupon, clearCart())
    expect(cleared.items).toHaveLength(0)
    expect(cleared.discountPercent).toBe(0)
  })
})
