import type { ReactElement } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { SiteManagePage } from '@/features/site-management/pages/SiteManagePage'
import { UserSettingsPage } from '@/features/auth/pages/UserSettingsPage'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

function RootRedirect() {
  const session = useAuthStore((state) => state.session)
  return session ? <DashboardPage /> : <Navigate replace to="/login" />
}

function GuestOnlyRoute() {
  const session = useAuthStore((state) => state.session)
  return session ? <Navigate replace to="/" /> : <LoginPage />
}

function AuthenticatedRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  return session ? element : <Navigate replace to="/login" />
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
    element: <GuestOnlyRoute />,
  },
  {
    path: '/site-manage',
    element: <AuthenticatedRoute element={<SiteManagePage />} />,
  },
  {
    path: '/user/:username/settings',
    element: <AuthenticatedRoute element={<UserSettingsPage />} />,
  },
  {
    path: '*',
    element: <Navigate replace to="/" />,
  },
])
