import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi, ProductUpsertRequest } from '@/admin/api/adminApi'
import { PagePayload, Product } from '@/admin/types'

interface ProductsState {
  items: Product[]
  page: number
  size: number
  totalElements: number
  loading: boolean
  saving: boolean
  error: string | null
}

const initialState: ProductsState = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  loading: false,
  saving: false,
  error: null,
}

interface ProductQuery {
  page: number
  size: number
  sortBy: string
  direction: 'asc' | 'desc'
}

const toPageResult = (payload: PagePayload<Product>) => payload

export const fetchProducts = createAsyncThunk('products/fetch', async (query: ProductQuery, thunkApi) => {
  try {
    return await adminApi.getProducts(query)
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load products')
  }
})

export const createProduct = createAsyncThunk('products/create', async (payload: ProductUpsertRequest, thunkApi) => {
  try {
    await adminApi.createProduct(payload)
    return await adminApi.getProducts({ page: 0, size: 10, sortBy: 'createdAt', direction: 'desc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to create product')
  }
})

export const updateProduct = createAsyncThunk(
  'products/update',
  async ({ id, payload }: { id: string; payload: ProductUpsertRequest }, thunkApi) => {
    try {
      await adminApi.updateProduct(id, payload)
      return await adminApi.getProducts({ page: 0, size: 10, sortBy: 'createdAt', direction: 'desc' })
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to update product')
    }
  },
)

export const deleteProduct = createAsyncThunk('products/delete', async (id: string, thunkApi) => {
  try {
    await adminApi.deleteProduct(id)
    return await adminApi.getProducts({ page: 0, size: 10, sortBy: 'createdAt', direction: 'desc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to delete product')
  }
})

const productsSlice = createSlice({
  name: 'adminProducts',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        const page = toPageResult(action.payload as PagePayload<Product>)
        state.loading = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load products'
      })
      .addCase(createProduct.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(updateProduct.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(deleteProduct.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(createProduct.fulfilled, (state, action) => {
        const page = toPageResult(action.payload as PagePayload<Product>)
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(updateProduct.fulfilled, (state, action) => {
        const page = toPageResult(action.payload as PagePayload<Product>)
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(deleteProduct.fulfilled, (state, action) => {
        const page = toPageResult(action.payload as PagePayload<Product>)
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(createProduct.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to create product'
      })
      .addCase(updateProduct.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to update product'
      })
      .addCase(deleteProduct.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to delete product'
      })
  },
})

export const productsReducer = productsSlice.reducer
