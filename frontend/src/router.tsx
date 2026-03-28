import { createBrowserRouter, Navigate } from 'react-router-dom'

import { DashboardPage } from '@/pages/DashboardPage'
import { LoginPage } from '@/pages/LoginPage'

const isAuthenticated = () => window.localStorage.getItem('auth_user') !== null

function RootRedirect() {
  return <Navigate replace to={isAuthenticated() ? '/dashboard' : '/login'} />
}

function GuestOnlyRoute() {
  return isAuthenticated() ? <Navigate replace to="/dashboard" /> : <LoginPage />
}

function ProtectedRoute() {
  return isAuthenticated() ? <DashboardPage /> : <Navigate replace to="/login" />
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
    path: '/dashboard',
    element: <ProtectedRoute />,
  },
  {
    path: '*',
    element: <Navigate replace to="/" />,
  },
])
