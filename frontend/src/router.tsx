import type { ReactElement } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { BlogPage, ProblemBlogPage, UserBlogPage } from '@/features/blog/pages/BlogPage'
import { BlogDetailPage } from '@/features/blog/pages/BlogDetailPage'
import { CreateBlogPage } from '@/features/blog/pages/CreateBlogPage'
import { CreateProblemPage } from '@/features/problem/pages/CreateProblemPage'
import { ProblemDataPage } from '@/features/problem/pages/ProblemDataPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { ProblemDetailPage } from '@/features/problem/pages/ProblemDetailPage'
import { ProblemPage } from '@/features/problem/pages/ProblemPage'
import { ProblemSubmitPage } from '@/features/problem/pages/ProblemSubmitPage'
import { CreateProblemSetPage } from '@/features/problemset/pages/CreateProblemSetPage'
import { MessageConversationPage } from '@/features/message/pages/MessageConversationPage'
import { MessageInboxPage } from '@/features/message/pages/MessageInboxPage'
import { useMessageRealtimeConnection } from '@/features/message/hooks/use-message-realtime-connection'
import { useNotificationRealtimeConnection } from '@/features/notification/hooks/use-notification-realtime-connection'
import { NotificationPage } from '@/features/notification/pages/NotificationPage'
import { ProblemSetDetailPage } from '@/features/problemset/pages/ProblemSetDetailPage'
import { ProblemSetPage } from '@/features/problemset/pages/ProblemSetPage'
import { ForbiddenPage } from '@/shared/pages/ForbiddenPage'
import { SiteManagePage } from '@/features/site-management/pages/SiteManagePage'
import { SubmissionDetailPage } from '@/features/submission/pages/SubmissionDetailPage'
import { ProblemSubmissionPage, SubmissionPage } from '@/features/submission/pages/SubmissionPage'
import { UserProfilePage } from '@/features/auth/pages/UserProfilePage'
import { RanklistPage } from '@/features/auth/pages/RanklistPage'
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
  useMessageRealtimeConnection()
  useNotificationRealtimeConnection()
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
    path: '/blogs',
    element: <AuthenticatedRoute element={<BlogPage />} />,
  },
  {
    path: '/blogs/new',
    element: <AuthenticatedRoute element={<CreateBlogPage />} />,
  },
  {
    path: '/blogs/:blogId',
    element: <AuthenticatedRoute element={<BlogDetailPage />} />,
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
    path: '/problems/:slug/blogs',
    element: <AuthenticatedRoute element={<ProblemBlogPage />} />,
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
    path: '/notifications',
    element: <AuthenticatedRoute element={<NotificationPage />} />,
  },
  {
    path: '/messages',
    element: <AuthenticatedRoute element={<MessageInboxPage />} />,
  },
  {
    path: '/messages/with/:username',
    element: <AuthenticatedRoute element={<MessageConversationPage />} />,
  },
  {
    path: '/submissions',
    element: <AuthenticatedRoute element={<SubmissionPage />} />,
  },
  {
    path: '/submissions/:submissionId',
    element: <AuthenticatedRoute element={<SubmissionDetailPage />} />,
  },
  {
    path: '/ranklist',
    element: <AuthenticatedRoute element={<RanklistPage />} />,
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
    path: '/user/:username/blogs',
    element: <AuthenticatedRoute element={<UserBlogPage />} />,
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
