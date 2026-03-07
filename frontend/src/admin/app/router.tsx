import { lazy, Suspense } from 'react';
import { Navigate, BrowserRouter, Route, Routes } from 'react-router-dom';
import { RouteLoader } from '@/components/RouteLoader';
import { ProtectedRoute } from '@/routes/ProtectedRoute';
import { RoleRoute } from '@/routes/RoleRoute';

const AdminLayout = lazy(async () => ({ default: (await import('@/layouts/AdminLayout')).AdminLayout }));
const LoginPage = lazy(async () => ({ default: (await import('@/pages/LoginPage')).LoginPage }));
const DashboardPage = lazy(async () => ({ default: (await import('@/pages/DashboardPage')).DashboardPage }));
const AnalyticsPage = lazy(async () => ({ default: (await import('@/pages/AnalyticsPage')).AnalyticsPage }));
const ProductsPage = lazy(async () => ({ default: (await import('@/pages/ProductsPage')).ProductsPage }));
const OrdersPage = lazy(async () => ({ default: (await import('@/pages/OrdersPage')).OrdersPage }));
const StoresPage = lazy(async () => ({ default: (await import('@/pages/StoresPage')).StoresPage }));
const UsersPage = lazy(async () => ({ default: (await import('@/pages/UsersPage')).UsersPage }));
const ApprovalsPage = lazy(async () => ({ default: (await import('@/pages/ApprovalsPage')).ApprovalsPage }));
const NotificationsPage = lazy(async () => ({ default: (await import('@/pages/NotificationsPage')).NotificationsPage }));
const NotFoundPage = lazy(async () => ({ default: (await import('@/pages/NotFoundPage')).NotFoundPage }));

/**
 * Renders the AppRouter component.
 *
 * @returns The rendered component tree.
 */
export const AppRouter = (): JSX.Element => (
  <BrowserRouter>
    <Suspense fallback={<RouteLoader />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AdminLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route element={<RoleRoute allowed={['ADMIN', 'B2B']} />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/analytics" element={<AnalyticsPage />} />
            </Route>
            <Route element={<RoleRoute allowed={['ADMIN']} />}>
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/stores" element={<StoresPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/approvals" element={<ApprovalsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  </BrowserRouter>
);
