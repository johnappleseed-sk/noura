import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi, StoreUpsertRequest } from '@/admin/api/adminApi'
import { PagePayload, Store } from '@/admin/types'

interface StoresState {
  items: Store[]
  page: number
  size: number
  totalElements: number
  loading: boolean
  saving: boolean
  error: string | null
}

const initialState: StoresState = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  loading: false,
  saving: false,
  error: null,
}

export const fetchStores = createAsyncThunk('stores/fetch', async (_, thunkApi) => {
  try {
    return await adminApi.getStores({ page: 0, size: 20, sortBy: 'name', direction: 'asc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load stores')
  }
})

export const createStore = createAsyncThunk('stores/create', async (payload: StoreUpsertRequest, thunkApi) => {
  try {
    await adminApi.createStore(payload)
    return await adminApi.getStores({ page: 0, size: 20, sortBy: 'name', direction: 'asc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to create store')
  }
})

export const updateStore = createAsyncThunk(
  'stores/update',
  async ({ id, payload }: { id: string; payload: StoreUpsertRequest }, thunkApi) => {
    try {
      await adminApi.updateStore(id, payload)
      return await adminApi.getStores({ page: 0, size: 20, sortBy: 'name', direction: 'asc' })
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to update store')
    }
  },
)

export const deleteStore = createAsyncThunk('stores/delete', async (id: string, thunkApi) => {
  try {
    await adminApi.deleteStore(id)
    return await adminApi.getStores({ page: 0, size: 20, sortBy: 'name', direction: 'asc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to delete store')
  }
})

const storesSlice = createSlice({
  name: 'adminStores',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchStores.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchStores.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Store>
        state.loading = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(fetchStores.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load stores'
      })
      .addCase(createStore.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(updateStore.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(deleteStore.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(createStore.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Store>
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(updateStore.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Store>
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(deleteStore.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Store>
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(createStore.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to create store'
      })
      .addCase(updateStore.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to update store'
      })
      .addCase(deleteStore.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to delete store'
      })
  },
})

export const storesReducer = storesSlice.reducer
