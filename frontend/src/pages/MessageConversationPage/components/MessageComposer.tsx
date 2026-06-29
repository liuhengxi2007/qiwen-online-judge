import { Link } from 'react-router-dom'
import { SendHorizontal, ShieldBan } from 'lucide-react'
import type { KeyboardEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { usernameValue } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 私信发送框上下文。
 */
type MessageComposerContext = {
  conversation: MessageConversationSummary | null
  viewerUsername: Username
}

type MessageComposerState = {
  isSending: boolean
  sendErrorMessage: string
  showManageBlocksShortcut: boolean
}

type MessageComposerActions = {
  setDraft: (value: string) => void
  submitDraft: () => void
  handleDraftKeyDown: (event: KeyboardEvent<HTMLTextAreaElement>) => void
}

type MessageComposerProps = {
  context: MessageComposerContext
  draft: string
  state: MessageComposerState
  actions: MessageComposerActions
}

/**
 * 私信发送框，展示正文输入、发送错误、屏蔽管理快捷入口和发送按钮。
 */
export function MessageComposer({
  context,
  draft,
  state,
  actions,
}: MessageComposerProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-3">
      <Textarea
        className="min-h-32 rounded-3xl border-slate-300 bg-white"
        value={draft}
        onChange={(event) => actions.setDraft(event.target.value)}
        onKeyDown={actions.handleDraftKeyDown}
        placeholder={t('messages.composePlaceholder')}
      />
      {state.sendErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{state.sendErrorMessage}</AlertDescription>
        </Alert>
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-500">{t('messages.composeHelp')}</p>
        <div className="flex gap-2">
          {context.conversation && state.showManageBlocksShortcut ? (
            <Button asChild type="button" variant="outline" className="rounded-2xl border-rose-300 bg-white text-rose-950">
              <Link to={`/user/${usernameValue(context.viewerUsername)}/settings#message-blocks`}>
                <ShieldBan className="size-4" />
                {t('messages.manageBlocks')}
              </Link>
            </Button>
          ) : null}
          <Button
            type="button"
            disabled={state.isSending}
            className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400"
            onClick={actions.submitDraft}
          >
            <SendHorizontal className="size-4" />
            {state.isSending ? t('messages.sending') : t('messages.send')}
          </Button>
        </div>
      </div>
    </div>
  )
}
