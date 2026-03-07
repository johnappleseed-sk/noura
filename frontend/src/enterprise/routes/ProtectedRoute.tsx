import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'

/**
 * Renders the ProtectedRoute component.
 *
 * @returns The rendered component tree.
 */
export const ProtectedRoute = (): JSX.Element => {
  const isAuthenticated = useAppSelector((state) => Boolean(state.auth.token))
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate replace state={{ from: location }} to="/login" />
  }

  return <Outlet />
}
