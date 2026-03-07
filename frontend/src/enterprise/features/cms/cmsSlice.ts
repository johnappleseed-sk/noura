import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { RootState } from '@/app/store'
import { cmsApi } from '@/api/cmsApi'
import { CmsMenuItem } from '@/types'

interface CmsState {
  menuItems: CmsMenuItem[]
  status: 'idle' | 'loading' | 'ready'
}

const initialState: CmsState = {
  menuItems: [],
  status: 'idle',
}

export const fetchCmsMenu = createAsyncThunk('cms/fetchMenu', async () => cmsApi.getMenuItems())

const cmsSlice = createSlice({
  name: 'cms',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCmsMenu.pending, (state) => {
        state.status = 'loading'
      })
      .addCase(fetchCmsMenu.fulfilled, (state, action) => {
        state.status = 'ready'
        state.menuItems = action.payload
      })
      .addCase(fetchCmsMenu.rejected, (state) => {
        state.status = 'ready'
      })
  },
})

export const selectCmsMenu = (state: RootState): CmsMenuItem[] => state.cms.menuItems
export const cmsReducer = cmsSlice.reducer
