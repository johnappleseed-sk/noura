import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { ordersApi } from '@/api/ordersApi'
import { RootState } from '@/app/store'
import { Order } from '@/types'

interface OrdersState {
  items: Order[]
  status: 'idle' | 'loading' | 'succeeded' | 'failed'
  error: string | null
  lastLoadedAt: number | null
}

const initialState: OrdersState = {
  items: [],
  status: 'idle',
  error: null,
  lastLoadedAt: null,
}

export const fetchMyOrders = createAsyncThunk('orders/fetchMyOrders', async () => ordersApi.getMyOrders())
export const fetchAdminOrders = createAsyncThunk('orders/fetchAdminOrders', async () => ordersApi.getAdminOrders())

const ordersSlice = createSlice({
  name: 'orders',
  initialState,
  reducers: {
    clearOrders: (state) => {
      state.items = []
      state.status = 'idle'
      state.error = null
      state.lastLoadedAt = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMyOrders.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(fetchMyOrders.fulfilled, (state, action) => {
        state.status = 'succeeded'
        state.items = action.payload
        state.lastLoadedAt = Date.now()
      })
      .addCase(fetchMyOrders.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.error.message ?? 'Unable to load your orders'
      })
      .addCase(fetchAdminOrders.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(fetchAdminOrders.fulfilled, (state, action) => {
        state.status = 'succeeded'
        state.items = action.payload
        state.lastLoadedAt = Date.now()
      })
      .addCase(fetchAdminOrders.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.error.message ?? 'Unable to load admin orders'
      })
  },
})

export const { clearOrders } = ordersSlice.actions
export const ordersReducer = ordersSlice.reducer

export const selectOrders = (state: RootState): Order[] => state.orders.items
export const selectOrdersState = (state: RootState): OrdersState => state.orders
