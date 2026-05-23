import type { Username } from '@/features/user/model/Username'
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
  return toForbiddenRedirect()
}

export function toForbiddenRedirect(): NavigationIntent {
  return { to: '/forbidden', replace: true }
}

function resolveManagedUserRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): ManagedUserRoutePolicy {
  const normalizedRouteUsername = routeUsername ?? viewerUsername
  const isEditingOwnSettings = normalizedRouteUsername === viewerUsername
  const canManageTarget = isEditingOwnSettings || siteManagerViewer

  if ((hasRouteUsername && !routeUsername) || (!hasRouteUsername && !siteManagerViewer)) {
    return {
      navigationIntent: toForbiddenRedirect(),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (routeUsername && !siteManagerViewer && routeUsername !== viewerUsername) {
    return {
      navigationIntent: toForbiddenRedirect(),
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
  })
}

export function resolveUserProfileRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): ManagedUserRoutePolicy {
  const normalizedRouteUsername = routeUsername ?? viewerUsername
  const isEditingOwnSettings = normalizedRouteUsername === viewerUsername
  const canManageTarget = isEditingOwnSettings || siteManagerViewer

  if (hasRouteUsername && !routeUsername) {
    return {
      navigationIntent: toForbiddenRedirect(),
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
