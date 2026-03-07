import { createAsyncThunk, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { storesApi } from '@/api/storesApi'
import { RootState } from '@/app/store'
import { storage } from '@/lib/storage'
import { StoreLocation } from '@/types'

interface StoresState {
  selectedStoreId: string | null
  selectedStoreDetails: StoreLocation | null
  availableStores: StoreLocation[]
  loading: boolean
  error: string | null
  suggestedStoreId: string | null
  suggestionDismissed: boolean
}

const initialState: StoresState = {
  selectedStoreId: storage.getSelectedStoreId(),
  selectedStoreDetails: null,
  availableStores: [],
  loading: false,
  error: null,
  suggestedStoreId: null,
  suggestionDismissed: storage.getStoreSuggestionDismissed(),
}

/**
 * Executes sync selected store.
 *
 * @param state The state value.
 * @param storeId The store id used to locate the target record.
 * @returns No value.
 */
const syncSelectedStore = (state: StoresState, storeId: string | null): void => {
  state.selectedStoreId = storeId
  state.selectedStoreDetails = storeId
    ? state.availableStores.find((store) => store.id === storeId) ?? null
    : null
}

export const fetchStores = createAsyncThunk('stores/fetchStores', async () => storesApi.getStores())

const storesSlice = createSlice({
  name: 'stores',
  initialState,
  reducers: {
    selectStoreById: (state, { payload }: PayloadAction<string>) => {
      syncSelectedStore(state, payload)
      storage.setSelectedStoreId(payload)
      state.suggestedStoreId = null
      state.suggestionDismissed = false
      storage.setStoreSuggestionDismissed(false)
    },
    clearSelectedStore: (state) => {
      syncSelectedStore(state, null)
      storage.clearSelectedStoreId()
    },
    hydrateSelectedStoreId: (state, { payload }: PayloadAction<string | null>) => {
      if (!payload) {
        syncSelectedStore(state, null)
        return
      }
      syncSelectedStore(state, payload)
    },
    setSuggestedStoreId: (state, { payload }: PayloadAction<string | null>) => {
      state.suggestedStoreId = payload
    },
    dismissStoreSuggestion: (state) => {
      state.suggestionDismissed = true
      storage.setStoreSuggestionDismissed(true)
    },
    resetStoreSuggestionDismissal: (state) => {
      state.suggestionDismissed = false
      storage.setStoreSuggestionDismissed(false)
    },
    upsertStore: (state, { payload }: PayloadAction<StoreLocation>) => {
      const index = state.availableStores.findIndex((store) => store.id === payload.id)
      if (index >= 0) {
        state.availableStores[index] = payload
      } else {
        state.availableStores.push(payload)
      }
      if (state.selectedStoreId === payload.id) {
        syncSelectedStore(state, payload.id)
      }
    },
    deleteStore: (state, { payload }: PayloadAction<string>) => {
      state.availableStores = state.availableStores.filter((store) => store.id !== payload)
      if (state.selectedStoreId === payload) {
        syncSelectedStore(state, null)
        storage.clearSelectedStoreId()
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchStores.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchStores.fulfilled, (state, action) => {
        state.loading = false
        state.availableStores = action.payload
        if (state.selectedStoreId) {
          syncSelectedStore(state, state.selectedStoreId)
        }
      })
      .addCase(fetchStores.rejected, (state, action) => {
        state.loading = false
        state.error = action.error.message ?? 'Unable to load stores.'
      })
  },
})

export const {
  clearSelectedStore,
  dismissStoreSuggestion,
  hydrateSelectedStoreId,
  resetStoreSuggestionDismissal,
  selectStoreById,
  setSuggestedStoreId,
  upsertStore,
  deleteStore,
} = storesSlice.actions

export const selectStoresState = (state: RootState): StoresState => state.stores
export const selectCurrentStore = (state: RootState): StoreLocation | null => state.stores.selectedStoreDetails

export const storesReducer = storesSlice.reducer
