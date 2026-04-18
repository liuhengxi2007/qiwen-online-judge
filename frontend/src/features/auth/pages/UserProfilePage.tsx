import { Link, Navigate, useParams } from 'react-router-dom'
import { Settings, UserRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { parseUsername, usernameValue } from '@/features/auth/domain/auth'
import { UserAccountPageShell } from '@/features/auth/components/user-account-page-shell'
import { UserPermissionsCard } from '@/features/auth/components/user-permissions-card'
import { UserProfileOverviewCard } from '@/features/auth/components/user-profile-overview-card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserSettingsQuery } from '@/features/auth/hooks/use-user-settings-query'
import { resolveUserProfileRoutePolicy } from '@/features/auth/lib/route-policy'
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

  if (routePolicy.navigationIntent) {
    return <Navigate replace={routePolicy.navigationIntent.replace} to={routePolicy.navigationIntent.to} />
  }

  if (query.navigationIntent) {
    return <Navigate replace={query.navigationIntent.replace} to={query.navigationIntent.to} />
  }

  return (
    <UserAccountPageShell
      heading={t('userProfile.heading')}
      subheading={
        displayedUser
          ? routePolicy.isEditingOwnSettings
            ? t('userProfile.viewingOwn', {
                displayName: displayedUser.displayName,
                username: displayedUser.username,
              })
            : t('userProfile.viewingOther', {
                displayName: displayedUser.displayName,
                username: displayedUser.username,
              })
          : t('userProfile.loadingFor', { username: routePolicy.targetUsername })
      }
    >
      <UserProfileOverviewCard
        description={t('userProfile.profileDescription')}
        fallbackUsername={routePolicy.targetUsername}
        icon={<UserRound className="size-5" />}
        loadingMessage={t('userProfile.loadingSelected')}
        title={t('userProfile.profileTitle')}
        user={displayedUser}
      >
        {routePolicy.canManageTarget ? (
          <Button asChild className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
            <Link to={`/user/${usernameValue(routePolicy.targetUsername)}/settings`}>
              <Settings className="size-4" />
              {t('userProfile.openSettings')}
            </Link>
          </Button>
        ) : null}
      </UserProfileOverviewCard>

      <UserPermissionsCard
        description={t('userProfile.permissionsDescription')}
        title={t('userProfile.permissionsTitle')}
        user={displayedUser}
      />
    </UserAccountPageShell>
  )
}
