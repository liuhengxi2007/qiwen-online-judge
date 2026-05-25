import type { LoginResponse } from '@/features/auth/model/response/LoginResponse'
import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'

export function toAuthSession(
  response: Pick<LoginResponse, 'displayName' | 'username' | 'email' | 'preferences' | 'siteManager' | 'problemManager'>,
): SessionResponse {
  return {
    displayName: response.displayName,
    username: response.username,
    email: response.email,
    preferences: response.preferences,
    siteManager: response.siteManager,
    problemManager: response.problemManager,
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
