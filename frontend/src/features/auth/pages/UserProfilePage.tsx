import { Link, Navigate, useParams } from 'react-router-dom'
import { Files, NotebookPen, Settings, UserRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { displayNameValue, parseUsername, usernameValue } from '@/features/auth/domain/auth'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserSettingsQuery } from '@/features/auth/hooks/use-user-settings-query'
import { resolveUserProfileRoutePolicy } from '@/features/auth/lib/route-policy'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

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

  const parsedRouteUsername = routeUsername ? parseUsername(routeUsername) : null
  const routePolicy = resolveUserProfileRoutePolicy({
    viewerUsername: viewer.username,
    routeUsername: parsedRouteUsername?.ok ? parsedRouteUsername.value : null,
    hasRouteUsername: Boolean(routeUsername),
    siteManagerViewer: viewer.siteManager,
  })

  const query = useUserSettingsQuery({
    canLoadTarget: true,
    targetUsername: routePolicy.targetUsername,
  })
  const displayedUser = routePolicy.isEditingOwnSettings ? viewer : query.editedUser
  const targetUsername = usernameValue(routePolicy.targetUsername)
  const profileName = displayedUser ? displayNameValue(displayedUser.displayName) : t('common.loading')

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
            <p className="text-sm text-slate-600">{t('userProfile.description', { username: profileName })}</p>
          </div>

          <AncestorNavigation />
        </div>

        <Card className="max-w-2xl border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
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
          <CardContent className="space-y-5">
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">{t('common.displayName')}</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{query.isLoadingSettings ? t('common.loading') : profileName}</p>
            </div>

            <div className="flex flex-wrap gap-3">
              <Button asChild className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
                <Link to={`/submissions?username=${encodeURIComponent(targetUsername)}`}>
                  <Files className="size-4" />
                  {t('userProfile.openSubmissions')}
                </Link>
              </Button>
              <Button asChild className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
                <Link to={`/blog/${targetUsername}`}>
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
            </div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
