import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { AdminLayout } from './layouts/AdminLayout'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { DashboardPage } from '../pages/DashboardPage'
import { UsersPage } from '../pages/UsersPage'
import { ProductsPage } from '../pages/ProductsPage'
import { InventoryPage } from '../pages/InventoryPage'
import { ReportsPage } from '../pages/ReportsPage'
import { SuppliersPage } from '../pages/SuppliersPage'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { OrdersPage } from '../pages/OrdersPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { ReturnsPage } from '../pages/ReturnsPage'
import { ReturnDetailPage } from '../pages/ReturnDetailPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import StoresPage from '../pages/StoresPage'
import StoreDetailPage from '../pages/StoreDetailPage'
import CompaniesPage from '../pages/CompaniesPage'
import CompanyDetailPage from '../pages/CompanyDetailPage'
import MarketplaceChannelsPage from '../pages/MarketplaceChannelsPage'
import { ADMIN_ROLES, MANAGEMENT_ROLES } from '../shared/auth/roles'

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
    element: <ProtectedRoute />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          {
            index: true,
            element: <DashboardPage />
          },
          {
            element: <ProtectedRoute allowedRoles={ADMIN_ROLES} />,
            children: [
              {
                path: 'users',
                element: <UsersPage />
              },
              {
                path: 'audit',
                element: <AuditLogsPage />
              }
            ]
          },
          {
            element: <ProtectedRoute allowedRoles={MANAGEMENT_ROLES} />,
            children: [
              {
                path: 'products',
                element: <ProductsPage />
              },
              {
                path: 'inventory',
                element: <InventoryPage />
              },
              {
                path: 'suppliers',
                element: <SuppliersPage />
              },
              {
                path: 'reports',
                element: <ReportsPage />
              },
              {
                path: 'orders',
                element: <OrdersPage />
              },
              {
                path: 'orders/:id',
                element: <OrderDetailPage />
              },
              {
                path: 'returns',
                element: <ReturnsPage />
              },
              {
                path: 'returns/:id',
                element: <ReturnDetailPage />
              },
              {
                path: 'stores',
                element: <StoresPage />
              },
              {
                path: 'stores/:id',
                element: <StoreDetailPage />
              },
              {
                path: 'b2b/companies',
                element: <CompaniesPage />
              },
              {
                path: 'b2b/companies/:id',
                element: <CompanyDetailPage />
              },
              {
                path: 'marketplace/channels',
                element: <MarketplaceChannelsPage />
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
])
