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

export function persistAuthSession(session: SessionResponse): void {
  window.localStorage.setItem(authSessionStorageKey, JSON.stringify(normalizeAuthPermissionFlags(session)))
  window.localStorage.removeItem(legacyAuthSessionStorageKey)
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(authSessionStorageKey)
  window.localStorage.removeItem(legacyAuthSessionStorageKey)
}

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
