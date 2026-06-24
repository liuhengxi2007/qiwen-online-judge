import { Link } from 'react-router-dom'
import { MessageCircle } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { MessageComposer } from './MessageComposer'
import { MessageList } from './MessageList'
import { usernameValue } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'
import type { MessageConversationModel } from '../hooks/useMessageConversation'

/**
 * 私信会话卡片属性，直接接收会话页模型。
 */
type MessageConversationCardProps = {
  model: MessageConversationModel
  viewerUsername: Username
}

/**
 * 私信会话卡片，组合会话标题、手动标记已读按钮、消息列表和发送框。
 * 组件不直接调用 API，所有副作用通过传入的模型回调触发。
 */
export function MessageConversationCard({
  model,
  viewerUsername,
}: MessageConversationCardProps) {
  const { t } = useI18n()
  const {
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
    handleDraftKeyDown,
    loadOlderMessages,
    markSingleMessageRead,
    markWholeConversationRead,
    setDraft,
    submitDraft,
  } = model

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
