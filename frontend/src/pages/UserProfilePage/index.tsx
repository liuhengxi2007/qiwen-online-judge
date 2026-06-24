import { useEffect, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { displayNameValue } from '@/objects/user/DisplayName'
import { userContributionValue } from '@/objects/user/UserContribution'
import { parseUsername, usernameValue } from '@/objects/user/Username'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { messageConversationPath } from '@/pages/routing/MessagePaths'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useAcceptedProblemsQuery } from './hooks/useAcceptedProblemsQuery'
import { useUserProfileQuery } from './hooks/useUserProfileQuery'
import { resolveUserProfileRoutePolicy } from '@/pages/routing/RoutePolicy'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import { AcceptedProblemsPanel } from './components/AcceptedProblemsPanel'
import { ContributionPanel } from './components/ContributionPanel'
import { ProfileAvatarUploadPanel } from './components/ProfileAvatarUploadPanel'
import { ProfileActionsPanel } from './components/ProfileActionsPanel'
import { ProfileOverviewPanel } from './components/ProfileOverviewPanel'
import { RatingPanel } from './components/RatingPanel'

/**
 * 个人资料页已接受题目列表的客户端分页大小。
 */
const acceptedProblemsPerPage = 10

/**
 * 用户资料入口页，负责登录保护和把当前会话传给资料内容组件。
 */
export function UserProfilePage() {
  const { t } = useI18n()
  usePageTitle(t('userProfile.pageTitle'))
  const { username: routeUsername } = useParams<{ username: string }>()
  const { session: viewer, setSession, navigationIntent: guardNavigationIntent } = useSessionGuard()

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (!viewer) {
    return <Navigate replace to="/login" />
  }

  return <UserProfilePageContent routeUsername={routeUsername} setViewer={setSession} viewer={viewer} />
}

/**
 * 用户资料内容区，解析目标用户名、执行路由权限策略、加载资料并组织统计/操作面板。
 * 自己的资料页允许头像上传并同步会话；受限访问会按路由策略跳转到权限页。
 */
function UserProfilePageContent({
  routeUsername,
  setViewer,
  viewer,
}: {
  routeUsername?: string
  setViewer: (session: SessionResponse | null) => void
  viewer: SessionResponse
}) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const [acceptedProblemsExpanded, setAcceptedProblemsExpanded] = useState(false)
  const [acceptedProblemsPage, setAcceptedProblemsPage] = useState(1)
  const parsedRouteUsername = routeUsername ? parseUsername(routeUsername) : null
  const routePolicy = resolveUserProfileRoutePolicy({
    viewerUsername: viewer.username,
    routeUsername: parsedRouteUsername?.ok ? parsedRouteUsername.value : null,
    hasRouteUsername: Boolean(routeUsername),
    siteManagerViewer: viewer.siteManager,
  })

  const query = useUserProfileQuery({
    targetUsername: routePolicy.targetUsername,
  })
  const displayedUser = query.profile
  const displayedContribution = displayedUser ? Math.round(userContributionValue(displayedUser.contribution)) : null
  const acceptedProblemCount = displayedUser?.acceptedProblemCount ?? 0
  const isOwnProfile = viewer.username === routePolicy.targetUsername
  const acceptedProblemsTotalPages = Math.max(1, Math.ceil(acceptedProblemCount / acceptedProblemsPerPage))
  const normalizedAcceptedProblemsPage = Math.min(acceptedProblemsPage, acceptedProblemsTotalPages)
  const acceptedProblemsQuery = useAcceptedProblemsQuery({
    enabled: acceptedProblemsExpanded && Boolean(displayedUser) && acceptedProblemCount > 0,
    page: normalizedAcceptedProblemsPage,
    targetUsername: routePolicy.targetUsername,
  })
  const acceptedProblemsPageItems = acceptedProblemsQuery.response?.items ?? []
  const targetUsername = usernameValue(routePolicy.targetUsername)
  const profileUnavailable = !query.isLoadingProfile && !displayedUser
  const profileName = displayedUser ? displayNameValue(displayedUser.displayName) : profileUnavailable ? '--' : t('common.loading')
  const profileUsername = displayedUser ? usernameValue(displayedUser.username) : targetUsername

  useEffect(() => {
    setAcceptedProblemsExpanded(false)
    setAcceptedProblemsPage(1)
  }, [routePolicy.targetUsername])

  if (routePolicy.navigationIntent) {
    return <Navigate replace={routePolicy.navigationIntent.replace} to={routePolicy.navigationIntent.to} />
  }

  if (query.navigationIntent) {
    return <Navigate replace={query.navigationIntent.replace} to={query.navigationIntent.to} />
  }

  return (
    <PageShell
      title={t('userProfile.heading')}
      description={t('userProfile.description', { username: profileUsername })}
      mainClassName="bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)]"
    >
      {query.profileLoadError ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{query.profileLoadError}</AlertDescription>
        </Alert>
      ) : null}
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
              <UserRound className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('userProfile.profileTitle')}</CardTitle>
              <CardDescription>{t('userProfile.profileDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid gap-5 lg:grid-cols-[minmax(18rem,0.85fr)_minmax(0,1.45fr)]">
            <div className="space-y-5">
              <ProfileOverviewPanel
                avatarUrl={displayedUser?.avatarUrl ?? null}
                isLoadingProfile={query.isLoadingProfile}
                profileDisplayName={displayedUser?.displayName ?? null}
                profileName={profileName}
              />

              <ProfileActionsPanel
                canManageTarget={routePolicy.canManageTarget}
                isOwnProfile={isOwnProfile}
                onOpenMessage={() => navigate(messageConversationPath(routePolicy.targetUsername))}
                targetUsername={targetUsername}
              />

              {isOwnProfile && displayedUser ? (
                <ProfileAvatarUploadPanel
                  onProfileUpdated={query.replaceProfile}
                  onSessionUpdated={setViewer}
                  profile={displayedUser}
                  targetUsername={routePolicy.targetUsername}
                />
              ) : null}
            </div>

            <div className="space-y-5">
              <ContributionPanel displayedContribution={displayedContribution} isLoadingProfile={query.isLoadingProfile} />

              <RatingPanel displayedRating={displayedUser?.rating ?? null} isLoadingProfile={query.isLoadingProfile} />

              <AcceptedProblemsPanel
                acceptedProblemCount={acceptedProblemCount}
                acceptedProblemsExpanded={acceptedProblemsExpanded}
                acceptedProblemsLoadError={acceptedProblemsQuery.errorMessage}
                acceptedProblemsPageItems={acceptedProblemsPageItems}
                acceptedProblemsTotalPages={acceptedProblemsTotalPages}
                hasProfile={Boolean(displayedUser)}
                isLoadingAcceptedProblems={acceptedProblemsQuery.isLoading}
                isLoadingProfile={query.isLoadingProfile}
                normalizedAcceptedProblemsPage={normalizedAcceptedProblemsPage}
                onNextPage={() => setAcceptedProblemsPage((page) => Math.min(acceptedProblemsTotalPages, page + 1))}
                onPreviousPage={() => setAcceptedProblemsPage((page) => Math.max(1, page - 1))}
                onToggleExpanded={() => {
                  setAcceptedProblemsExpanded((isExpanded) => !isExpanded)
                  setAcceptedProblemsPage(1)
                }}
              />
            </div>
          </div>
        </CardContent>
      </Card>
    </PageShell>
  )
}
