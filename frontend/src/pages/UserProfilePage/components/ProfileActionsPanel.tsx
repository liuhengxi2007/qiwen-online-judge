import { Link } from 'react-router-dom'
import { Files, Mail, NotebookPen, Settings, ShieldBan } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/system/i18n/use-i18n'

type ProfileActionsPanelProps = {
  canManageTarget: boolean
  isOwnProfile: boolean
  onOpenMessage: () => void
  targetUsername: string
}

export function ProfileActionsPanel({
  canManageTarget,
  isOwnProfile,
  onOpenMessage,
  targetUsername,
}: ProfileActionsPanelProps) {
  const { t } = useI18n()

  return (
    <div id="profile-actions" className="rounded-3xl border border-slate-100 bg-slate-50 p-6 scroll-mt-28">
      <div className="grid gap-3 sm:grid-cols-2">
        <Button asChild className="h-11 rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
          <Link to={`/submissions?username=${encodeURIComponent(targetUsername)}`}>
            <Files className="size-4" />
            {t('userProfile.openSubmissions')}
          </Link>
        </Button>

        <Button asChild className="h-11 rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
          <Link to={`/user/${targetUsername}/blogs`}>
            <NotebookPen className="size-4" />
            {t('userProfile.openBlogs')}
          </Link>
        </Button>

        {isOwnProfile ? (
          <Button asChild className="h-11 rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400">
            <Link to="/messages">
              <Mail className="size-4" />
              {t('userProfile.openMessages')}
            </Link>
          </Button>
        ) : (
          <Button
            type="button"
            variant="outline"
            className="h-11 rounded-2xl border-cyan-300 bg-white text-cyan-950"
            onClick={onOpenMessage}
          >
            <Mail className="size-4" />
            {t('nav.messages')}
          </Button>
        )}

        {canManageTarget ? (
          <Button asChild variant="outline" className="h-11 rounded-2xl border-violet-300 bg-white text-violet-950">
            <Link to={`/user/${targetUsername}/settings`}>
              <Settings className="size-4" />
              {t('userProfile.openSettings')}
            </Link>
          </Button>
        ) : null}

        {isOwnProfile ? (
          <Button asChild variant="outline" className="h-11 rounded-2xl border-cyan-300 bg-white text-cyan-950 sm:col-span-2">
            <Link to={`/user/${targetUsername}/settings#message-blocks`}>
              <ShieldBan className="size-4" />
              {t('messages.manageBlocks')}
            </Link>
          </Button>
        ) : null}
      </div>
    </div>
  )
}
