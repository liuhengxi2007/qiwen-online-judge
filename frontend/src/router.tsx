import type { ReactElement } from 'react'
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'

import { BlogPage } from '@/pages/BlogPage'
import { BlogDetailPage } from '@/pages/BlogDetailPage'
import { CreateBlogPage } from '@/pages/CreateBlogPage'
import { CreateContestPage } from '@/pages/CreateContestPage'
import { CreateProblemPage } from '@/pages/CreateProblemPage'
import { ContestPage } from '@/pages/ContestPage'
import { ContestDetailPage } from '@/pages/ContestDetailPage'
import { ContestRegistrantPage } from '@/pages/ContestRegistrantPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { ProblemDataPage } from '@/pages/ProblemDataPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ProblemDetailPage } from '@/pages/ProblemDetailPage'
import { ProblemPage } from '@/pages/ProblemPage'
import { ProblemSubmitPage } from '@/pages/ProblemSubmitPage'
import { CreateProblemSetPage } from '@/pages/CreateProblemSetPage'
import { MessageConversationPage } from '@/pages/MessageConversationPage'
import { MessageInboxPage } from '@/pages/MessageInboxPage'
import { NotificationPage } from '@/pages/NotificationPage'
import { ProblemSetDetailPage } from '@/pages/ProblemSetDetailPage'
import { ProblemSetPage } from '@/pages/ProblemSetPage'
import { ProblemBlogPage } from '@/pages/ProblemBlogPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { SiteManagePage } from '@/pages/SiteManagePage'
import { SubmissionDetailPage } from '@/pages/SubmissionDetailPage'
import { ProblemSubmissionPage } from '@/pages/ProblemSubmissionPage'
import { SubmissionPage } from '@/pages/SubmissionPage'
import { UserProfilePage } from '@/pages/UserProfilePage'
import { UserBlogPage } from '@/pages/UserBlogPage'
import { RanklistPage } from '@/pages/RanklistPage'
import { UserSettingsPage } from '@/pages/UserSettingsPage'
import { CreateUserGroupPage } from '@/pages/CreateUserGroupPage'
import { UserGroupDetailPage } from '@/pages/UserGroupDetailPage'
import { UserGroupPage } from '@/pages/UserGroupPage'
import { useMessageRealtimeConnection } from '@/pages/hooks/useMessageRealtimeConnection'
import { useNotificationRealtimeConnection } from '@/pages/hooks/useNotificationRealtimeConnection'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { I18nProvider } from '@/system/i18n/i18n'

function RootLayout() {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)

  return (
    <I18nProvider sessionLocale={sessionLocale}>
      <Outlet />
    </I18nProvider>
  )
}

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
  useMessageRealtimeConnection()
  useNotificationRealtimeConnection()
  return session ? element : <Navigate replace to="/login" />
}

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <RootRedirect />,
      },
      {
        path: 'login',
        element: <GuestOnlyRoute element={<LoginPage />} />,
      },
      {
        path: 'register',
        element: <GuestOnlyRoute element={<RegisterPage />} />,
      },
      {
        path: 'forbidden',
        element: <ForbiddenPage />,
      },
      {
        path: 'site-manage',
        element: <AuthenticatedRoute element={<SiteManagePage />} />,
      },
      {
        path: 'blogs',
        element: <AuthenticatedRoute element={<BlogPage />} />,
      },
      {
        path: 'blogs/new',
        element: <AuthenticatedRoute element={<CreateBlogPage />} />,
      },
      {
        path: 'blogs/:blogId',
        element: <AuthenticatedRoute element={<BlogDetailPage />} />,
      },
      {
        path: 'problems',
        element: <AuthenticatedRoute element={<ProblemPage />} />,
      },
      {
        path: 'problems/new',
        element: <AuthenticatedRoute element={<CreateProblemPage />} />,
      },
      {
        path: 'problems/:slug',
        element: <AuthenticatedRoute element={<ProblemDetailPage />} />,
      },
      {
        path: 'problems/:slug/submit',
        element: <AuthenticatedRoute element={<ProblemSubmitPage />} />,
      },
      {
        path: 'problems/:slug/submissions',
        element: <AuthenticatedRoute element={<ProblemSubmissionPage />} />,
      },
      {
        path: 'problems/:slug/blogs',
        element: <AuthenticatedRoute element={<ProblemBlogPage />} />,
      },
      {
        path: 'problems/:slug/data',
        element: <AuthenticatedRoute element={<ProblemDataPage />} />,
      },
      {
        path: 'problem-sets',
        element: <AuthenticatedRoute element={<ProblemSetPage />} />,
      },
      {
        path: 'problem-sets/new',
        element: <AuthenticatedRoute element={<CreateProblemSetPage />} />,
      },
      {
        path: 'problem-sets/:slug',
        element: <AuthenticatedRoute element={<ProblemSetDetailPage />} />,
      },
      {
        path: 'contests',
        element: <AuthenticatedRoute element={<ContestPage />} />,
      },
      {
        path: 'contests/new',
        element: <AuthenticatedRoute element={<CreateContestPage />} />,
      },
      {
        path: 'contests/:slug/registrants',
        element: <AuthenticatedRoute element={<ContestRegistrantPage />} />,
      },
      {
        path: 'contests/:slug',
        element: <AuthenticatedRoute element={<ContestDetailPage />} />,
      },
      {
        path: 'notifications',
        element: <AuthenticatedRoute element={<NotificationPage />} />,
      },
      {
        path: 'messages',
        element: <AuthenticatedRoute element={<MessageInboxPage />} />,
      },
      {
        path: 'messages/with/:username',
        element: <AuthenticatedRoute element={<MessageConversationPage />} />,
      },
      {
        path: 'submissions',
        element: <AuthenticatedRoute element={<SubmissionPage />} />,
      },
      {
        path: 'submissions/:submissionId',
        element: <AuthenticatedRoute element={<SubmissionDetailPage />} />,
      },
      {
        path: 'ranklist',
        element: <AuthenticatedRoute element={<RanklistPage />} />,
      },
      {
        path: 'user-groups',
        element: <AuthenticatedRoute element={<UserGroupPage />} />,
      },
      {
        path: 'user-groups/new',
        element: <AuthenticatedRoute element={<CreateUserGroupPage />} />,
      },
      {
        path: 'user-groups/:slug',
        element: <AuthenticatedRoute element={<UserGroupDetailPage />} />,
      },
      {
        path: 'user/:username',
        element: <AuthenticatedRoute element={<UserProfilePage />} />,
      },
      {
        path: 'user/:username/blogs',
        element: <AuthenticatedRoute element={<UserBlogPage />} />,
      },
      {
        path: 'user/:username/settings',
        element: <AuthenticatedRoute element={<UserSettingsPage />} />,
      },
      {
        path: '*',
        element: <Navigate replace to="/" />,
      },
    ],
  },
])
