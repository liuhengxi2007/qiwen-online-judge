import { useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bell, LogOut, Mail } from 'lucide-react'

import { logout as logoutRequest } from '@/apis/auth/Logout'
import { Button } from '@/components/ui/button'
import { usernameValue } from '@/objects/user/Username'
import { useAuthStore } from '@/pages/stores/auth/use-auth-store'
import { useMessageStore } from '@/pages/stores/message/use-message-store'
import { useNotificationStore } from '@/pages/stores/notification/use-notification-store'
import { formatUserDisplayLabel } from '@/pages/objects/user-display-label'
import { useI18n } from '@/system/i18n/use-i18n'

type AccountActionsProps = {
  showSignOutLabel?: boolean
}

export function AccountActions({ showSignOutLabel = false }: AccountActionsProps) {
  const { t } = useI18n()
  const session = useAuthStore((state) => state.session)
  const clearSession = useAuthStore((state) => state.clearSession)
  const navigate = useNavigate()
  const signOut = useCallback(async () => {
    await logoutRequest().catch(() => undefined)
    clearSession()
    navigate('/login?notice=signed-out', { replace: true })
  }, [clearSession, navigate])
  const totalUnreadCount = useMessageStore((state) => state.totalUnreadCount)
  const unreadNotificationCount = useNotificationStore((state) => state.unreadCount)

  if (!session) {
    return null
  }

  return (
    <div className="flex flex-wrap items-center justify-end gap-2">
      <div className="inline-flex items-center rounded-xl border border-slate-300 bg-white shadow-sm">
        <Link
          className="inline-flex items-center rounded-l-xl px-3 py-1.5 text-sm text-slate-700 transition hover:bg-slate-50 hover:text-slate-950"
          to={`/user/${usernameValue(session.username)}`}
        >
          <span className="font-semibold text-slate-950">
            {formatUserDisplayLabel(session, session.preferences.displayMode)}
          </span>
        </Link>
        <span aria-hidden className="h-5 w-px bg-slate-200" />
        <Link
          aria-label={t('nav.openProfileNotifications')}
          className="relative inline-flex items-center justify-center px-3 py-1.5 text-slate-700 transition hover:bg-amber-50 hover:text-amber-950"
          title={t('nav.openProfileNotifications')}
          to="/notifications"
        >
          <Bell className="size-4" />
          {unreadNotificationCount > 0 ? (
            <span className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full bg-amber-500 px-1.5 text-[11px] font-semibold leading-5 text-white">
              {String(unreadNotificationCount)}
            </span>
          ) : null}
        </Link>
        <span aria-hidden className="h-5 w-px bg-slate-200" />
        <Link
          aria-label={t('nav.openProfileMessages')}
          className="relative inline-flex items-center justify-center rounded-r-xl px-3 py-1.5 text-slate-700 transition hover:bg-cyan-50 hover:text-cyan-950"
          title={t('nav.openProfileMessages')}
          to="/messages"
        >
          <Mail className="size-4" />
          {totalUnreadCount > 0 ? (
            <span className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full bg-cyan-500 px-1.5 text-[11px] font-semibold leading-5 text-white">
              {String(totalUnreadCount)}
            </span>
          ) : null}
        </Link>
      </div>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="rounded-xl border-slate-300 bg-white px-3"
        aria-label={t('dashboard.signOut')}
        title={t('dashboard.signOut')}
        onClick={() => {
          void signOut()
        }}
      >
        <LogOut className="size-4" />
        {showSignOutLabel ? t('dashboard.signOut') : null}
      </Button>
    </div>
  )
}
