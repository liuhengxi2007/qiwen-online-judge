import { Button } from '@/components/ui/button'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageId } from '@/objects/message/MessageId'
import { messageIdValue } from '@/objects/message/MessageId'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 单条私信气泡已读控制状态。
 */
export type MessageReadControls = {
  autoMarkMessageRead: boolean
  isMarkingConversationRead: boolean
  pendingReadMessageId: string | null
  markSingleMessageRead: (messageId: MessageId) => Promise<void>
}

type MessageBubbleProps = {
  message: DirectMessage
  isOwn: boolean
  isSending: boolean
  readControls: MessageReadControls
}

/**
 * 单条私信气泡，区分自己/对方消息样式，并在允许手动已读时展示单条标记按钮。
 */
export function MessageBubble({
  message,
  isOwn,
  isSending,
  readControls,
}: MessageBubbleProps) {
  const { t } = useI18n()
  const isUnreadIncoming = !isOwn && message.readAt === null
  const isPendingRead = readControls.pendingReadMessageId === messageIdValue(message.id)

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
          {isUnreadIncoming && !readControls.autoMarkMessageRead ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={isPendingRead || readControls.isMarkingConversationRead || isSending}
              className="h-auto px-2 py-1 text-xs text-sky-700 hover:text-sky-900"
              onClick={() => {
                void readControls.markSingleMessageRead(message.id)
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
