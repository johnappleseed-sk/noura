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
import { ServiceAreasPage } from '../pages/ServiceAreasPage'
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
import { ADMIN_ROLES, CAPABILITIES, INVENTORY_PORTAL_ROLES } from '../shared/auth/roles'

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
    element: <ProtectedRoute allowedRoles={INVENTORY_PORTAL_ROLES} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <ProtectedRoute requiredCapability={CAPABILITIES.OVERVIEW_DASHBOARD}><DashboardPage /></ProtectedRoute> },
          { path: 'analytics', element: <ProtectedRoute requiredCapability={CAPABILITIES.OVERVIEW_ANALYTICS}><AdminAnalyticsDashboard /></ProtectedRoute> },
          // Backwards-compatible alias
          { path: 'analytics/dashboard', element: <Navigate to="/admin/analytics" replace /> },
          { path: 'commerce/catalog', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_CATALOG}><CommerceCatalogPage /></ProtectedRoute> },
          { path: 'commerce/carousels', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_CAROUSELS}><CarouselsPage /></ProtectedRoute> },
          { path: 'commerce/recommendations', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_RECOMMENDATIONS}><RecommendationsPage /></ProtectedRoute> },
          { path: 'commerce/merchandising', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_MERCHANDISING}><MerchandisingPage /></ProtectedRoute> },
          { path: 'orders', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_ORDERS}><OrdersPage /></ProtectedRoute> },
          { path: 'returns', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_RETURNS}><ReturnsPage /></ProtectedRoute> },
          { path: 'stores', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_STORES}><StoresPage /></ProtectedRoute> },
          { path: 'pricing', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_PRICING}><PricingPage /></ProtectedRoute> },
          { path: 'users', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_USERS}><UsersPage /></ProtectedRoute> },
          { path: 'notifications', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_NOTIFICATIONS}><NotificationsPage /></ProtectedRoute> },
          { path: 'tools/control-center', element: <ProtectedRoute requiredCapability={CAPABILITIES.TOOLS_CONTROL_CENTER}><ControlCenterPage /></ProtectedRoute> },
          { path: 'tools/product-generator', element: <ProtectedRoute requiredCapability={CAPABILITIES.TOOLS_PRODUCT_GENERATOR}><ProductGeneratorPage /></ProtectedRoute> },
          { path: 'warehouse/catalog', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_CATALOG}><CatalogPage /></ProtectedRoute> },
          { path: 'warehouse/locations', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_LOCATIONS}><LocationsPage /></ProtectedRoute> },
          { path: 'warehouse/stock', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_STOCK}><InventoryPage /></ProtectedRoute> },
          { path: 'warehouse/movements', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_MOVEMENTS}><MovementsPage /></ProtectedRoute> },
          { path: 'warehouse/batches', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_BATCHES}><BatchesPage /></ProtectedRoute> },
          { path: 'warehouse/serials', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_SERIALS}><SerialsPage /></ProtectedRoute> },
          { path: 'warehouse/reports', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_REPORTS}><ReportsPage /></ProtectedRoute> },
          { path: 'warehouse/webhooks', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.WAREHOUSE_WEBHOOKS}><WebhooksPage /></ProtectedRoute> },
          { path: 'warehouse/audit-logs', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.WAREHOUSE_AUDIT_LOGS}><AuditLogsPage /></ProtectedRoute> },
        ]
      }
    ]
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
])
