import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Inbox, MessageSquareMore, Search } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { usernameValue } from '@/features/auth/domain/auth'
import type { UserIdentity } from '@/features/auth/domain/auth'
import { markAllMessagesRead } from '@/features/message/api/message-client'
import { messageConversationPath, messageConversationIdValue } from '@/features/message/domain/message'
import { useMessageStore } from '@/features/message/stores/use-message-store'
import { listUserSuggestions } from '@/features/user/api/user-client'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { HttpClientError } from '@/shared/api/http-client'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

export function MessageInboxPage() {
  const { t } = useI18n()
  usePageTitle(t('messages.pageTitle'))
  const navigate = useNavigate()
  const { session, navigationIntent } = useSessionGuard()
  const conversations = useMessageStore((state) => state.conversations)
  const totalUnreadCount = useMessageStore((state) => state.totalUnreadCount)
  const isLoadingInbox = useMessageStore((state) => state.isLoadingInbox)
  const inboxError = useMessageStore((state) => state.inboxError)
  const refreshInbox = useMessageStore((state) => state.refreshInbox)
  const [searchQuery, setSearchQuery] = useState('')
  const [suggestions, setSuggestions] = useState<UserIdentity[]>([])
  const [searchError, setSearchError] = useState('')
  const [inboxActionError, setInboxActionError] = useState('')
  const [isMarkingAllRead, setIsMarkingAllRead] = useState(false)

  useEffect(() => {
    void refreshInbox()
  }, [refreshInbox])

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSuggestions([])
      setSearchError('')
      return
    }

    const timeoutId = window.setTimeout(() => {
      void listUserSuggestions(searchQuery)
        .then((items) => {
          setSuggestions(items)
          setSearchError('')
        })
        .catch((error) => {
          setSuggestions([])
          setSearchError(error instanceof HttpClientError ? error.message : t('messages.searchFailed'))
        })
    }, 150)

    return () => window.clearTimeout(timeoutId)
  }, [searchQuery, t])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!session) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('messages.heading')}</h1>
            <p className="text-sm text-slate-600">{t('messages.description', { totalUnreadCount: String(totalUnreadCount) })}</p>
          </div>
          <AncestorNavigation />
        </div>

        <AppSectionBar />

        <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
                  <MessageSquareMore className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('messages.newConversationTitle')}</CardTitle>
                  <CardDescription>{t('messages.newConversationDescription')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {searchError ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{searchError}</AlertDescription>
                </Alert>
              ) : null}
              <div className="space-y-2">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-3 size-4 text-slate-400" />
                  <Input
                    className="rounded-2xl border-slate-300 bg-white pl-10"
                    value={searchQuery}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder={t('messages.searchPlaceholder')}
                  />
                </div>
                <div className="space-y-2">
                  {suggestions
                    .filter((suggestion) => suggestion.username !== session.username)
                    .map((suggestion) => (
                      <button
                        key={usernameValue(suggestion.username)}
                        type="button"
                        className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-cyan-300 hover:bg-cyan-50"
                        onClick={() => navigate(messageConversationPath(suggestion.username))}
                      >
                        <div>
                          <p className="font-medium text-slate-950">{suggestion.displayName}</p>
                          <p className="text-sm text-slate-600">@{usernameValue(suggestion.username)}</p>
                        </div>
                        <span className="text-sm font-medium text-cyan-700">{t('messages.openConversation')}</span>
                      </button>
                    ))}
                </div>
              </div>
            </CardContent>
          </Card>

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
                  onClick={() => {
                    setIsMarkingAllRead(true)
                    void markAllMessagesRead()
                      .then(async () => {
                        setInboxActionError('')
                        await refreshInbox()
                      })
                      .catch((error) => {
                        setInboxActionError(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
                      })
                      .finally(() => setIsMarkingAllRead(false))
                  }}
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
                    <p className="mt-2 text-xs text-slate-500">{new Date(conversation.lastMessageAt).toLocaleString()}</p>
                  </Link>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
