import { Link } from 'react-router-dom'
import { SendHorizontal, ShieldBan } from 'lucide-react'
import type { KeyboardEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { usernameValue } from '@/objects/user/user-parsers'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

type MessageComposerProps = {
  conversation: MessageConversationSummary | null
  draft: string
  isSending: boolean
  sendErrorMessage: string
  showManageBlocksShortcut: boolean
  viewerUsername: Username
  setDraft: (value: string) => void
  submitDraft: () => void
  handleDraftKeyDown: (event: KeyboardEvent<HTMLTextAreaElement>) => void
}

export function MessageComposer({
  conversation,
  draft,
  isSending,
  sendErrorMessage,
  showManageBlocksShortcut,
  viewerUsername,
  setDraft,
  submitDraft,
  handleDraftKeyDown,
}: MessageComposerProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-3">
      <Textarea
        className="min-h-32 rounded-3xl border-slate-300 bg-white"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleDraftKeyDown}
        placeholder={t('messages.composePlaceholder')}
      />
      {sendErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{sendErrorMessage}</AlertDescription>
        </Alert>
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-500">{t('messages.composeHelp')}</p>
        <div className="flex gap-2">
          {conversation && showManageBlocksShortcut ? (
            <Button asChild type="button" variant="outline" className="rounded-2xl border-rose-300 bg-white text-rose-950">
              <Link to={`/user/${usernameValue(viewerUsername)}/settings#message-blocks`}>
                <ShieldBan className="size-4" />
                {t('messages.manageBlocks')}
              </Link>
            </Button>
          ) : null}
          <Button
            type="button"
            disabled={isSending}
            className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400"
            onClick={submitDraft}
          >
            <SendHorizontal className="size-4" />
            {isSending ? t('messages.sending') : t('messages.send')}
          </Button>
        </div>
      </div>
    </div>
  )
}
