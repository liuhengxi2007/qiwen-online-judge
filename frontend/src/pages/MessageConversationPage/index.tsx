import { Navigate, useParams } from 'react-router-dom'

import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { MessageConversationCard } from './components/MessageConversationCard'
import { useMessageConversation } from './hooks/useMessageConversation'
import { parseUsername } from '@/objects/user/Username'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 私信会话页，负责登录保护、目标用户名路由解析和会话模型装配。
 * 非法用户名回到消息收件箱，实际会话创建和历史加载由 hook 处理。
 */
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
    <PageShell
      title={
        conversationModel.conversation
          ? t('messages.conversationHeading', { displayName: conversationModel.conversation.otherUser.displayName })
          : t('messages.conversationFallbackHeading')
      }
      description={t('messages.conversationDescription')}
      mainClassName="bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)]"
    >
      <MessageConversationCard
        model={conversationModel}
        viewerUsername={session.username}
      />
    </PageShell>
  )
}
