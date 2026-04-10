import {
  parseDisplayName,
  parseEmailAddress,
  parseUsername,
  type AuthSession,
} from '@/features/auth/domain/auth'

const authUserStorageKey = 'auth_user'

export function persistAuthSession(session: AuthSession): void {
  window.localStorage.setItem(authUserStorageKey, JSON.stringify(session))
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(authUserStorageKey)
}

export function readAuthSession(): AuthSession | null {
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

    if (!displayNameResult.ok || !usernameResult.ok || !emailResult.ok) {
      clearAuthSession()
      return null
    }

    return {
      displayName: displayNameResult.value,
      username: usernameResult.value,
      email: emailResult.value,
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
    typeof record.siteManager === 'boolean' &&
    typeof record.problemManager === 'boolean'
  )
}
