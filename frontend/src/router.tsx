import type { ReactElement } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { CreateProblemPage } from '@/features/problem/pages/CreateProblemPage'
import { ProblemDataPage } from '@/features/problem/pages/ProblemDataPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { ProblemDetailPage } from '@/features/problem/pages/ProblemDetailPage'
import { ProblemPage } from '@/features/problem/pages/ProblemPage'
import { ProblemSubmitPage } from '@/features/problem/pages/ProblemSubmitPage'
import { CreateProblemSetPage } from '@/features/problemset/pages/CreateProblemSetPage'
import { ProblemSetDetailPage } from '@/features/problemset/pages/ProblemSetDetailPage'
import { ProblemSetPage } from '@/features/problemset/pages/ProblemSetPage'
import { ForbiddenPage } from '@/shared/pages/ForbiddenPage'
import { SiteManagePage } from '@/features/site-management/pages/SiteManagePage'
import { SubmissionDetailPage } from '@/features/submission/pages/SubmissionDetailPage'
import { ProblemSubmissionPage, SubmissionPage, UserSubmissionPage } from '@/features/submission/pages/SubmissionPage'
import { UserProfilePage } from '@/features/auth/pages/UserProfilePage'
import { UserSettingsPage } from '@/features/auth/pages/UserSettingsPage'
import { CreateUserGroupPage } from '@/features/usergroup/pages/CreateUserGroupPage'
import { UserGroupDetailPage } from '@/features/usergroup/pages/UserGroupDetailPage'
import { UserGroupPage } from '@/features/usergroup/pages/UserGroupPage'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

function RootRedirect() {
  const session = useAuthStore((state) => state.session)
  return session ? <DashboardPage /> : <Navigate replace to="/login" />
}

function GuestOnlyRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  return session ? <Navigate replace to="/" /> : element
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
    element: <GuestOnlyRoute element={<LoginPage />} />,
  },
  {
    path: '/register',
    element: <GuestOnlyRoute element={<RegisterPage />} />,
  },
  {
    path: '/forbidden',
    element: <ForbiddenPage />,
  },
  {
    path: '/site-manage',
    element: <AuthenticatedRoute element={<SiteManagePage />} />,
  },
  {
    path: '/problems',
    element: <AuthenticatedRoute element={<ProblemPage />} />,
  },
  {
    path: '/problems/new',
    element: <AuthenticatedRoute element={<CreateProblemPage />} />,
  },
  {
    path: '/problems/:slug',
    element: <AuthenticatedRoute element={<ProblemDetailPage />} />,
  },
  {
    path: '/problems/:slug/submit',
    element: <AuthenticatedRoute element={<ProblemSubmitPage />} />,
  },
  {
    path: '/problems/:slug/submissions',
    element: <AuthenticatedRoute element={<ProblemSubmissionPage />} />,
  },
  {
    path: '/problems/:slug/data',
    element: <AuthenticatedRoute element={<ProblemDataPage />} />,
  },
  {
    path: '/problem-sets',
    element: <AuthenticatedRoute element={<ProblemSetPage />} />,
  },
  {
    path: '/problem-sets/new',
    element: <AuthenticatedRoute element={<CreateProblemSetPage />} />,
  },
  {
    path: '/problem-sets/:slug',
    element: <AuthenticatedRoute element={<ProblemSetDetailPage />} />,
  },
  {
    path: '/submissions',
    element: <AuthenticatedRoute element={<SubmissionPage />} />,
  },
  {
    path: '/submission/:username',
    element: <AuthenticatedRoute element={<UserSubmissionPage />} />,
  },
  {
    path: '/submissions/:submissionId',
    element: <AuthenticatedRoute element={<SubmissionDetailPage />} />,
  },
  {
    path: '/user-groups',
    element: <AuthenticatedRoute element={<UserGroupPage />} />,
  },
  {
    path: '/user-groups/new',
    element: <AuthenticatedRoute element={<CreateUserGroupPage />} />,
  },
  {
    path: '/user-groups/:slug',
    element: <AuthenticatedRoute element={<UserGroupDetailPage />} />,
  },
  {
    path: '/user/:username',
    element: <AuthenticatedRoute element={<UserProfilePage />} />,
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
