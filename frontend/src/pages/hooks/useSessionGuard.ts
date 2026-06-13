import { useCallback, useEffect, useState } from 'react'

import { asSiteManagerSession } from '@/pages/stores/auth/AuthSession'
import { GetSession } from '@/apis/auth/GetSession'
import { isHttpClientError } from '@/system/api/http-client'
import { Logout } from '@/apis/auth/Logout'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import {
  toSessionExpiredRedirect,
  toSignedOutRedirect,
  toSiteManageDeniedRedirect,
} from '@/pages/routing/RoutePolicy'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { sendAPI } from '@/system/api/api-message'

/**
 * 会话守卫配置，允许页面声明必须具备站点管理员权限。
 */
type UseSessionGuardOptions = {
  requireSiteManager?: boolean
}

/**
 * 校验并刷新当前登录会话；会发起 GetSession 请求，必要时清理会话并返回跳转意图。
 */
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

        if (isHttpClientError(error) && error.kind === 'unauthorized') {
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
