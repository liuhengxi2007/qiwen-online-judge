import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { PreviewInspectorBridge } from '@/components/PreviewInspectorBridge'

import { MockSystemProvider } from '@/components/MockSystemProvider'
import { router } from '@/router'

import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MockSystemProvider>
            <PreviewInspectorBridge />
      <RouterProvider router={router} />
    </MockSystemProvider>
  </StrictMode>,
)
