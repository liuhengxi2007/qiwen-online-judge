import { parseEmailAddress } from '@/objects/auth/EmailAddress'
import { normalizeAuthPermissionFlags } from '@/objects/auth/AuthPermissionFlags'
import { parseProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { parseDisplayName } from '@/objects/user/DisplayName'
import { parseUserDisplayMode } from '@/objects/user/UserDisplayMode'
import { parseUserLocale } from '@/objects/user/UserLocale'
import { parseUsername } from '@/objects/user/Username'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'

const authSessionStorageKey = 'auth_session'
const legacyAuthSessionStorageKey = 'auth_user'

/**
 * 将规范化后的会话写入 localStorage，并清理旧版本存储键。
 */
export function persistAuthSession(session: SessionResponse): void {
  window.localStorage.setItem(authSessionStorageKey, JSON.stringify(normalizeAuthPermissionFlags(session)))
  window.localStorage.removeItem(legacyAuthSessionStorageKey)
}

/**
 * 清除当前和旧版本 localStorage 会话键，用于退出登录或会话失效。
 */
export function clearAuthSession(): void {
  window.localStorage.removeItem(authSessionStorageKey)
  window.localStorage.removeItem(legacyAuthSessionStorageKey)
}

/**
 * 从 localStorage 读取并校验会话；旧键读取成功后会迁移到当前键。
 */
export function readAuthSession(): SessionResponse | null {
  const storedSession = readStoredSession()

  if (!storedSession) {
    return null
  }

  const { rawSession, migratedFromLegacyKey } = storedSession

  const session = decodeStoredAuthSession(rawSession)

  if (!session) {
    clearAuthSession()
    return null
  }

  if (migratedFromLegacyKey) {
    persistAuthSession(session)
  }

  return session
}

/**
 * 读取当前或旧版本会话原文，并标记是否需要迁移。
 */
function readStoredSession(): { rawSession: string; migratedFromLegacyKey: boolean } | null {
  const rawSession = window.localStorage.getItem(authSessionStorageKey)
  if (!rawSession) {
    const legacyRawSession = window.localStorage.getItem(legacyAuthSessionStorageKey)

    if (!legacyRawSession) {
      return null
    }

    return { rawSession: legacyRawSession, migratedFromLegacyKey: true }
  }

  return { rawSession, migratedFromLegacyKey: false }
}

/**
 * 解码 localStorage 中的不可信会话 JSON；字段解析失败或结构不匹配时返回 null。
 */
function decodeStoredAuthSession(rawSession: string): SessionResponse | null {
  try {
    const parsed = JSON.parse(rawSession) as unknown

    if (!isStoredAuthSessionValue(parsed)) {
      return null
    }

    const displayNameResult = parseDisplayName(parsed.displayName)
    const usernameResult = parseUsername(parsed.username)
    const emailResult = parseEmailAddress(parsed.email)
    const displayModeResult = parseUserDisplayMode(parsed.preferences.displayMode)
    const localeResult = parseUserLocale(parsed.preferences.locale)
    const problemTitleDisplayModeResult = parseProblemTitleDisplayMode(parsed.preferences.problemTitleDisplayMode)

    if (
      !displayNameResult.ok ||
      !usernameResult.ok ||
      !emailResult.ok ||
      !displayModeResult.ok ||
      !localeResult.ok ||
      !problemTitleDisplayModeResult.ok
    ) {
      return null
    }

    return normalizeAuthPermissionFlags({
      displayName: displayNameResult.value,
      username: usernameResult.value,
      // FIXME-CN: avatarUrl 只校验为字符串后直接品牌断言，localStorage 被篡改时可能把非法 URL 带入 img src。
      avatarUrl: typeof parsed.avatarUrl === 'string' ? (parsed.avatarUrl as UserAvatarUrl) : null,
      email: emailResult.value,
      preferences: {
        displayMode: displayModeResult.value,
        locale: localeResult.value,
        problemTitleDisplayMode: problemTitleDisplayModeResult.value,
        autoMarkMessageRead: parsed.preferences.autoMarkMessageRead,
      },
      siteManager: parsed.siteManager,
      problemManager: parsed.problemManager,
      contestManager: parsed.contestManager ?? false,
    })
  } catch {
    return null
  }
}

/**
 * 校验存储会话的原始结构，只确认字段形态，具体领域合法性由 parse 函数继续验证。
 */
function isStoredAuthSessionValue(
  value: unknown,
): value is {
  displayName: string
  username: string
  email: string
  avatarUrl?: string | null
  preferences: {
    displayMode: string
    locale: string
    problemTitleDisplayMode: string
    autoMarkMessageRead: boolean
  }
  siteManager: boolean
  problemManager: boolean
  contestManager?: boolean
} {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const record = value as Record<string, unknown>

  return (
    typeof record.displayName === 'string' &&
    typeof record.username === 'string' &&
    typeof record.email === 'string' &&
    typeof record.preferences === 'object' &&
    record.preferences !== null &&
    typeof (record.preferences as { displayMode?: unknown }).displayMode === 'string' &&
    typeof (record.preferences as { locale?: unknown }).locale === 'string' &&
    typeof (record.preferences as { problemTitleDisplayMode?: unknown }).problemTitleDisplayMode === 'string' &&
    typeof (record.preferences as { autoMarkMessageRead?: unknown }).autoMarkMessageRead === 'boolean' &&
    typeof record.siteManager === 'boolean' &&
    typeof record.problemManager === 'boolean' &&
    (record.contestManager === undefined || typeof record.contestManager === 'boolean')
  )
}
