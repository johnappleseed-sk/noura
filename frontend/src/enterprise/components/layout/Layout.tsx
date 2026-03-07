import { lazy, Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import { Footer } from '@/components/layout/Footer'
import { Navbar } from '@/components/layout/Navbar'

const ChatbotWidget = lazy(() =>
  import('@/components/ai/ChatbotWidget').then((module) => ({ default: module.ChatbotWidget })),
)

/**
 * Renders the Layout component.
 *
 * @returns The rendered component tree.
 */
export const Layout = (): JSX.Element => (
  <div className="app-shell flex min-h-screen flex-col">
    <Navbar />
    <main className="w-full flex-1 px-4 py-8 sm:px-6 lg:px-8">
      <Outlet />
    </main>
    <Footer />
    <Suspense fallback={null}>
      <ChatbotWidget />
    </Suspense>
  </div>
)
