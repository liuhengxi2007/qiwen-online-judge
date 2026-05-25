import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { logout as logoutRequest } from '@/features/auth/http/api/Logout'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

export function useLogoutAction() {
  const navigate = useNavigate()
  const clearSession = useAuthStore((state) => state.clearSession)

  return useCallback(async () => {
    await logoutRequest().catch(() => undefined)
    clearSession()
    navigate('/login?notice=signed-out', { replace: true })
  }, [clearSession, navigate])
}
