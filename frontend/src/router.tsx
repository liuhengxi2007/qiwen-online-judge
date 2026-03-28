import { createBrowserRouter, Navigate } from 'react-router-dom'

import { hasAuthSession } from '@/domain/auth'
import { DashboardPage } from '@/pages/DashboardPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'

function RootRedirect() {
  return hasAuthSession() ? <DashboardPage /> : <Navigate replace to="/login" />
}

function GuestOnlyRoute() {
  return hasAuthSession() ? <Navigate replace to="/" /> : <LoginPage />
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootRedirect />,
  },
  {
    path: '/login',
    element: <GuestOnlyRoute />,
  },
  {
    path: '/register',
    element: hasAuthSession() ? <Navigate replace to="/" /> : <RegisterPage />,
  },
  {
    path: '*',
    element: <Navigate replace to="/" />,
  },
])
