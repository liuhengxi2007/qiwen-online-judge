import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'

import { AuthI18nProvider } from '@/features/auth/components/auth-i18n-provider'
import { router } from '@/router'

import 'katex/dist/katex.min.css'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthI18nProvider>
      <RouterProvider router={router} />
    </AuthI18nProvider>
  </StrictMode>,
)
