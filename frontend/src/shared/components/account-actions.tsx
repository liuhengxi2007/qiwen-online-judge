import { Link, useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { logout as logoutRequest } from '@/features/auth/api/auth-client'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { useI18n } from '@/shared/i18n/i18n'

export function AccountActions() {
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
    <div className="flex flex-wrap items-center justify-end gap-2">
      <Link
        className="inline-flex items-center rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 transition hover:border-slate-400 hover:text-slate-950"
        to={`/user/${usernameValue(session.username)}`}
      >
        <span className="font-semibold text-slate-950">{displayNameValue(session.displayName)}</span>
        <span className="ml-2 font-mono text-xs text-slate-500">@{usernameValue(session.username)}</span>
      </Link>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="rounded-xl border-slate-300 bg-white px-3"
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
