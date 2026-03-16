import { Suspense, lazy } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { ADMIN_ROLES, CAPABILITIES, INVENTORY_PORTAL_ROLES } from '../shared/auth/roles'
import { Spinner } from '../shared/ui/Spinner'
import { AdminLayout } from './layouts/AdminLayout'
import { ProtectedRoute } from './routes/ProtectedRoute'

const lazyNamed = (importer, exportName) =>
  lazy(() => importer().then((module) => ({ default: module[exportName] })))

const lazyPage = (LazyComponent, label) => (
  <Suspense fallback={<Spinner label={label} />}>
    <LazyComponent />
  </Suspense>
)

const AnalyticsPage = lazy(() => import('../features/analytics/pages/AnalyticsPage'))
const DashboardPage = lazyNamed(() => import('../pages/DashboardPage'), 'DashboardPage')
const ControlCenterPage = lazyNamed(() => import('../pages/ControlCenterPage'), 'ControlCenterPage')
const MerchantsPage = lazyNamed(() => import('../pages/MerchantsPage'), 'MerchantsPage')
const CreateMerchantPage = lazyNamed(() => import('../pages/CreateMerchantPage'), 'CreateMerchantPage')
const ProductSubmissionReviewPage = lazyNamed(() => import('../pages/ProductSubmissionReviewPage'), 'ProductSubmissionReviewPage')
const OrdersPage = lazyNamed(() => import('../pages/OrdersPage'), 'OrdersPage')
const ReturnsPage = lazyNamed(() => import('../pages/ReturnsPage'), 'ReturnsPage')
const CommerceCatalogPage = lazyNamed(() => import('../pages/CommerceCatalogPage'), 'CommerceCatalogPage')
const StoresPage = lazyNamed(() => import('../pages/StoresPage'), 'StoresPage')
const ServiceAreasPage = lazyNamed(() => import('../pages/ServiceAreasPage'), 'ServiceAreasPage')
const PricingPage = lazyNamed(() => import('../pages/PricingPage'), 'PricingPage')
const UsersPage = lazyNamed(() => import('../pages/UsersPage'), 'UsersPage')
const CarouselsPage = lazyNamed(() => import('../pages/CarouselsPage'), 'CarouselsPage')
const RecommendationsPage = lazyNamed(() => import('../pages/RecommendationsPage'), 'RecommendationsPage')
const MerchandisingPage = lazyNamed(() => import('../pages/MerchandisingPage'), 'MerchandisingPage')
const NotificationsPage = lazyNamed(() => import('../pages/NotificationsPage'), 'NotificationsPage')
const CatalogPage = lazyNamed(() => import('../pages/CatalogPage'), 'CatalogPage')
const LocationsPage = lazyNamed(() => import('../pages/LocationsPage'), 'LocationsPage')
const InventoryPage = lazyNamed(() => import('../pages/InventoryPage'), 'InventoryPage')
const MovementsPage = lazyNamed(() => import('../pages/MovementsPage'), 'MovementsPage')
const BatchesPage = lazyNamed(() => import('../pages/BatchesPage'), 'BatchesPage')
const SerialsPage = lazyNamed(() => import('../pages/SerialsPage'), 'SerialsPage')
const ReportsPage = lazyNamed(() => import('../pages/ReportsPage'), 'ReportsPage')
const WebhooksPage = lazyNamed(() => import('../pages/WebhooksPage'), 'WebhooksPage')
const AuditLogsPage = lazyNamed(() => import('../pages/AuditLogsPage'), 'AuditLogsPage')
const ProductGeneratorPage = lazyNamed(() => import('../pages/ProductGeneratorPage'), 'ProductGeneratorPage')
const RecoveryCenterPage = lazyNamed(() => import('../pages/RecoveryCenterPage'), 'RecoveryCenterPage')
const RolesPermissionsPage = lazyNamed(() => import('../pages/RolesPermissionsPage'), 'RolesPermissionsPage')
const UnauthorizedPage = lazyNamed(() => import('../pages/UnauthorizedPage'), 'UnauthorizedPage')
const NotFoundPage = lazyNamed(() => import('../pages/NotFoundPage'), 'NotFoundPage')

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
    element: lazyPage(UnauthorizedPage, 'Loading...')
  },
  {
    path: '/admin',
    element: <ProtectedRoute allowedRoles={INVENTORY_PORTAL_ROLES} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <ProtectedRoute requiredCapability={CAPABILITIES.OVERVIEW_DASHBOARD}>{lazyPage(DashboardPage, 'Loading dashboard...')}</ProtectedRoute> },
          { path: 'analytics', element: <ProtectedRoute requiredCapability={CAPABILITIES.OVERVIEW_ANALYTICS}>{lazyPage(AnalyticsPage, 'Loading analytics...')}</ProtectedRoute> },
          // Backwards-compatible alias
          { path: 'analytics/dashboard', element: <Navigate to="/admin/analytics" replace /> },
          { path: 'merchants', element: <ProtectedRoute requiredCapability={CAPABILITIES.NETWORK_MERCHANTS}>{lazyPage(MerchantsPage, 'Loading merchants...')}</ProtectedRoute> },
          { path: 'merchants/create', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.NETWORK_MERCHANTS}>{lazyPage(CreateMerchantPage, 'Loading create merchant form...')}</ProtectedRoute> },
          { path: 'product-submissions', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.GOVERNANCE_PRODUCT_SUBMISSIONS}>{lazyPage(ProductSubmissionReviewPage, 'Loading product submissions...')}</ProtectedRoute> },
          { path: 'commerce/catalog', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_CATALOG}>{lazyPage(CommerceCatalogPage, 'Loading catalog...')}</ProtectedRoute> },
          { path: 'commerce/carousels', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_CAROUSELS}>{lazyPage(CarouselsPage, 'Loading carousels...')}</ProtectedRoute> },
          { path: 'commerce/recommendations', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_RECOMMENDATIONS}>{lazyPage(RecommendationsPage, 'Loading recommendations...')}</ProtectedRoute> },
          { path: 'commerce/merchandising', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_MERCHANDISING}>{lazyPage(MerchandisingPage, 'Loading merchandising...')}</ProtectedRoute> },
          { path: 'orders', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_ORDERS}>{lazyPage(OrdersPage, 'Loading orders...')}</ProtectedRoute> },
          { path: 'returns', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_RETURNS}>{lazyPage(ReturnsPage, 'Loading returns...')}</ProtectedRoute> },
          { path: 'stores', element: <ProtectedRoute requiredCapability={CAPABILITIES.NETWORK_STORES}>{lazyPage(StoresPage, 'Loading stores...')}</ProtectedRoute> },
          { path: 'service-areas', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_STORES}>{lazyPage(ServiceAreasPage, 'Loading service areas...')}</ProtectedRoute> },
          { path: 'pricing', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_PRICING}>{lazyPage(PricingPage, 'Loading pricing...')}</ProtectedRoute> },
          { path: 'users', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_USERS}>{lazyPage(UsersPage, 'Loading users...')}</ProtectedRoute> },
          { path: 'notifications', element: <ProtectedRoute requiredCapability={CAPABILITIES.COMMERCE_NOTIFICATIONS}>{lazyPage(NotificationsPage, 'Loading notifications...')}</ProtectedRoute> },
          { path: 'tools/control-center', element: <ProtectedRoute requiredCapability={CAPABILITIES.TOOLS_CONTROL_CENTER}>{lazyPage(ControlCenterPage, 'Loading control center...')}</ProtectedRoute> },
          { path: 'tools/product-generator', element: <ProtectedRoute requiredCapability={CAPABILITIES.TOOLS_PRODUCT_GENERATOR}>{lazyPage(ProductGeneratorPage, 'Loading generator...')}</ProtectedRoute> },
          { path: 'warehouse/catalog', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_CATALOG}>{lazyPage(CatalogPage, 'Loading warehouse catalog...')}</ProtectedRoute> },
          { path: 'warehouse/locations', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_LOCATIONS}>{lazyPage(LocationsPage, 'Loading locations...')}</ProtectedRoute> },
          { path: 'warehouse/stock', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_STOCK}>{lazyPage(InventoryPage, 'Loading stock...')}</ProtectedRoute> },
          { path: 'warehouse/movements', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_MOVEMENTS}>{lazyPage(MovementsPage, 'Loading movements...')}</ProtectedRoute> },
          { path: 'warehouse/batches', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_BATCHES}>{lazyPage(BatchesPage, 'Loading batches...')}</ProtectedRoute> },
          { path: 'warehouse/serials', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_SERIALS}>{lazyPage(SerialsPage, 'Loading serials...')}</ProtectedRoute> },
          { path: 'warehouse/reports', element: <ProtectedRoute requiredCapability={CAPABILITIES.WAREHOUSE_REPORTS}>{lazyPage(ReportsPage, 'Loading reports...')}</ProtectedRoute> },
          { path: 'warehouse/webhooks', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.WAREHOUSE_WEBHOOKS}>{lazyPage(WebhooksPage, 'Loading webhooks...')}</ProtectedRoute> },
          { path: 'warehouse/audit-logs', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.WAREHOUSE_AUDIT_LOGS}>{lazyPage(AuditLogsPage, 'Loading audit logs...')}</ProtectedRoute> },
          { path: 'governance/recovery', element: <ProtectedRoute allowedRoles={ADMIN_ROLES} requiredCapability={CAPABILITIES.GOVERNANCE_RECOVERY}>{lazyPage(RecoveryCenterPage, 'Loading recovery center...')}</ProtectedRoute> },
          { path: 'governance/roles-permissions', element: <ProtectedRoute requiredCapability={CAPABILITIES.GOVERNANCE_RBAC}>{lazyPage(RolesPermissionsPage, 'Loading roles & permissions...')}</ProtectedRoute> },
        ]
      }
    ]
  },
  {
    path: '*',
    element: lazyPage(NotFoundPage, 'Loading...')
  }
])
