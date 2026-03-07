import { CartItem } from '@/features/cart/cartSlice'

const CART_STATE_KEY = 'enterprise_cart_state'

interface CartSnapshot {
  items: CartItem[]
  couponCode: string | null
  discountPercent: number
  updatedAt: number
}

export const cartRealtimeSync = {
  key: CART_STATE_KEY,
  read: (): CartSnapshot | null => {
    const raw = localStorage.getItem(CART_STATE_KEY)
    return raw ? (JSON.parse(raw) as CartSnapshot) : null
  },
  write: (snapshot: CartSnapshot): void => {
    localStorage.setItem(CART_STATE_KEY, JSON.stringify(snapshot))
  },
}
