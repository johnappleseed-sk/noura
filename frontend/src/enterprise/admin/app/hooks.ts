import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux'
import { AppDispatch, RootState } from '@/admin/app/store'

export const useAdminDispatch = (): AppDispatch => useDispatch<AppDispatch>()
export const useAdminSelector: TypedUseSelectorHook<RootState> = useSelector
