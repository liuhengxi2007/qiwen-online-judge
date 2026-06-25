import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import { messageIdValue } from '@/objects/message/MessageId'
import { MessageBubble } from './MessageBubble'
import type { MessageReadControls } from './MessageBubble'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 私信消息列表加载状态。
 */
type MessageListLoadingState = {
  isLoading: boolean
  isLoadingOlderMessages: boolean
  olderMessagesError: string
}

type MessageListProps = {
  history: MessageHistoryResponse | null
  viewerUsername: Username
  isSending: boolean
  loadingState: MessageListLoadingState
  readControls: MessageReadControls
  onLoadOlderMessages: () => void
}

/**
 * 私信消息列表，展示加载/空状态、历史分页按钮、错误消息和按发送方区分的消息气泡。
 */
export function MessageList({
  history,
  viewerUsername,
  isSending,
  loadingState,
  readControls,
  onLoadOlderMessages,
}: MessageListProps) {
  // 保留扁平 props：列表数据、发送状态、加载状态和已读控制已按职责分组，再拆会让调用端更绕。
  const { t } = useI18n()

  return (
    <div className="space-y-3 rounded-3xl bg-slate-50 p-4">
      {loadingState.isLoading && !history ? <p className="text-sm text-slate-500">{t('common.loading')}</p> : null}
      {!loadingState.isLoading && history?.messages.length === 0 ? <p className="text-sm text-slate-500">{t('messages.noMessagesYet')}</p> : null}
      {history?.hasMore && history.messages.length > 0 ? (
        <div className="flex justify-center">
          <Button
            type="button"
            variant="outline"
            className="rounded-2xl border-slate-300 bg-white"
            disabled={loadingState.isLoadingOlderMessages}
            onClick={onLoadOlderMessages}
          >
            {loadingState.isLoadingOlderMessages ? t('messages.loadingOlder') : t('messages.loadOlder')}
          </Button>
        </div>
      ) : null}
      {loadingState.olderMessagesError ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{loadingState.olderMessagesError}</AlertDescription>
        </Alert>
      ) : null}
      {history?.messages.map((message) => (
        <MessageBubble
          key={messageIdValue(message.id)}
          message={message}
          isOwn={message.sender.username === viewerUsername}
          isSending={isSending}
          readControls={readControls}
        />
      ))}
    </div>
  )
}
