import { Order } from '@/types'

export const mockOrders: Order[] = [
  {
    id: 'ord-2026-001',
    createdAt: '2026-02-24T11:15:00.000Z',
    status: 'shipped',
    total: 1678,
    storeId: 'store-nyc-01',
    storeName: 'Noura Manhattan Flagship',
    items: [
      { productId: 'p-001', name: 'Atlas Pro Laptop 14"', quantity: 1, price: 1499, storeId: 'store-nyc-01' },
      { productId: 'p-009', name: 'TerraBlend Eco Water Bottle', quantity: 1, price: 49, storeId: 'store-nyc-01' },
      { productId: 'p-010', name: 'UrbanLoom Minimal Backpack', quantity: 1, price: 119, storeId: 'store-nyc-01' },
    ],
  },
  {
    id: 'ord-2026-002',
    createdAt: '2026-02-10T17:40:00.000Z',
    status: 'delivered',
    total: 628,
    storeId: 'store-sf-01',
    storeName: 'Noura Bay Store',
    items: [
      { productId: 'p-004', name: 'Vertex Ergonomic Office Chair', quantity: 1, price: 599, storeId: 'store-sf-01' },
      { productId: 'p-009', name: 'TerraBlend Eco Water Bottle', quantity: 1, price: 29, storeId: 'store-sf-01' },
    ],
  },
]
