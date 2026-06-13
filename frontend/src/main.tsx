import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'

import { router } from '@/router'

import 'katex/dist/katex.min.css'
import './index.css'

// 注意：Vite 入口 HTML 固定提供 #root，非空断言只用于满足 React 挂载 API 的类型要求。
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
