import { Navigate, useParams } from 'react-router-dom'

import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { MessageConversationCard } from '@/features/message/components/message-conversation-card'
import { MessageConversationPageHeader } from '@/features/message/components/message-conversation-page-header'
import { useMessageConversation } from '@/features/message/hooks/use-message-conversation'
import { parseUsername } from '@/features/user/lib/user-parsers'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

export function MessageConversationPage() {
  const { t } = useI18n()
  usePageTitle(t('messages.conversationPageTitle'))
  const { username: routeUsername } = useParams<{ username: string }>()
  const { session, navigationIntent } = useSessionGuard()
  const parsedRouteUsername = routeUsername ? parseUsername(routeUsername) : null
  const targetUsername = parsedRouteUsername && parsedRouteUsername.ok ? parsedRouteUsername.value : null
  const conversationModel = useMessageConversation({ session, targetUsername })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!session) {
    return <Navigate replace to="/login" />
  }

  if (!targetUsername) {
    return <Navigate replace to="/messages" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <MessageConversationPageHeader conversation={conversationModel.conversation} />
        <MessageConversationCard
          autoMarkMessageRead={conversationModel.autoMarkMessageRead}
          conversation={conversationModel.conversation}
          draft={conversationModel.draft}
          errorMessage={conversationModel.errorMessage}
          hasUnreadMessages={conversationModel.hasUnreadMessages}
          history={conversationModel.history}
          isLoading={conversationModel.isLoading}
          isLoadingOlderMessages={conversationModel.isLoadingOlderMessages}
          isMarkingConversationRead={conversationModel.isMarkingConversationRead}
          isSending={conversationModel.isSending}
          olderMessagesError={conversationModel.olderMessagesError}
          pendingReadMessageId={conversationModel.pendingReadMessageId}
          sendErrorMessage={conversationModel.sendErrorMessage}
          showManageBlocksShortcut={conversationModel.showManageBlocksShortcut}
          viewerUsername={session.username}
          handleDraftKeyDown={conversationModel.handleDraftKeyDown}
          loadOlderMessages={conversationModel.loadOlderMessages}
          markSingleMessageRead={conversationModel.markSingleMessageRead}
          markWholeConversationRead={conversationModel.markWholeConversationRead}
          setDraft={conversationModel.setDraft}
          submitDraft={conversationModel.submitDraft}
        />
      </section>
    </main>
  )
}
