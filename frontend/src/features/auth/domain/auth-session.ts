import type { LoginResponse } from '@/features/auth/model/LoginResponse'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'

export function toAuthSession(
  response: Pick<LoginResponse, 'displayName' | 'username' | 'email' | 'siteManager' | 'problemManager'>,
): SessionResponse {
  return {
    displayName: response.displayName,
    username: response.username,
    email: response.email,
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
