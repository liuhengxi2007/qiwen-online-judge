import { createBrowserRouter, Navigate } from 'react-router-dom'

import { hasAuthSession } from '@/domain/auth'
import { DashboardPage } from '@/pages/DashboardPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { SiteManagePage } from '@/pages/SiteManagePage'
import { UserSettingsPage } from '@/pages/UserSettingsPage'

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
    path: '/site-manage',
    element: hasAuthSession() ? <SiteManagePage /> : <Navigate replace to="/login" />,
  },
  {
    path: '/user/:username/settings',
    element: hasAuthSession() ? <UserSettingsPage /> : <Navigate replace to="/login" />,
  },
  {
    path: '*',
    element: <Navigate replace to="/" />,
  },
])
