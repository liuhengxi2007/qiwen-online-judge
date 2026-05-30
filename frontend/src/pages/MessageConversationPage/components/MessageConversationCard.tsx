import { Link } from 'react-router-dom'
import { MessageCircle } from 'lucide-react'
import type { KeyboardEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { MessageComposer } from './MessageComposer'
import { MessageList } from './MessageList'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import type { MessageId } from '@/objects/message/MessageId'
import { usernameValue } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

type MessageConversationCardProps = {
  autoMarkMessageRead: boolean
  conversation: MessageConversationSummary | null
  draft: string
  errorMessage: string
  hasUnreadMessages: boolean
  history: MessageHistoryResponse | null
  isLoading: boolean
  isLoadingOlderMessages: boolean
  isMarkingConversationRead: boolean
  isSending: boolean
  olderMessagesError: string
  pendingReadMessageId: string | null
  sendErrorMessage: string
  showManageBlocksShortcut: boolean
  viewerUsername: Username
  handleDraftKeyDown: (event: KeyboardEvent<HTMLTextAreaElement>) => void
  loadOlderMessages: () => void
  markSingleMessageRead: (messageId: MessageId) => Promise<void>
  markWholeConversationRead: () => Promise<void>
  setDraft: (value: string) => void
  submitDraft: () => void
}

export function MessageConversationCard({
  autoMarkMessageRead,
  conversation,
  draft,
  errorMessage,
  hasUnreadMessages,
  history,
  isLoading,
  isLoadingOlderMessages,
  isMarkingConversationRead,
  isSending,
  olderMessagesError,
  pendingReadMessageId,
  sendErrorMessage,
  showManageBlocksShortcut,
  viewerUsername,
  handleDraftKeyDown,
  loadOlderMessages,
  markSingleMessageRead,
  markWholeConversationRead,
  setDraft,
  submitDraft,
}: MessageConversationCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <MessageCircle className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">
                {conversation ? (
                  <Link
                    className="transition hover:text-cyan-800 hover:underline"
                    to={`/user/${usernameValue(conversation.otherUser.username)}`}
                  >
                    {conversation.otherUser.displayName}
                  </Link>
                ) : (
                  t('messages.conversationTitle')
                )}
              </CardTitle>
              <CardDescription>
                {conversation
                  ? `@${usernameValue(conversation.otherUser.username)}`
                  : t('messages.conversationTitleDescription')}
              </CardDescription>
            </div>
          </div>
          {conversation && !autoMarkMessageRead ? (
            <Button
              type="button"
              variant="outline"
              disabled={!hasUnreadMessages || isMarkingConversationRead || isSending}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={() => {
                void markWholeConversationRead()
              }}
            >
              {isMarkingConversationRead ? t('messages.markingRead') : t('messages.markConversationRead')}
            </Button>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        {errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <MessageList
          autoMarkMessageRead={autoMarkMessageRead}
          history={history}
          isLoading={isLoading}
          isLoadingOlderMessages={isLoadingOlderMessages}
          isMarkingConversationRead={isMarkingConversationRead}
          isSending={isSending}
          olderMessagesError={olderMessagesError}
          pendingReadMessageId={pendingReadMessageId}
          viewerUsername={viewerUsername}
          loadOlderMessages={loadOlderMessages}
          markSingleMessageRead={markSingleMessageRead}
        />

        <MessageComposer
          conversation={conversation}
          draft={draft}
          isSending={isSending}
          sendErrorMessage={sendErrorMessage}
          showManageBlocksShortcut={showManageBlocksShortcut}
          viewerUsername={viewerUsername}
          setDraft={setDraft}
          submitDraft={submitDraft}
          handleDraftKeyDown={handleDraftKeyDown}
        />
      </CardContent>
    </Card>
  )
}
