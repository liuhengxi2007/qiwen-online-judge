import { Link } from 'react-router-dom'
import { Inbox } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { usernameValue } from '@/objects/user/Username'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { messageConversationPath } from '@/pages/routing/MessagePaths'
import { useI18n } from '@/system/i18n/use-i18n'

type MessageInboxCardProps = {
  conversations: MessageConversationSummary[]
  currentPage: number
  inboxActionError: string
  inboxError: string
  isLoadingInbox: boolean
  isMarkingAllRead: boolean
  onMarkAllRead: () => void
  onPageChange: (page: number) => void
  pageNumbers: number[]
  totalPages: number
  totalUnreadCount: number
}

export function MessageInboxCard({
  conversations,
  currentPage,
  inboxActionError,
  inboxError,
  isLoadingInbox,
  isMarkingAllRead,
  onMarkAllRead,
  onPageChange,
  pageNumbers,
  totalPages,
  totalUnreadCount,
}: MessageInboxCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
              <Inbox className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('messages.inboxTitle')}</CardTitle>
              <CardDescription>{t('messages.inboxDescription')}</CardDescription>
            </div>
          </div>
          <Button
            type="button"
            variant="outline"
            disabled={totalUnreadCount === 0 || isMarkingAllRead}
            className="rounded-2xl border-slate-300 bg-white"
            onClick={onMarkAllRead}
          >
            {isMarkingAllRead ? t('messages.markingRead') : t('messages.markAllRead')}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {inboxActionError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{inboxActionError}</AlertDescription>
          </Alert>
        ) : null}
        {inboxError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{inboxError}</AlertDescription>
          </Alert>
        ) : null}
        {isLoadingInbox && conversations.length === 0 ? <p className="text-sm text-slate-500">{t('common.loading')}</p> : null}
        {!isLoadingInbox && conversations.length === 0 ? <p className="text-sm text-slate-500">{t('messages.emptyInbox')}</p> : null}
        <div className="space-y-3">
          {conversations.map((conversation) => (
            <Link
              key={messageConversationIdValue(conversation.id)}
              className="block rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-sky-300 hover:bg-sky-50"
              to={messageConversationPath(conversation.otherUser.username)}
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-950">{conversation.otherUser.displayName}</p>
                  <p className="text-sm text-slate-600">@{usernameValue(conversation.otherUser.username)}</p>
                </div>
                {conversation.unreadCount > 0 ? (
                  <span className="rounded-full bg-sky-300 px-2.5 py-1 text-xs font-semibold text-sky-950">
                    {t('messages.unreadBadge', { count: String(conversation.unreadCount) })}
                  </span>
                ) : null}
              </div>
              <p className="mt-3 line-clamp-2 text-sm text-slate-700">
                {conversation.lastMessagePreview ?? t('messages.noMessagesYet')}
              </p>
              <DateTimeText className="mt-2 text-xs text-slate-500" value={conversation.lastMessageAt} />
            </Link>
          ))}
        </div>
        {!isLoadingInbox && conversations.length > 0 && totalPages > 1 ? (
          <PaginationControls
            currentPage={currentPage}
            pageNumbers={pageNumbers}
            totalPages={totalPages}
            previousLabel={t('common.pagination.previous')}
            nextLabel={t('common.pagination.next')}
            onPageChange={onPageChange}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}
