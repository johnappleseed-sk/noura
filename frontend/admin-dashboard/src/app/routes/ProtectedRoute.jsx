import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import { hasAnyRole } from '../../shared/auth/roles'
import { Spinner } from '../../shared/ui/Spinner'

export function ProtectedRoute({ allowedRoles = [] }) {
  const { auth, initializing, isAuthenticated } = useAuth()
  const location = useLocation()

  if (initializing) {
    return <Spinner label="Checking session..." />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (!hasAnyRole(auth?.roles, allowedRoles)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <Outlet />
}
