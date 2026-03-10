import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { ThemeProvider } from './shared/ui/ThemeProvider'
import { router } from './app/router'
import './styles/theme.css'
import './styles/base.css'
import "./styles/global.css"

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>
)
