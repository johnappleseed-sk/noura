import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { adminApi } from '@/admin/api/adminApi'
import { Order, OrderStatus, PagePayload, RefundStatus } from '@/admin/types'

interface OrdersState {
  items: Order[]
  page: number
  size: number
  totalElements: number
  loading: boolean
  saving: boolean
  error: string | null
}

const initialState: OrdersState = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  loading: false,
  saving: false,
  error: null,
}

export const fetchOrders = createAsyncThunk('orders/fetch', async (_, thunkApi) => {
  try {
    return await adminApi.getOrders({ page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' })
  } catch (error) {
    return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to load orders')
  }
})

export const updateOrderStatus = createAsyncThunk(
  'orders/updateStatus',
  async ({ id, status, refundStatus }: { id: string; status: OrderStatus; refundStatus: RefundStatus }, thunkApi) => {
    try {
      await adminApi.updateOrder(id, status, refundStatus)
      return await adminApi.getOrders({ page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' })
    } catch (error) {
      return thunkApi.rejectWithValue(error instanceof Error ? error.message : 'Unable to update order')
    }
  },
)

const ordersSlice = createSlice({
  name: 'adminOrders',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchOrders.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchOrders.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Order>
        state.loading = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(fetchOrders.rejected, (state, action) => {
        state.loading = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to load orders'
      })
      .addCase(updateOrderStatus.pending, (state) => {
        state.saving = true
        state.error = null
      })
      .addCase(updateOrderStatus.fulfilled, (state, action) => {
        const page = action.payload as PagePayload<Order>
        state.saving = false
        state.items = page.content
        state.page = page.page
        state.size = page.size
        state.totalElements = page.totalElements
      })
      .addCase(updateOrderStatus.rejected, (state, action) => {
        state.saving = false
        state.error = (action.payload as string | undefined) ?? action.error.message ?? 'Unable to update order'
      })
  },
})

export const ordersReducer = ordersSlice.reducer
