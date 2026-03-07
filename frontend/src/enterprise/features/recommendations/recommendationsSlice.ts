import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { recommendationsApi } from '@/api/recommendationsApi'
import { RootState } from '@/app/store'
import { rankRecommendations } from '@/utils/recommendationEngine'

interface RecommendationsState {
  productIds: string[]
  status: 'idle' | 'loading' | 'ready'
  source: 'backend' | 'local'
  reason: string
  lastSyncedAt: number | null
}

const initialState: RecommendationsState = {
  productIds: [],
  status: 'idle',
  source: 'local',
  reason: '',
  lastSyncedAt: null,
}

const localRecommendations = (state: RootState, limit = 6): string[] =>
  rankRecommendations(
    state.products.items,
    state.products.browsingHistory,
    { limit },
  ).map((product) => product.id)

export const refreshRecommendations = createAsyncThunk(
  'recommendations/refresh',
  async (_, { getState }) => {
    const state = getState() as RootState
    try {
      const response = await recommendationsApi.aiRanked()
      const knownProductIds = new Set(state.products.items.map((product) => product.id))
      const backendIds = response.products
        .map((product) => product.id)
        .filter((id) => knownProductIds.has(id))
      const ids = backendIds.length > 0 ? backendIds : localRecommendations(state, 6)
      return {
        ids,
        source: backendIds.length > 0 ? 'backend' : 'local',
        reason: backendIds.length > 0 ? `${response.engine}: ${response.reason}` : 'Local fallback ranking',
      } as const
    } catch {
      return {
        ids: localRecommendations(state, 6),
        source: 'local',
        reason: 'Local fallback ranking',
      } as const
    }
  },
)

const recommendationsSlice = createSlice({
  name: 'recommendations',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(refreshRecommendations.pending, (state) => {
        state.status = 'loading'
      })
      .addCase(refreshRecommendations.fulfilled, (state, action) => {
        state.status = 'ready'
        state.productIds = action.payload.ids
        state.source = action.payload.source
        state.reason = action.payload.reason
        state.lastSyncedAt = Date.now()
      })
      .addCase(refreshRecommendations.rejected, (state) => {
        state.status = 'idle'
      })
  },
})

/**
 * Executes select recommended products.
 *
 * @param state The state value.
 * @returns The result of select recommended products.
 */
export const selectRecommendedProducts = (state: RootState) =>
  state.recommendations.productIds
    .map((id) => state.products.items.find((product) => product.id === id))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))

export const selectRecommendationReason = (state: RootState): string => state.recommendations.reason
export const selectRecommendationSource = (state: RootState): 'backend' | 'local' =>
  state.recommendations.source

export const recommendationsReducer = recommendationsSlice.reducer
