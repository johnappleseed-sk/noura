import { ConfigProvider, theme as antdTheme } from 'antd'
import { Provider } from 'react-redux'
import { AdminRouter } from '@/admin/app/router'
import { useAdminSelector } from '@/admin/app/hooks'
import { adminStore } from '@/admin/app/store'

/**
 * Renders the ThemedRouter component.
 *
 * @returns The rendered component tree.
 */
const ThemedRouter = (): JSX.Element => {
  const themeMode = useAdminSelector((state) => state.theme.mode)

  return (
    <ConfigProvider
      theme={{
        algorithm: themeMode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        token: {
          colorPrimary: '#1677ff',
          borderRadius: 10,
        },
      }}
    >
      <AdminRouter />
    </ConfigProvider>
  )
}

/**
 * Renders the AdminApp component.
 *
 * @returns The rendered component tree.
 */
export const AdminApp = (): JSX.Element => (
  <Provider store={adminStore}>
    <ThemedRouter />
  </Provider>
)
