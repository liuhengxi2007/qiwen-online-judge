import { createBrowserRouter, Navigate } from 'react-router-dom'

import { DashboardPage } from '@/pages/DashboardPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'

const isAuthenticated = () => window.localStorage.getItem('auth_user') !== null

function RootRedirect() {
  return isAuthenticated() ? <DashboardPage /> : <Navigate replace to="/login" />
}

function GuestOnlyRoute() {
  return isAuthenticated() ? <Navigate replace to="/" /> : <LoginPage />
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
    element: isAuthenticated() ? <Navigate replace to="/" /> : <RegisterPage />,
  },
  {
    path: '*',
    element: <Navigate replace to="/" />,
  },
])
