import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAdminSelector } from '@/admin/app/hooks'

/**
 * Renders the RequireAuth component.
 *
 * @returns The rendered component tree.
 */
export const RequireAuth = (): JSX.Element => {
  const token = useAdminSelector((state) => state.auth.token)
  const location = useLocation()

  if (!token) {
    return <Navigate replace state={{ from: location }} to="/login" />
  }

  return <Outlet />
}

/**
 * Renders the RequireAdmin component.
 *
 * @returns The rendered component tree.
 */
export const RequireAdmin = (): JSX.Element => {
  const user = useAdminSelector((state) => state.auth.user)

  if (!user || !user.roles.includes('ADMIN')) {
    return <Navigate replace to="/login" />
  }

  return <Outlet />
}
