import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'

import { router } from '@/router'
import { GlobalLanguageFooter } from '@/shared/components/global-language-footer'
import { I18nProvider } from '@/shared/i18n/i18n'

import 'katex/dist/katex.min.css'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider>
      <RouterProvider router={router} />
      <GlobalLanguageFooter />
    </I18nProvider>
  </StrictMode>,
)
