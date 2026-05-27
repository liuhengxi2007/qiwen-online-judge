import { Link } from 'react-router-dom'
import { Mail, ShieldBan } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/system/i18n/use-i18n'

type OwnMessagePanelProps = {
  targetUsername: string
}

export function OwnMessagePanel({ targetUsername }: OwnMessagePanelProps) {
  const { t } = useI18n()

  return (
    <div id="profile-messages" className="rounded-3xl border border-cyan-100 bg-cyan-50 p-6 scroll-mt-28">
      <div className="flex items-start gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
          <Mail className="size-4" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-base font-semibold text-cyan-950">{t('userProfile.messagesTitle')}</p>
          <p className="mt-1 text-sm text-cyan-800">{t('userProfile.messagesDescription')}</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <Button asChild className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400">
              <Link to="/messages">
                <Mail className="size-4" />
                {t('userProfile.openMessages')}
              </Link>
            </Button>
            <Button asChild variant="outline" className="rounded-2xl border-cyan-300 bg-white text-cyan-950">
              <Link to={`/user/${targetUsername}/settings#message-blocks`}>
                <ShieldBan className="size-4" />
                {t('messages.manageBlocks')}
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
