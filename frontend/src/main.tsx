import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import 'antd/dist/reset.css'
import './index.css'

(globalThis as { __APP_ENV__?: Record<string, string | undefined> }).__APP_ENV__ = import.meta.env as Record<
  string,
  string | undefined
>

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
