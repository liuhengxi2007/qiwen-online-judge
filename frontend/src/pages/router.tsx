import { createBrowserRouter, Navigate } from 'react-router-dom'

import { BlogPage, ProblemBlogPage, UserBlogPage } from '@/pages/BlogPage'
import { BlogDetailPage } from '@/pages/BlogDetailPage'
import { CreateBlogPage } from '@/pages/CreateBlogPage'
import { CreateProblemPage } from '@/pages/CreateProblemPage'
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
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { SiteManagePage } from '@/pages/SiteManagePage'
import { SubmissionDetailPage } from '@/pages/SubmissionDetailPage'
import { ProblemSubmissionPage, SubmissionPage } from '@/pages/SubmissionPage'
import { UserProfilePage } from '@/pages/UserProfilePage'
import { RanklistPage } from '@/pages/RanklistPage'
import { UserSettingsPage } from '@/pages/UserSettingsPage'
import { CreateUserGroupPage } from '@/pages/CreateUserGroupPage'
import { UserGroupDetailPage } from '@/pages/UserGroupDetailPage'
import { UserGroupPage } from '@/pages/UserGroupPage'
import { AuthenticatedRoute, GuestOnlyRoute, RootRedirect } from '@/pages/route-components'

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
