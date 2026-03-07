import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'

/**
 * Renders the AdminRoute component.
 *
 * @returns The rendered component tree.
 */
export const AdminRoute = (): JSX.Element => {
  const user = useAppSelector((state) => state.auth.user)

  if (!user || user.role !== 'admin') {
    return <Navigate replace to="/" />
  }

  return <Outlet />
}
