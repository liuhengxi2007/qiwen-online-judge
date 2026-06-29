import type { Username } from '@/objects/user/Username'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'

/**
 * 用户相关路由策略的输入，包含当前访问者、路由用户名和站点管理员权限。
 */
type UserSettingsRoutePolicyArgs = {
  viewerUsername: Username
  routeUsername: Username | null
  hasRouteUsername: boolean
  siteManagerViewer: boolean
}

/**
 * 管理型用户路由策略结果，描述目标用户、管理权限和可选重定向。
 */
type ManagedUserRoutePolicy = {
  navigationIntent: NavigationIntent | null
  targetUsername: Username
  isEditingOwnSettings: boolean
  canManageTarget: boolean
}

/**
 * 构造会话过期后的登录页跳转意图。
 */
export function toSessionExpiredRedirect(): NavigationIntent {
  return { to: '/login?notice=session-expired' }
}

/**
 * 构造主动退出后的登录页跳转意图。
 */
export function toSignedOutRedirect(): NavigationIntent {
  return { to: '/login?notice=signed-out' }
}

/**
 * 构造密码变更后的登录页跳转意图，并替换当前历史记录。
 */
export function toPasswordChangedRedirect(): NavigationIntent {
  return { to: '/login?notice=password-changed', replace: true }
}

/**
 * 构造站点管理权限不足的跳转意图，当前统一落到禁止访问页。
 */
export function toSiteManageDeniedRedirect(): NavigationIntent {
  // 注意：站点管理权限不足复用 forbidden 落点，避免在管理入口区分具体受限资源。
  return toForbiddenRedirect()
}

/**
 * 构造禁止访问页跳转意图，并替换当前历史记录避免返回受限页。
 */
export function toForbiddenRedirect(): NavigationIntent {
  return { to: '/forbidden', replace: true }
}

/**
 * 解析需要管理目标用户的路由权限；访客伪造用户名或越权访问时返回 forbidden 跳转。
 */
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

/**
 * 解析用户设置页权限策略；普通用户只能访问自己的设置，站点管理员可管理他人。
 */
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

/**
 * 解析用户资料页权限策略；缺失用户名回退到当前用户，非法显式用户名跳转 forbidden，其余资料页允许展示。
 */
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
