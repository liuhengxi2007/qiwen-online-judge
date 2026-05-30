import { useState } from 'react'
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
import { useUserProfileQuery } from './hooks/useUserProfileQuery'
import { resolveUserProfileRoutePolicy } from '@/pages/routing/RoutePolicy'
import { AppSectionBar } from '@/pages/components/AppSectionBar'
import { AncestorNavigation } from '@/pages/components/AncestorNavigation'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import { AcceptedProblemsPanel } from './components/AcceptedProblemsPanel'
import { ContributionPanel } from './components/ContributionPanel'
import { OwnMessagePanel } from './components/OwnMessagePanel'
import { ProfileOverviewPanel } from './components/ProfileOverviewPanel'

const acceptedProblemsPerPage = 10

export function UserProfilePage() {
  const { t } = useI18n()
  usePageTitle(t('userProfile.pageTitle'))
  const { username: routeUsername } = useParams<{ username: string }>()
  const { session: viewer, navigationIntent: guardNavigationIntent } = useSessionGuard()

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (!viewer) {
    return <Navigate replace to="/login" />
  }

  return <UserProfilePageContent routeUsername={routeUsername} viewer={viewer} />
}

function UserProfilePageContent({ routeUsername, viewer }: { routeUsername?: string; viewer: SessionResponse }) {
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
  const acceptedProblems = displayedUser?.acceptedProblems ?? []
  const isOwnProfile = viewer.username === routePolicy.targetUsername
  const acceptedProblemsTotalPages = Math.max(1, Math.ceil(acceptedProblems.length / acceptedProblemsPerPage))
  const normalizedAcceptedProblemsPage = Math.min(acceptedProblemsPage, acceptedProblemsTotalPages)
  const acceptedProblemsPageItems = acceptedProblems.slice(
    (normalizedAcceptedProblemsPage - 1) * acceptedProblemsPerPage,
    normalizedAcceptedProblemsPage * acceptedProblemsPerPage,
  )
  const targetUsername = usernameValue(routePolicy.targetUsername)
  const profileUnavailable = !query.isLoadingProfile && !displayedUser
  const profileName = displayedUser ? displayNameValue(displayedUser.displayName) : profileUnavailable ? '--' : t('common.loading')
  const profileUsername = displayedUser ? usernameValue(displayedUser.username) : targetUsername

  if (routePolicy.navigationIntent) {
    return <Navigate replace={routePolicy.navigationIntent.replace} to={routePolicy.navigationIntent.to} />
  }

  if (query.navigationIntent) {
    return <Navigate replace={query.navigationIntent.replace} to={query.navigationIntent.to} />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {t('userProfile.heading')}
            </h1>
            <p className="text-sm text-slate-600">{t('userProfile.description', { username: profileUsername })}</p>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
                  canManageTarget={routePolicy.canManageTarget}
                  isLoadingProfile={query.isLoadingProfile}
                  isOwnProfile={isOwnProfile}
                  onOpenMessage={() => navigate(messageConversationPath(routePolicy.targetUsername))}
                  profileName={profileName}
                  targetUsername={targetUsername}
                />

                {isOwnProfile ? <OwnMessagePanel targetUsername={targetUsername} /> : null}
              </div>

              <div className="space-y-5">
                <ContributionPanel displayedContribution={displayedContribution} isLoadingProfile={query.isLoadingProfile} />

                <AcceptedProblemsPanel
                  acceptedProblems={acceptedProblems}
                  acceptedProblemsExpanded={acceptedProblemsExpanded}
                  acceptedProblemsPageItems={acceptedProblemsPageItems}
                  acceptedProblemsTotalPages={acceptedProblemsTotalPages}
                  hasProfile={Boolean(displayedUser)}
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
      </section>
    </main>
  )
}
