import { ReactNode, useEffect } from 'react'
import { HelmetProvider } from 'react-helmet-async'
import { Provider } from 'react-redux'
import { useAppSelector } from '@/app/hooks'
import { store } from '@/app/store'
import { selectTheme } from '@/features/ui/uiSlice'

interface AppProvidersProps {
  children: ReactNode
}

/**
 * Renders the ThemeProvider component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
const ThemeProvider = ({ children }: AppProvidersProps): JSX.Element => {
  const theme = useAppSelector(selectTheme)

  useEffect(() => {
    const root = document.documentElement
    root.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return <>{children}</>
}

/**
 * Renders the AppProviders component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const AppProviders = ({ children }: AppProvidersProps): JSX.Element => (
  <Provider store={store}>
    <HelmetProvider>
      <ThemeProvider>{children}</ThemeProvider>
    </HelmetProvider>
  </Provider>
)
