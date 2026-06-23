import type { ReactElement } from 'react'
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'

import { BlogPage } from '@/pages/BlogPage'
import { BlogDetailPage } from '@/pages/BlogDetailPage'
import { CreateBlogPage } from '@/pages/CreateBlogPage'
import { CreateContestPage } from '@/pages/CreateContestPage'
import { CreateProblemPage } from '@/pages/CreateProblemPage'
import { ContestPage } from '@/pages/ContestPage'
import { ContestDetailPage } from '@/pages/ContestDetailPage'
import { ContestManagePage } from '@/pages/ContestManagePage'
import { ContestRanklistPage } from '@/pages/ContestRanklistPage'
import { ContestRegistrantPage } from '@/pages/ContestRegistrantPage'
import { ContestSubmissionPage } from '@/pages/ContestSubmissionPage'
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
import { HackPage } from '@/pages/HackPage'
import { HackDetailPage } from '@/pages/HackDetailPage'
import { SiteManagePage } from '@/pages/SiteManagePage'
import { SubmissionDetailPage } from '@/pages/SubmissionDetailPage'
import { SubmissionHackPage } from '@/pages/SubmissionHackPage'
import { ProblemSubmissionPage } from '@/pages/ProblemSubmissionPage'
import { SubmissionPage } from '@/pages/SubmissionPage'
import { UserProfilePage } from '@/pages/UserProfilePage'
import { UserBlogPage } from '@/pages/UserBlogPage'
import { RanklistPage } from '@/pages/RanklistPage'
import { RatingManagePage } from '@/pages/RatingManagePage'
import { UserSettingsPage } from '@/pages/UserSettingsPage'
import { CreateUserGroupPage } from '@/pages/CreateUserGroupPage'
import { UserGroupDetailPage } from '@/pages/UserGroupDetailPage'
import { UserGroupPage } from '@/pages/UserGroupPage'
import { useRealtimeConnection } from '@/pages/hooks/useRealtimeConnection'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { I18nProvider } from '@/system/i18n/i18n'

/**
 * 应用根布局，负责把登录会话中的语言偏好传入国际化 Provider 并渲染子路由。
 */
function RootLayout() {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)

  return (
    <I18nProvider sessionLocale={sessionLocale}>
      <Outlet />
    </I18nProvider>
  )
}

/**
 * 首页重定向入口；已登录用户进入仪表盘，访客跳转登录页。
 */
function RootRedirect() {
  const session = useAuthStore((state) => state.session)
  return session ? <DashboardPage /> : <Navigate replace to="/login" />
}

/**
 * 访客专用路由守卫；已有会话时回到首页，避免登录/注册页覆盖当前会话。
 */
function GuestOnlyRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  return session ? <Navigate replace to="/" /> : element
}

/**
 * 登录态路由守卫；为已登录页面建立消息和通知实时连接，未登录时跳转登录页。
 */
function AuthenticatedRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  useRealtimeConnection()
  return session ? element : <Navigate replace to="/login" />
}

/**
 * 浏览器路由表，集中声明页面路径、登录态边界和兜底跳转。
 */
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
        path: 'contests/:slug/ranklist',
        element: <AuthenticatedRoute element={<ContestRanklistPage />} />,
      },
      {
        path: 'contests/:slug/submissions',
        element: <AuthenticatedRoute element={<ContestSubmissionPage />} />,
      },
      {
        path: 'contests/:slug/manage',
        element: <AuthenticatedRoute element={<ContestManagePage />} />,
      },
      {
        path: 'contests/:contestSlug/problems/:slug/submit',
        element: <AuthenticatedRoute element={<ProblemSubmitPage />} />,
      },
      {
        path: 'contests/:contestSlug/problems/:slug/data',
        element: <AuthenticatedRoute element={<ProblemDataPage />} />,
      },
      {
        path: 'contests/:contestSlug/problems/:slug',
        element: <AuthenticatedRoute element={<ProblemDetailPage />} />,
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
        path: 'submissions/:submissionId/hack/:subtaskIndex',
        element: <AuthenticatedRoute element={<SubmissionHackPage />} />,
      },
      {
        path: 'hacks',
        element: <AuthenticatedRoute element={<HackPage />} />,
      },
      {
        path: 'hacks/:hackId',
        element: <AuthenticatedRoute element={<HackDetailPage />} />,
      },
      {
        path: 'ranklist',
        element: <AuthenticatedRoute element={<RanklistPage />} />,
      },
      {
        path: 'ratings/manage',
        element: <AuthenticatedRoute element={<RatingManagePage />} />,
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
