import { Link, useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { logout as logoutRequest } from '@/features/auth/api/auth-client'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { useI18n } from '@/shared/i18n/i18n'

export function AuthenticatedUserBar() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const session = useAuthStore((state) => state.session)
  const clearSession = useAuthStore((state) => state.clearSession)

  if (!session) {
    return null
  }

  async function signOut() {
    await logoutRequest().catch(() => undefined)
    clearSession()
    navigate('/login?notice=signed-out', { replace: true })
  }

  return (
    <div className="fixed right-4 top-4 z-50 flex flex-wrap items-center gap-3 rounded-full border border-slate-200 bg-white/90 px-4 py-2 shadow-[0_18px_40px_rgba(15,23,42,0.12)] backdrop-blur">
      <Link className="text-sm font-semibold text-slate-900 hover:underline" to={`/user/${usernameValue(session.username)}`}>
        {displayNameValue(session.displayName)}
      </Link>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="rounded-full border-slate-300 bg-white"
        onClick={() => {
          void signOut()
        }}
      >
        <LogOut className="size-4" />
        {t('dashboard.signOut')}
      </Button>
    </div>
  )
}
