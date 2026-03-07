export type OrderStatus =
  | 'created'
  | 'reviewed'
  | 'payment_pending'
  | 'paid'
  | 'packed'
  | 'shipped'
  | 'delivered'
  | 'cancelled'
  | 'refunded'

export interface OrderItem {
  productId: string
  name: string
  quantity: number
  price: number
  storeId?: string | null
}

export interface Order {
  id: string
  createdAt: string
  status: OrderStatus
  total: number
  items: OrderItem[]
  storeId?: string | null
  storeName?: string
}
