import { useCallback, useEffect, useState } from 'react'

import { asSiteManagerSession } from '@/features/auth/domain/auth'
import { AuthClientError, getSession, logout as logoutRequest } from '@/features/auth/http/api/auth-client'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import {
  toSessionExpiredRedirect,
  toSignedOutRedirect,
  toSiteManageDeniedRedirect,
} from '@/features/auth/lib/route-policy'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

type UseSessionGuardOptions = {
  requireSiteManager?: boolean
}

export function useSessionGuard(options: UseSessionGuardOptions = {}) {
  const session = useAuthStore((state) => state.session)
  const setSession = useAuthStore((state) => state.setSession)
  const clearSession = useAuthStore((state) => state.clearSession)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)
  const siteManagerSession = session ? asSiteManagerSession(session) : null

  useEffect(() => {
    let isCancelled = false

    const syncSession = async () => {
      try {
        const nextSession = await getSession()

        if (isCancelled) {
          return
        }

        setSession(nextSession)

        if (options.requireSiteManager && !asSiteManagerSession(nextSession)) {
          setNavigationIntent(toSiteManageDeniedRedirect())
        }
      } catch (error) {
        if (isCancelled) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'unauthorized') {
          clearSession()
          setNavigationIntent(toSessionExpiredRedirect())
          return
        }

        clearSession()
        setNavigationIntent(toSessionExpiredRedirect())
      }
    }

    void syncSession()

    return () => {
      isCancelled = true
    }
  }, [clearSession, options.requireSiteManager, setSession])

  const signOut = useCallback(async () => {
    await logoutRequest()
    clearSession()
    setNavigationIntent(toSignedOutRedirect())
  }, [clearSession])

  return {
    session,
    setSession,
    siteManagerSession,
    signOut,
    navigationIntent,
  }
}
