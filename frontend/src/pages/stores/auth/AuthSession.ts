import type { LoginResponse } from '@/objects/auth/response/LoginResponse'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { normalizeAuthPermissionFlags } from '@/objects/auth/AuthPermissionFlags'

/**
 * 将登录响应裁剪并规范化为前端会话对象，确保权限布尔字段都有稳定默认值。
 */
export function toAuthSession(
  response: Pick<
    LoginResponse,
    'displayName' | 'username' | 'avatarUrl' | 'email' | 'preferences' | 'siteManager' | 'problemManager' | 'contestManager'
  >,
): SessionResponse {
  return normalizeAuthPermissionFlags({
    displayName: response.displayName,
    username: response.username,
    avatarUrl: response.avatarUrl,
    email: response.email,
    preferences: response.preferences,
    siteManager: response.siteManager,
    problemManager: response.problemManager,
    contestManager: response.contestManager,
  })
}

/**
 * 将会话收窄为站点管理员会话；非管理员返回 null，便于权限分支使用。
 */
export function asSiteManagerSession(session: SessionResponse): (SessionResponse & { siteManager: true }) | null {
  return isSiteManagerSession(session) ? session : null
}

/**
 * 将会话收窄为题目管理员会话；非管理员返回 null，便于权限分支使用。
 */
export function asProblemManagerSession(
  session: SessionResponse,
): (SessionResponse & { problemManager: true }) | null {
  return isProblemManagerSession(session) ? session : null
}

/**
 * 判断会话是否具备站点管理员权限，并在类型层收窄权限字段。
 */
export function isSiteManagerSession(session: SessionResponse): session is SessionResponse & { siteManager: true } {
  return session.siteManager
}

/**
 * 判断会话是否具备题目管理员权限，并在类型层收窄权限字段。
 */
export function isProblemManagerSession(
  session: SessionResponse,
): session is SessionResponse & { problemManager: true } {
  return session.problemManager
}
