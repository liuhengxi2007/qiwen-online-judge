import { useEffect, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useMessageInboxActions } from './hooks/useMessageInboxActions'
import { useMessageInboxRefresh } from '@/pages/hooks/useMessageInboxRefresh'
import { useMessageRecipientSuggestions } from './hooks/useMessageRecipientSuggestions'
import { messageConversationPath } from '@/pages/routing/MessagePaths'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import { MessageInboxCard } from './components/MessageInboxCard'
import { MessageRecipientSearchCard } from './components/MessageRecipientSearchCard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'

const conversationsPerPage = 10

/**
 * 私信收件箱页面，负责会话守卫、分页加载、收件人搜索和全部已读操作组合。
 */
export function MessageInboxPage() {
  const { t } = useI18n()
  usePageTitle(t('messages.pageTitle'))
  const navigate = useNavigate()
  const { session, navigationIntent } = useSessionGuard()
  const conversations = useMessageStore((state) => state.conversations)
  const totalUnreadCount = useMessageStore((state) => state.totalUnreadCount)
  const totalItems = useMessageStore((state) => state.totalItems)
  const pageSize = useMessageStore((state) => state.pageSize)
  const isLoadingInbox = useMessageStore((state) => state.isLoadingInbox)
  const inboxError = useMessageStore((state) => state.inboxError)
  const refreshInbox = useMessageInboxRefresh()
  const [searchQuery, setSearchQuery] = useState('')
  const recipientSuggestions = useMessageRecipientSuggestions(searchQuery, t('messages.searchFailed'))
  const inboxActions = useMessageInboxActions({
    refreshInbox,
    markAllReadFailedMessage: t('messages.loadFailed'),
  })
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const totalPages = calculateTotalPages(totalItems, pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  useEffect(() => {
    void refreshInbox({ page: currentPage, pageSize: conversationsPerPage })
  }, [currentPage, refreshInbox])

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: isLoadingInbox,
    setSearchParams,
  })

  const visibleSuggestions = searchQuery.trim() ? recipientSuggestions.suggestions : []
  const visibleSearchError = searchQuery.trim() ? recipientSuggestions.searchError : ''

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!session) {
    return <Navigate replace to="/login" />
  }

  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell
      title={t('messages.heading')}
      description={t('messages.description', { totalUnreadCount: String(totalUnreadCount) })}
      mainClassName="bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)]"
    >
      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <MessageRecipientSearchCard
          currentUsername={session.username}
          searchQuery={searchQuery}
          searchError={visibleSearchError}
          suggestions={visibleSuggestions}
          onSearchQueryChange={setSearchQuery}
          onSuggestionSelect={(username) => navigate(messageConversationPath(username))}
        />

        <MessageInboxCard
          conversations={conversations}
          currentPage={currentPage}
          inboxActionError={inboxActions.inboxActionError}
          inboxError={inboxError}
          isLoadingInbox={isLoadingInbox}
          isMarkingAllRead={inboxActions.isMarkingAllRead}
          onMarkAllRead={() => {
            void inboxActions.markAllRead({ page: currentPage, pageSize: conversationsPerPage })
          }}
          onPageChange={onPageChange}
          pageNumbers={pageNumbers}
          totalPages={totalPages}
          totalUnreadCount={totalUnreadCount}
        />
      </div>
    </PageShell>
  )
}
