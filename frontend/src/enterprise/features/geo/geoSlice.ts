import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/app/store'
import { CurrencyCode, GeoContext, GeoRegion, LanguageCode } from '@/types'

interface GeoState {
  context: GeoContext
}

const initialState: GeoState = {
  context: {
    region: 'global',
    countryCode: 'US',
    locale: 'en-US',
    currency: 'USD',
    language: 'en-US',
    source: 'client-fallback',
  },
}

const geoSlice = createSlice({
  name: 'geo',
  initialState,
  reducers: {
    setGeoContext: (state, { payload }: PayloadAction<GeoContext>) => {
      state.context = payload
    },
    setRegion: (state, { payload }: PayloadAction<GeoRegion>) => {
      state.context.region = payload
    },
    setCurrency: (state, { payload }: PayloadAction<CurrencyCode>) => {
      state.context.currency = payload
    },
    setLanguage: (state, { payload }: PayloadAction<LanguageCode>) => {
      state.context.language = payload
      state.context.locale = payload
    },
  },
})

export const { setGeoContext, setRegion, setCurrency, setLanguage } = geoSlice.actions
export const selectGeoContext = (state: RootState): GeoContext => state.geo.context
export const geoReducer = geoSlice.reducer
