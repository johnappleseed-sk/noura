import { createAsyncThunk, createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { productsApi } from '@/api/productsApi'
import { getProductStockAtStore } from '@/lib/productAvailability'
import { storage } from '@/lib/storage'
import { Product, ProductFilters, ProductSortBy } from '@/types'
import { RootState } from '@/app/store'

interface ProductsState {
  items: Product[]
  status: 'idle' | 'loading' | 'succeeded' | 'failed'
  error: string | null
  filters: ProductFilters
  sortBy: ProductSortBy
  currentPage: number
  pageSize: number
  browsingHistory: string[]
}

const DEFAULT_MAX_PRICE = 10000

const initialState: ProductsState = {
  items: [],
  status: 'idle',
  error: null,
  filters: {
    searchQuery: '',
    categories: [],
    brands: [],
    minPrice: 0,
    maxPrice: DEFAULT_MAX_PRICE,
    minRating: 0,
    availableAtMyStore: false,
  },
  sortBy: 'popularity',
  currentPage: 1,
  pageSize: 8,
  browsingHistory: storage.getHistory(),
}

export const fetchProducts = createAsyncThunk('products/fetchProducts', async () => productsApi.getProducts())

const productsSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    setSearchQuery: (state, { payload }: PayloadAction<string>) => {
      state.filters.searchQuery = payload
      state.currentPage = 1
    },
    setCategoryFilters: (state, { payload }: PayloadAction<string[]>) => {
      state.filters.categories = payload
      state.currentPage = 1
    },
    setBrandFilters: (state, { payload }: PayloadAction<string[]>) => {
      state.filters.brands = payload
      state.currentPage = 1
    },
    setPriceRange: (state, { payload }: PayloadAction<{ minPrice: number; maxPrice: number }>) => {
      state.filters.minPrice = payload.minPrice
      state.filters.maxPrice = payload.maxPrice
      state.currentPage = 1
    },
    setMinRating: (state, { payload }: PayloadAction<number>) => {
      state.filters.minRating = payload
      state.currentPage = 1
    },
    setAvailableAtMyStore: (state, { payload }: PayloadAction<boolean>) => {
      state.filters.availableAtMyStore = payload
      state.currentPage = 1
    },
    resetFilters: (state) => {
      state.filters = {
        searchQuery: '',
        categories: [],
        brands: [],
        minPrice: 0,
        maxPrice: DEFAULT_MAX_PRICE,
        minRating: 0,
        availableAtMyStore: false,
      }
      state.currentPage = 1
    },
    setSortBy: (state, { payload }: PayloadAction<ProductSortBy>) => {
      state.sortBy = payload
    },
    setCurrentPage: (state, { payload }: PayloadAction<number>) => {
      state.currentPage = Math.max(1, payload)
    },
    trackProductView: (state, { payload }: PayloadAction<string>) => {
      state.browsingHistory = storage.pushHistory(payload)
    },
    upsertProduct: (state, { payload }: PayloadAction<Product>) => {
      const existingIndex = state.items.findIndex((item) => item.id === payload.id)
      if (existingIndex === -1) {
        state.items.push(payload)
      } else {
        state.items[existingIndex] = payload
      }
    },
    deleteProduct: (state, { payload }: PayloadAction<string>) => {
      state.items = state.items.filter((item) => item.id !== payload)
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.status = 'succeeded'
        state.items = action.payload
        const ceiling =
          action.payload.length > 0
            ? Math.ceil(Math.max(...action.payload.map((item) => item.price)))
            : DEFAULT_MAX_PRICE
        state.filters.maxPrice = Math.max(ceiling, state.filters.minPrice)
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.error.message ?? 'Unable to load products'
      })
  },
})

export const {
  setSearchQuery,
  setCategoryFilters,
  setBrandFilters,
  setPriceRange,
  setMinRating,
  setAvailableAtMyStore,
  resetFilters,
  setSortBy,
  setCurrentPage,
  trackProductView,
  upsertProduct,
  deleteProduct,
} = productsSlice.actions

export const selectProductsState = (state: RootState): ProductsState => state.products

export const selectFilterOptions = createSelector([selectProductsState], (productsState) => {
  const categories = Array.from(new Set(productsState.items.map((item) => item.category)))
  const brands = Array.from(new Set(productsState.items.map((item) => item.brand)))
  const highestPrice = Math.ceil(
    Math.max(0, ...productsState.items.map((product) => product.price), productsState.filters.maxPrice),
  )

  return { categories, brands, highestPrice }
})

export const selectFilteredProducts = createSelector(
  [selectProductsState, (state: RootState) => state.stores.selectedStoreId],
  (productsState, selectedStoreId) => {
    const { items, filters } = productsState
  const query = filters.searchQuery.trim().toLowerCase()

    return items.filter((product) => {
      const matchesQuery =
        query.length === 0 ||
        product.name.toLowerCase().includes(query) ||
        product.description.toLowerCase().includes(query) ||
        product.tags.some((tag) => tag.toLowerCase().includes(query))

      const matchesCategory =
        filters.categories.length === 0 || filters.categories.includes(product.category)
      const matchesBrand = filters.brands.length === 0 || filters.brands.includes(product.brand)
      const matchesPrice = product.price >= filters.minPrice && product.price <= filters.maxPrice
      const matchesRating = product.rating >= filters.minRating
      const availableAtStore =
        !filters.availableAtMyStore || getProductStockAtStore(product, selectedStoreId) > 0

      return matchesQuery && matchesCategory && matchesBrand && matchesPrice && matchesRating && availableAtStore
    })
  },
)

export const selectSortedProducts = createSelector(
  [selectFilteredProducts, (state: RootState) => state.products.sortBy, (state: RootState) => state.stores.selectedStoreId],
  (filteredProducts, sortBy, selectedStoreId) => {
    const copy = [...filteredProducts]
    copy.sort((left, right) => {
      switch (sortBy) {
        case 'price-asc':
          return left.price - right.price
        case 'price-desc':
          return right.price - left.price
        case 'newest':
          return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
        case 'store-availability':
          return getProductStockAtStore(right, selectedStoreId) - getProductStockAtStore(left, selectedStoreId)
        case 'popularity':
        default:
          return right.popularity - left.popularity
      }
    })
    return copy
  },
)

export const selectPaginatedProducts = createSelector(
  [selectSortedProducts, (state: RootState) => state.products.currentPage, (state: RootState) => state.products.pageSize],
  (sortedProducts, currentPage, pageSize) => {
    const start = (currentPage - 1) * pageSize
    return sortedProducts.slice(start, start + pageSize)
  },
)

export const selectPaginationMeta = createSelector(
  [selectSortedProducts, (state: RootState) => state.products.currentPage, (state: RootState) => state.products.pageSize],
  (sortedProducts, currentPage, pageSize) => {
    const totalItems = sortedProducts.length
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
    return { totalItems, totalPages, currentPage, pageSize }
  },
)

/**
 * Executes select product by id.
 *
 * @param state The state value.
 * @param productId The product id used to locate the target record.
 * @returns The result of select product by id.
 */
export const selectProductById = (state: RootState, productId: string): Product | undefined =>
  state.products.items.find((item) => item.id === productId)

export const productsReducer = productsSlice.reducer
