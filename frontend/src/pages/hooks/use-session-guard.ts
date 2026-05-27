import { useCallback, useEffect, useState } from 'react'

import { asSiteManagerSession } from '@/pages/stores/auth/auth-session'
import { HttpClientError } from '@/system/api/http-client'
import { getSession } from '@/apis/auth/GetSession'
import { logout as logoutRequest } from '@/apis/auth/Logout'
import type { NavigationIntent } from '@/pages/routing/navigation-intent'
import {
  toSessionExpiredRedirect,
  toSignedOutRedirect,
  toSiteManageDeniedRedirect,
} from '@/pages/routing/route-policy'
import { useAuthStore } from '@/pages/stores/auth/use-auth-store'

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

        if (error instanceof HttpClientError && error.kind === 'unauthorized') {
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
