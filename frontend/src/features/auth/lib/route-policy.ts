import { usernameValue, type Username } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

type UserSettingsRoutePolicyArgs = {
  viewerUsername: Username
  routeUsername: Username | null
  hasRouteUsername: boolean
  siteManagerViewer: boolean
}

type ManagedUserRoutePolicy = {
  navigationIntent: NavigationIntent | null
  targetUsername: Username
  isEditingOwnSettings: boolean
  canManageTarget: boolean
}

export function toSessionExpiredRedirect(): NavigationIntent {
  return { to: '/login?notice=session-expired' }
}

export function toSignedOutRedirect(): NavigationIntent {
  return { to: '/login?notice=signed-out' }
}

export function toPasswordChangedRedirect(): NavigationIntent {
  return { to: '/login?notice=password-changed', replace: true }
}

export function toSiteManageDeniedRedirect(): NavigationIntent {
  return { to: '/?notice=site-manage-denied', replace: true }
}

export function toCorrectedUserSettingsRedirect(viewerUsername: Username): NavigationIntent {
  return {
    to: `/user/${usernameValue(viewerUsername)}/settings?notice=route-corrected`,
    replace: true,
  }
}

export function toCorrectedUserProfileRedirect(viewerUsername: Username): NavigationIntent {
  return {
    to: `/user/${usernameValue(viewerUsername)}?notice=route-corrected`,
    replace: true,
  }
}

function resolveManagedUserRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
  correctedRedirect,
}: UserSettingsRoutePolicyArgs & {
  correctedRedirect: (viewerUsername: Username) => NavigationIntent
}): ManagedUserRoutePolicy {
  const normalizedRouteUsername = routeUsername ?? viewerUsername
  const isEditingOwnSettings = normalizedRouteUsername === viewerUsername
  const canManageTarget = isEditingOwnSettings || siteManagerViewer

  if ((hasRouteUsername && !routeUsername) || (!hasRouteUsername && !siteManagerViewer)) {
    return {
      navigationIntent: correctedRedirect(viewerUsername),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (routeUsername && !siteManagerViewer && routeUsername !== viewerUsername) {
    return {
      navigationIntent: correctedRedirect(viewerUsername),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (hasRouteUsername && !canManageTarget) {
    return {
      navigationIntent: toSiteManageDeniedRedirect(),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  return {
    navigationIntent: null,
    targetUsername: normalizedRouteUsername,
    isEditingOwnSettings,
    canManageTarget,
  }
}

export function resolveUserSettingsRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): ManagedUserRoutePolicy {
  return resolveManagedUserRoutePolicy({
    viewerUsername,
    routeUsername,
    hasRouteUsername,
    siteManagerViewer,
    correctedRedirect: toCorrectedUserSettingsRedirect,
  })
}

export function resolveUserProfileRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): ManagedUserRoutePolicy {
  return resolveManagedUserRoutePolicy({
    viewerUsername,
    routeUsername,
    hasRouteUsername,
    siteManagerViewer,
    correctedRedirect: toCorrectedUserProfileRedirect,
  })
}
