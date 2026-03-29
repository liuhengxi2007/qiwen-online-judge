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
    const parsed = JSON.parse(rawSession) as {
      displayName?: unknown
      username?: unknown
      email?: unknown
      siteManager?: unknown
      problemManager?: unknown
    }

    if (
      typeof parsed.displayName !== 'string' ||
      typeof parsed.username !== 'string' ||
      typeof parsed.email !== 'string' ||
      typeof parsed.siteManager !== 'boolean' ||
      typeof parsed.problemManager !== 'boolean'
    ) {
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
