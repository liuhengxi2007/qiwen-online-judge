import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import type { MessageId } from '@/objects/message/MessageId'
import { messageIdValue } from '@/objects/message/MessageId'
import { MessageBubble } from './message-bubble'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

type MessageListProps = {
  autoMarkMessageRead: boolean
  history: MessageHistoryResponse | null
  isLoading: boolean
  isLoadingOlderMessages: boolean
  isMarkingConversationRead: boolean
  isSending: boolean
  olderMessagesError: string
  pendingReadMessageId: string | null
  viewerUsername: Username
  loadOlderMessages: () => void
  markSingleMessageRead: (messageId: MessageId) => Promise<void>
}

export function MessageList({
  autoMarkMessageRead,
  history,
  isLoading,
  isLoadingOlderMessages,
  isMarkingConversationRead,
  isSending,
  olderMessagesError,
  pendingReadMessageId,
  viewerUsername,
  loadOlderMessages,
  markSingleMessageRead,
}: MessageListProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-3 rounded-3xl bg-slate-50 p-4">
      {isLoading && !history ? <p className="text-sm text-slate-500">{t('common.loading')}</p> : null}
      {!isLoading && history?.messages.length === 0 ? <p className="text-sm text-slate-500">{t('messages.noMessagesYet')}</p> : null}
      {history?.hasMore && history.messages.length > 0 ? (
        <div className="flex justify-center">
          <Button
            type="button"
            variant="outline"
            className="rounded-2xl border-slate-300 bg-white"
            disabled={isLoadingOlderMessages}
            onClick={loadOlderMessages}
          >
            {isLoadingOlderMessages ? t('messages.loadingOlder') : t('messages.loadOlder')}
          </Button>
        </div>
      ) : null}
      {olderMessagesError ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{olderMessagesError}</AlertDescription>
        </Alert>
      ) : null}
      {history?.messages.map((message) => (
        <MessageBubble
          key={messageIdValue(message.id)}
          autoMarkMessageRead={autoMarkMessageRead}
          isMarkingConversationRead={isMarkingConversationRead}
          isOwn={message.sender.username === viewerUsername}
          isSending={isSending}
          message={message}
          pendingReadMessageId={pendingReadMessageId}
          markSingleMessageRead={markSingleMessageRead}
        />
      ))}
    </div>
  )
}
