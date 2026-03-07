import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { PageLoader } from '@/components/common/PageLoader'
import { Layout } from '@/components/layout/Layout'
import { AdminRoute } from '@/routes/AdminRoute'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

const HomePage = lazy(() => import('@/pages/HomePage').then((module) => ({ default: module.HomePage })))
const ProductListPage = lazy(() =>
  import('@/pages/ProductListPage').then((module) => ({ default: module.ProductListPage })),
)
const ProductDetailPage = lazy(() =>
  import('@/pages/ProductDetailPage').then((module) => ({ default: module.ProductDetailPage })),
)
const StoreLocatorPage = lazy(() =>
  import('@/pages/StoreLocatorPage').then((module) => ({ default: module.StoreLocatorPage })),
)
const CartPage = lazy(() => import('@/pages/CartPage').then((module) => ({ default: module.CartPage })))
const CheckoutPage = lazy(() =>
  import('@/pages/CheckoutPage').then((module) => ({ default: module.CheckoutPage })),
)
const LoginPage = lazy(() => import('@/pages/LoginPage').then((module) => ({ default: module.LoginPage })))
const RegisterPage = lazy(() =>
  import('@/pages/RegisterPage').then((module) => ({ default: module.RegisterPage })),
)
const AccountPage = lazy(() => import('@/pages/AccountPage').then((module) => ({ default: module.AccountPage })))
const AdminPanelPage = lazy(() =>
  import('@/pages/AdminPanelPage').then((module) => ({ default: module.AdminPanelPage })),
)
const NotFoundPage = lazy(() =>
  import('@/pages/NotFoundPage').then((module) => ({ default: module.NotFoundPage })),
)

/**
 * Renders the AppRouter component.
 *
 * @returns The rendered component tree.
 */
export const AppRouter = (): JSX.Element => (
  <BrowserRouter>
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<Layout />} path="/">
          <Route element={<HomePage />} index />
          <Route element={<ProductListPage />} path="products" />
          <Route element={<ProductDetailPage />} path="products/:productId" />
          <Route element={<StoreLocatorPage />} path="stores" />
          <Route element={<CartPage />} path="cart" />
          <Route element={<CheckoutPage />} path="checkout" />
          <Route element={<LoginPage />} path="login" />
          <Route element={<RegisterPage />} path="register" />

          <Route element={<ProtectedRoute />}>
            <Route element={<AccountPage />} path="account" />
          </Route>

          <Route element={<AdminRoute />}>
            <Route element={<AdminPanelPage />} path="admin" />
          </Route>

          <Route element={<NotFoundPage />} path="*" />
        </Route>
      </Routes>
    </Suspense>
  </BrowserRouter>
)
