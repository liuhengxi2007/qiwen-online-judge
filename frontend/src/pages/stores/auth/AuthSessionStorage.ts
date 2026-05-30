import { parseEmailAddress } from '@/objects/auth/EmailAddress'
import { parseProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { parseDisplayName } from '@/objects/user/DisplayName'
import { parseUserDisplayMode } from '@/objects/user/UserDisplayMode'
import { parseUserLocale } from '@/objects/user/UserLocale'
import { parseUsername } from '@/objects/user/Username'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'

const authUserStorageKey = 'auth_user'

export function persistAuthSession(session: SessionResponse): void {
  window.localStorage.setItem(authUserStorageKey, JSON.stringify(session))
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(authUserStorageKey)
}

export function readAuthSession(): SessionResponse | null {
  const rawSession = window.localStorage.getItem(authUserStorageKey)
  if (!rawSession) {
    return null
  }

  try {
    const parsed = JSON.parse(rawSession) as unknown

    if (!isStoredAuthSessionValue(parsed)) {
      clearAuthSession()
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
      clearAuthSession()
      return null
    }

    return {
      displayName: displayNameResult.value,
      username: usernameResult.value,
      email: emailResult.value,
      preferences: {
        displayMode: displayModeResult.value,
        locale: localeResult.value,
        problemTitleDisplayMode: problemTitleDisplayModeResult.value,
        autoMarkMessageRead: parsed.preferences.autoMarkMessageRead,
      },
      siteManager: parsed.siteManager,
      problemManager: parsed.problemManager,
    }
  } catch {
    clearAuthSession()
    return null
  }
}

function isStoredAuthSessionValue(
  value: unknown,
): value is {
  displayName: string
  username: string
  email: string
  preferences: {
    displayMode: string
    locale: string
    problemTitleDisplayMode: string
    autoMarkMessageRead: boolean
  }
  siteManager: boolean
  problemManager: boolean
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
    typeof record.problemManager === 'boolean'
  )
}
