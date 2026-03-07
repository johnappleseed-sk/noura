export interface ProductReview {
  id: string
  user: string
  rating: number
  comment: string
  createdAt: string
}

export interface Product {
  id: string
  name: string
  description: string
  category: string
  brand: string
  price: number
  originalPrice?: number
  rating: number
  reviewCount: number
  stock: number
  popularity: number
  createdAt: string
  images: string[]
  tags: string[]
  features: string[]
  reviews: ProductReview[]
  storeInventory?: Record<string, number>
  pickupAvailableStoreIds?: string[]
  exclusiveStoreIds?: string[]
}

export interface ProductFilters {
  searchQuery: string
  categories: string[]
  brands: string[]
  minPrice: number
  maxPrice: number
  minRating: number
  availableAtMyStore?: boolean
}

export type ProductSortBy = 'price-asc' | 'price-desc' | 'popularity' | 'newest' | 'store-availability'
