import { Link } from 'react-router-dom'
import { Files, NotebookPen, Settings } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/system/i18n/use-i18n'

type ProfileOverviewPanelProps = {
  canManageTarget: boolean
  isLoadingProfile: boolean
  isOwnProfile: boolean
  onOpenMessage: () => void
  profileName: string
  targetUsername: string
}

export function ProfileOverviewPanel({
  canManageTarget,
  isLoadingProfile,
  isOwnProfile,
  onOpenMessage,
  profileName,
  targetUsername,
}: ProfileOverviewPanelProps) {
  const { t } = useI18n()

  return (
    <>
      <div className="rounded-3xl bg-slate-50 p-6">
        <p className="text-sm text-slate-500">{t('common.displayName')}</p>
        <p className="mt-2 text-2xl font-semibold text-slate-900">{isLoadingProfile ? t('common.loading') : profileName}</p>
      </div>

      <div className="flex flex-wrap gap-3 rounded-3xl border border-slate-100 bg-slate-50 p-6">
        <Button asChild className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
          <Link to={`/submissions?username=${encodeURIComponent(targetUsername)}`}>
            <Files className="size-4" />
            {t('userProfile.openSubmissions')}
          </Link>
        </Button>
        <Button asChild className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
          <Link to={`/user/${targetUsername}/blogs`}>
            <NotebookPen className="size-4" />
            {t('userProfile.openBlogs')}
          </Link>
        </Button>
        {canManageTarget ? (
          <Button asChild variant="outline" className="rounded-2xl border-violet-300 bg-white text-violet-950">
            <Link to={`/user/${targetUsername}/settings`}>
              <Settings className="size-4" />
              {t('userProfile.openSettings')}
            </Link>
          </Button>
        ) : null}
        {!isOwnProfile ? (
          <Button
            type="button"
            variant="outline"
            className="rounded-2xl border-cyan-300 bg-white text-cyan-950"
            onClick={onOpenMessage}
          >
            {t('nav.messages')}
          </Button>
        ) : null}
      </div>
    </>
  )
}
