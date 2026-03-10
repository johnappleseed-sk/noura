import { Navigate, createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { AdminLayout } from './layouts/AdminLayout'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { DashboardPage } from '../pages/DashboardPage'
import AdminAnalyticsDashboard from '../pages/AdminAnalyticsDashboard'
import { ControlCenterPage } from '../pages/ControlCenterPage'
import { OrdersPage } from '../pages/OrdersPage'
import { ReturnsPage } from '../pages/ReturnsPage'
import { CommerceCatalogPage } from '../pages/CommerceCatalogPage'
import { StoresPage } from '../pages/StoresPage'
import { PricingPage } from '../pages/PricingPage'
import { UsersPage } from '../pages/UsersPage'
import { CarouselsPage } from '../pages/CarouselsPage'
import { RecommendationsPage } from '../pages/RecommendationsPage'
import { MerchandisingPage } from '../pages/MerchandisingPage'
import { NotificationsPage } from '../pages/NotificationsPage'
import { CatalogPage } from '../pages/CatalogPage'
import { LocationsPage } from '../pages/LocationsPage'
import { InventoryPage } from '../pages/InventoryPage'
import { MovementsPage } from '../pages/MovementsPage'
import { BatchesPage } from '../pages/BatchesPage'
import { SerialsPage } from '../pages/SerialsPage'
import { ReportsPage } from '../pages/ReportsPage'
import { WebhooksPage } from '../pages/WebhooksPage'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { ProductGeneratorPage } from '../pages/ProductGeneratorPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ADMIN_ROLES } from '../shared/auth/roles'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/admin" replace />
  },
  {
    path: '/login',
    element: <LoginPage />
  },
  {
    path: '/unauthorized',
    element: <UnauthorizedPage />
  },
  {
    path: '/admin',
    element: <ProtectedRoute allowedRoles={ADMIN_ROLES} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'analytics', element: <AdminAnalyticsDashboard /> },
          // Backwards-compatible alias
          { path: 'analytics/dashboard', element: <Navigate to="/admin/analytics" replace /> },
          { path: 'commerce/catalog', element: <CommerceCatalogPage /> },
          { path: 'commerce/carousels', element: <CarouselsPage /> },
          { path: 'commerce/recommendations', element: <RecommendationsPage /> },
          { path: 'commerce/merchandising', element: <MerchandisingPage /> },
          { path: 'orders', element: <OrdersPage /> },
          { path: 'returns', element: <ReturnsPage /> },
          { path: 'stores', element: <StoresPage /> },
          { path: 'pricing', element: <PricingPage /> },
          { path: 'users', element: <UsersPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          { path: 'tools/control-center', element: <ControlCenterPage /> },
          { path: 'tools/product-generator', element: <ProductGeneratorPage /> },
          { path: 'warehouse/catalog', element: <CatalogPage /> },
          { path: 'warehouse/locations', element: <LocationsPage /> },
          { path: 'warehouse/stock', element: <InventoryPage /> },
          { path: 'warehouse/movements', element: <MovementsPage /> },
          { path: 'warehouse/batches', element: <BatchesPage /> },
          { path: 'warehouse/serials', element: <SerialsPage /> },
          { path: 'warehouse/reports', element: <ReportsPage /> },
          {
            element: <ProtectedRoute allowedRoles={ADMIN_ROLES} />,
            children: [
              { path: 'warehouse/webhooks', element: <WebhooksPage /> },
              { path: 'warehouse/audit-logs', element: <AuditLogsPage /> }
            ]
          },
        ]
      }
    ]
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
])
