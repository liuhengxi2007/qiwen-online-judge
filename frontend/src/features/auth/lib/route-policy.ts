import {
  sameUsername,
  usernameValue,
  type Username,
} from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

type UserSettingsRoutePolicyArgs = {
  viewerUsername: Username
  routeUsername: Username | null
  hasRouteUsername: boolean
  siteManagerViewer: boolean
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

export function resolveUserSettingsRoutePolicy({
  viewerUsername,
  routeUsername,
  hasRouteUsername,
  siteManagerViewer,
}: UserSettingsRoutePolicyArgs): {
  navigationIntent: NavigationIntent | null
  targetUsername: Username
  isEditingOwnSettings: boolean
  canManageTarget: boolean
} {
  const normalizedRouteUsername = routeUsername ?? viewerUsername
  const isEditingOwnSettings = sameUsername(normalizedRouteUsername, viewerUsername)
  const canManageTarget = isEditingOwnSettings || siteManagerViewer

  if ((hasRouteUsername && !routeUsername) || (!hasRouteUsername && !siteManagerViewer)) {
    return {
      navigationIntent: toCorrectedUserSettingsRedirect(viewerUsername),
      targetUsername: normalizedRouteUsername,
      isEditingOwnSettings,
      canManageTarget,
    }
  }

  if (routeUsername && !siteManagerViewer && !sameUsername(routeUsername, viewerUsername)) {
    return {
      navigationIntent: toCorrectedUserSettingsRedirect(viewerUsername),
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
