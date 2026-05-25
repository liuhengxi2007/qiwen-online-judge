import { Button } from '@/components/ui/button'
import type { DirectMessage } from '@/features/message/model/response/DirectMessage'
import type { MessageId } from '@/features/message/model/MessageId'
import { messageIdValue } from '@/features/message/lib/message-parsers'
import { DateTimeText } from '@/shared/components/date-time-text'
import { useI18n } from '@/shared/i18n/use-i18n'

type MessageBubbleProps = {
  autoMarkMessageRead: boolean
  isMarkingConversationRead: boolean
  isOwn: boolean
  isSending: boolean
  message: DirectMessage
  pendingReadMessageId: string | null
  markSingleMessageRead: (messageId: MessageId) => Promise<void>
}

export function MessageBubble({
  autoMarkMessageRead,
  isMarkingConversationRead,
  isOwn,
  isSending,
  message,
  pendingReadMessageId,
  markSingleMessageRead,
}: MessageBubbleProps) {
  const { t } = useI18n()
  const isUnreadIncoming = !isOwn && message.readAt === null
  const isPendingRead = pendingReadMessageId === messageIdValue(message.id)

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-3xl px-4 py-3 ${
          isOwn ? 'bg-cyan-300 text-cyan-950' : 'bg-white text-slate-900 shadow-sm'
        }`}
      >
        <p className="whitespace-pre-wrap text-sm leading-6">{message.content}</p>
        <div className={`mt-2 flex items-center gap-2 text-xs ${isOwn ? 'text-cyan-900' : 'text-slate-500'}`}>
          <DateTimeText value={message.createdAt} />
          {isOwn ? <span>{message.readAt ? t('messages.readStatus.read') : t('messages.readStatus.unread')}</span> : null}
          {isUnreadIncoming && !autoMarkMessageRead ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={isPendingRead || isMarkingConversationRead || isSending}
              className="h-auto px-2 py-1 text-xs text-sky-700 hover:text-sky-900"
              onClick={() => {
                void markSingleMessageRead(message.id)
              }}
            >
              {isPendingRead ? t('messages.markingRead') : t('messages.markRead')}
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  )
}
