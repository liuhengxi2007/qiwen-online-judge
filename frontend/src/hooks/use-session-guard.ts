import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { asSiteManagerSession } from '@/domain/auth'
import { AuthClientError, getSession, logout as logoutRequest } from '@/lib/auth-client'
import { useAuthStore } from '@/stores/use-auth-store'

type UseSessionGuardOptions = {
  requireSiteManager?: boolean
}

export function useSessionGuard(options: UseSessionGuardOptions = {}) {
  const navigate = useNavigate()
  const session = useAuthStore((state) => state.session)
  const setSession = useAuthStore((state) => state.setSession)
  const clearSession = useAuthStore((state) => state.clearSession)
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
          navigate('/?notice=site-manage-denied', { replace: true })
        }
      } catch (error) {
        if (isCancelled) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'unauthorized') {
          clearSession()
          navigate('/login?notice=session-expired')
          return
        }

        clearSession()
        navigate('/login?notice=session-expired')
      }
    }

    void syncSession()

    return () => {
      isCancelled = true
    }
  }, [navigate, options.requireSiteManager])

  const signOut = async () => {
    await logoutRequest()
    clearSession()
    navigate('/login?notice=signed-out')
  }

  return {
    session,
    setSession,
    siteManagerSession,
    signOut,
  }
}
