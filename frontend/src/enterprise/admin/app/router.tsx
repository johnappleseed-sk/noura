import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AdminLayout } from '@/admin/components/AdminLayout'
import { RequireAdmin, RequireAuth } from '@/admin/components/RequireAuth'
import { ApprovalsPage } from '@/admin/pages/ApprovalsPage'
import { DashboardPage } from '@/admin/pages/DashboardPage'
import { LoginPage } from '@/admin/pages/LoginPage'
import { NotificationsPage } from '@/admin/pages/NotificationsPage'
import { OrdersPage } from '@/admin/pages/OrdersPage'
import { ProductsPage } from '@/admin/pages/ProductsPage'
import { StoresPage } from '@/admin/pages/StoresPage'
import { UsersPage } from '@/admin/pages/UsersPage'

/**
 * Renders the AdminRouter component.
 *
 * @returns The rendered component tree.
 */
export const AdminRouter = (): JSX.Element => (
  <BrowserRouter>
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<RequireAuth />}>
        <Route element={<RequireAdmin />}>
          <Route element={<AdminLayout />}>
            <Route element={<Navigate replace to="/dashboard" />} path="/" />
            <Route element={<DashboardPage />} path="/dashboard" />
            <Route element={<ProductsPage />} path="/products" />
            <Route element={<OrdersPage />} path="/orders" />
            <Route element={<StoresPage />} path="/stores" />
            <Route element={<UsersPage />} path="/users" />
            <Route element={<ApprovalsPage />} path="/approvals" />
            <Route element={<NotificationsPage />} path="/notifications" />
            <Route element={<Navigate replace to="/dashboard" />} path="*" />
          </Route>
        </Route>
      </Route>
    </Routes>
  </BrowserRouter>
)
