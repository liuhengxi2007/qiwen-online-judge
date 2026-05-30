import { useCallback, useEffect, useState } from 'react'

import { asSiteManagerSession } from '@/pages/stores/auth/AuthSession'
import { GetSession } from '@/apis/auth/GetSession'
import { HttpClientError } from '@/system/api/http-client'
import { Logout } from '@/apis/auth/Logout'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import {
  toSessionExpiredRedirect,
  toSignedOutRedirect,
  toSiteManageDeniedRedirect,
} from '@/pages/routing/RoutePolicy'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { sendAPI } from '@/system/api/api-message'

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
        const nextSession = await sendAPI(new GetSession())

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
    await sendAPI(new Logout()).catch(() => undefined)
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
