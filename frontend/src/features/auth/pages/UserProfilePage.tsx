import { useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { Files, Mail, NotebookPen, Settings, ShieldBan, Sparkles, UserRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { contributionTextClassName } from '@/features/auth/domain/contribution-style'
import { displayNameValue, parseUsername, userContributionValue, usernameValue } from '@/features/auth/domain/auth'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'
import { messageConversationPath } from '@/features/message/domain/message'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { problemSlugValue } from '@/features/problem/domain/problem'
import { formatProblemTitleDisplay } from '@/features/problem/domain/problem-display'
import { useProblemTitleDisplayMode } from '@/features/problem/hooks/use-problem-title-display'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserProfileQuery } from '@/features/auth/hooks/use-user-profile-query'
import { resolveUserProfileRoutePolicy } from '@/features/auth/lib/route-policy'
import { AppSectionBar } from '@/features/auth/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'
import { formatDateTime, formatUtcOffsetTitle } from '@/shared/lib/date-time'

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
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
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
                <div className="rounded-3xl bg-slate-50 p-6">
                  <p className="text-sm text-slate-500">{t('common.displayName')}</p>
                  <p className="mt-2 text-2xl font-semibold text-slate-900">{query.isLoadingProfile ? t('common.loading') : profileName}</p>
                </div>

                <div className="flex flex-wrap gap-3 rounded-3xl border border-slate-100 bg-slate-50 p-6">
                  <Button asChild className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
                    <Link to={`/submissions?username=${encodeURIComponent(targetUsername)}`}>
                      <Files className="size-4" />
                      {t('userProfile.openSubmissions')}
                    </Link>
                  </Button>
                  <Button asChild className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
                    <Link to={`/user/${targetUsername}/blogs`}>
                      <NotebookPen className="size-4" />
                      {t('userProfile.openBlogs')}
                    </Link>
                  </Button>
                  {routePolicy.canManageTarget ? (
                    <Button asChild variant="outline" className="rounded-2xl border-violet-300 bg-white text-violet-950">
                      <Link to={`/user/${targetUsername}/settings`}>
                        <Settings className="size-4" />
                        {t('userProfile.openSettings')}
                      </Link>
                    </Button>
                  ) : null}
                  {!isOwnProfile ? (
                    <Button
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-cyan-300 bg-white text-cyan-950"
                      onClick={() => navigate(messageConversationPath(routePolicy.targetUsername))}
                    >
                      {t('nav.messages')}
                    </Button>
                  ) : null}
                </div>

                {isOwnProfile ? (
                  <div id="profile-messages" className="rounded-3xl border border-cyan-100 bg-cyan-50 p-6 scroll-mt-28">
                    <div className="flex items-start gap-3">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
                        <Mail className="size-4" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-base font-semibold text-cyan-950">{t('userProfile.messagesTitle')}</p>
                        <p className="mt-1 text-sm text-cyan-800">{t('userProfile.messagesDescription')}</p>
                        <div className="mt-4 flex flex-wrap gap-3">
                          <Button asChild className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400">
                            <Link to="/messages">
                              <Mail className="size-4" />
                              {t('userProfile.openMessages')}
                            </Link>
                          </Button>
                          <Button asChild variant="outline" className="rounded-2xl border-cyan-300 bg-white text-cyan-950">
                            <Link to={`/user/${targetUsername}/settings#message-blocks`}>
                              <ShieldBan className="size-4" />
                              {t('messages.manageBlocks')}
                            </Link>
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : null}
              </div>

              <div className="space-y-5">
                <div className="rounded-3xl bg-violet-50 p-6">
                  <div className="flex items-center gap-2 text-violet-800">
                    <Sparkles className="size-4" />
                    <p className="text-sm font-medium">{t('userProfile.contribution')}</p>
                  </div>
                  <p className={`mt-2 text-3xl font-semibold ${displayedContribution === null ? 'text-violet-950' : contributionTextClassName(displayedContribution)}`}>
                    {displayedContribution === null ? (query.isLoadingProfile ? t('common.loading') : '--') : String(displayedContribution)}
                  </p>
                  <p className="mt-1 text-sm text-violet-700">{t('userProfile.contributionDescription')}</p>
                </div>

                <div className="rounded-3xl bg-emerald-50 p-6">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <p className="text-sm font-medium text-emerald-800">{t('userProfile.acceptedProblems')}</p>
                      <p className="mt-2 text-3xl font-semibold text-emerald-700">
                        {displayedUser ? String(acceptedProblems.length) : query.isLoadingProfile ? t('common.loading') : '--'}
                      </p>
                    </div>
                    <Button
                      disabled={!displayedUser || acceptedProblems.length === 0}
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
                      onClick={() => {
                        setAcceptedProblemsExpanded((isExpanded) => !isExpanded)
                        setAcceptedProblemsPage(1)
                      }}
                    >
                      {acceptedProblemsExpanded ? t('userProfile.hideAcceptedProblems') : t('userProfile.showAcceptedProblems')}
                    </Button>
                  </div>

                  {acceptedProblemsExpanded ? (
                    acceptedProblems.length > 0 ? (
                      <div className="mt-4 space-y-2">
                        {acceptedProblemsPageItems.map((problem) => (
                          <div
                            key={problemSlugValue(problem.slug)}
                            className="rounded-2xl border border-emerald-100 bg-white px-4 py-3"
                          >
                            <Link
                              className="font-medium text-slate-900 hover:underline"
                              to={`/problems/${problemSlugValue(problem.slug)}`}
                            >
                              {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
                            </Link>
                            <p className="mt-1 text-sm text-emerald-700">
                              <span title={formatUtcOffsetTitle(problem.acceptedAt)}>
                                {t('userProfile.acceptedAt', { acceptedAt: formatDateTime(problem.acceptedAt) })}
                              </span>
                            </p>
                          </div>
                        ))}
                        <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                          <p className="text-sm text-emerald-700">
                            {t('userProfile.acceptedProblemsPageStatus', {
                              page: String(normalizedAcceptedProblemsPage),
                              totalPages: String(acceptedProblemsTotalPages),
                            })}
                          </p>
                          <div className="flex gap-2">
                            <Button
                              disabled={normalizedAcceptedProblemsPage <= 1}
                              type="button"
                              variant="outline"
                              className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
                              onClick={() => setAcceptedProblemsPage((page) => Math.max(1, page - 1))}
                            >
                              {t('submission.pagination.previous')}
                            </Button>
                            <Button
                              disabled={normalizedAcceptedProblemsPage >= acceptedProblemsTotalPages}
                              type="button"
                              variant="outline"
                              className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
                              onClick={() => setAcceptedProblemsPage((page) => Math.min(acceptedProblemsTotalPages, page + 1))}
                            >
                              {t('submission.pagination.next')}
                            </Button>
                          </div>
                        </div>
                      </div>
                    ) : (
                      <p className="mt-4 text-sm text-emerald-700">{t('userProfile.acceptedProblemsEmpty')}</p>
                    )
                  ) : null}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
