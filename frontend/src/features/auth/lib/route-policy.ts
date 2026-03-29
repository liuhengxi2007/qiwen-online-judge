import type { NavigationIntent } from '@/shared/routing/navigation-intent'

type UserSettingsRoutePolicyArgs = {
  viewerUsername: string
  routeUsername: string | undefined
  siteManagerViewer: boolean
}

export function toSessionExpiredRedirect(): NavigationIntent {
  return { to: '/login?notice=session-expired' }
}

export function toSignedOutRedirect(): NavigationIntent {
  return { to: '/login?notice=signed-out' }
}

export function toSiteManageDeniedRedirect(): NavigationIntent {
  return { to: '/?notice=site-manage-denied', replace: true }
}

export function toCorrectedUserSettingsRedirect(viewerUsername: string): NavigationIntent {
  return {
    to: `/user/${viewerUsername}/settings?notice=route-corrected`,
    replace: true,
  }
}

export function resolveUserSettingsRoutePolicy({
  viewerUsername,
  routeUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): {
  navigationIntent: NavigationIntent | null
  targetUsername: string
  isEditingOwnSettings: boolean
  canManageTarget: boolean
} {
  const normalizedRouteUsername = routeUsername?.trim() || viewerUsername
  const isEditingOwnSettings = normalizedRouteUsername.toLowerCase() === viewerUsername.toLowerCase()
  const canManageTarget = isEditingOwnSettings || siteManagerViewer

  if (!routeUsername && !siteManagerViewer) {
    return {
      navigationIntent: toCorrectedUserSettingsRedirect(viewerUsername),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (routeUsername && !siteManagerViewer && routeUsername.toLowerCase() !== viewerUsername.toLowerCase()) {
    return {
      navigationIntent: toCorrectedUserSettingsRedirect(viewerUsername),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (routeUsername && !canManageTarget) {
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
