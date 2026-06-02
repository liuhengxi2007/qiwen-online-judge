import type { LoginResponse } from '@/objects/auth/response/LoginResponse'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'

export function toAuthSession(
  response: Pick<
    LoginResponse,
    'displayName' | 'username' | 'avatarUrl' | 'email' | 'preferences' | 'siteManager' | 'problemManager' | 'contestManager'
  >,
): SessionResponse {
  return {
    displayName: response.displayName,
    username: response.username,
    avatarUrl: response.avatarUrl,
    email: response.email,
    preferences: response.preferences,
    siteManager: response.siteManager,
    problemManager: response.problemManager,
    contestManager: response.contestManager,
  }
}

export function asSiteManagerSession(session: SessionResponse): (SessionResponse & { siteManager: true }) | null {
  return isSiteManagerSession(session) ? session : null
}

export function asProblemManagerSession(
  session: SessionResponse,
): (SessionResponse & { problemManager: true }) | null {
  return isProblemManagerSession(session) ? session : null
}

export function isSiteManagerSession(session: SessionResponse): session is SessionResponse & { siteManager: true } {
  return session.siteManager
}

export function isProblemManagerSession(
  session: SessionResponse,
): session is SessionResponse & { problemManager: true } {
  return session.problemManager
}
