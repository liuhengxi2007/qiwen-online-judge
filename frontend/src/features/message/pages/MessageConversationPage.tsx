import { useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { MessageCircle, SendHorizontal, ShieldBan } from 'lucide-react'
import type { KeyboardEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { usernameValue } from '@/features/auth/domain/auth'
import { getConversationHistory, markConversationRead, sendDirectMessage } from '@/features/message/api/message-client'
import type { MessageHistoryResponse } from '@/features/message/domain/message'
import { messageConversationIdValue, messageIdValue, parseMessageContent, parseMessageConversationId } from '@/features/message/domain/message'
import { messageStreamEventName } from '@/features/message/hooks/use-message-realtime-connection'
import { useMessageStore } from '@/features/message/stores/use-message-store'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { HttpClientError } from '@/shared/api/http-client'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

type MessageStreamEventDetail =
  | { type: 'message_received'; payload: { conversationId?: string } }
  | { type: 'conversation_read'; payload: { conversationId?: string } }
  | { type: 'inbox_changed'; payload: unknown }

const minimumIncomingMessagesBeforeBlockShortcut = 5

export function MessageConversationPage() {
  const { t } = useI18n()
  usePageTitle(t('messages.conversationPageTitle'))
  const { conversationId: routeConversationId } = useParams<{ conversationId: string }>()
  const { session, navigationIntent } = useSessionGuard()
  const refreshInbox = useMessageStore((state) => state.refreshInbox)
  const [history, setHistory] = useState<MessageHistoryResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [sendErrorMessage, setSendErrorMessage] = useState('')
  const [draft, setDraft] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [isMarkingConversationRead, setIsMarkingConversationRead] = useState(false)
  const [pendingReadMessageId, setPendingReadMessageId] = useState<string | null>(null)

  const parsedConversationId = routeConversationId ? parseMessageConversationId(routeConversationId) : null
  const conversationId = parsedConversationId && parsedConversationId.ok ? parsedConversationId.value : null

  useEffect(() => {
    if (!conversationId) {
      return
    }
    const activeConversationId = conversationId

    let cancelled = false

    async function loadConversation() {
      setIsLoading(true)
      try {
        const response = await getConversationHistory(activeConversationId)
        if (cancelled) {
          return
        }
        setHistory(response)
        setErrorMessage('')
        setSendErrorMessage('')
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadConversation()

    return () => {
      cancelled = true
    }
  }, [conversationId, refreshInbox, t])

  useEffect(() => {
    if (!conversationId) {
      return
    }
    const activeConversationId = conversationId

    function handleRealtimeEvent(event: Event) {
      const detail = (event as CustomEvent<MessageStreamEventDetail>).detail
      if (!detail || detail.type === 'inbox_changed') {
        return
      }
      if (detail.payload.conversationId !== messageConversationIdValue(activeConversationId)) {
        return
      }

      void getConversationHistory(activeConversationId)
        .then((response) => {
          setHistory(response)
          setErrorMessage('')
          setSendErrorMessage('')
          void refreshInbox()
        })
        .catch((error) => {
          setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
        })
    }

    window.addEventListener(messageStreamEventName, handleRealtimeEvent as EventListener)
    return () => window.removeEventListener(messageStreamEventName, handleRealtimeEvent as EventListener)
  }, [conversationId, refreshInbox, t])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!session) {
    return <Navigate replace to="/login" />
  }

  if (!conversationId) {
    return <Navigate replace to="/messages" />
  }

  const conversation = history?.conversation ?? null
  const showManageBlocksShortcut =
    !history?.facts.viewerHasSentMessage &&
    (history?.facts.otherParticipantMessageCount ?? 0) >= minimumIncomingMessagesBeforeBlockShortcut
  const hasUnreadMessages = (history?.conversation.unreadCount ?? 0) > 0
  const submitDraft = () => {
    const validation = parseMessageContent(draft)
    if (!validation.ok) {
      setSendErrorMessage(validation.error)
      return
    }

    setIsSending(true)
    void sendDirectMessage(conversationId, { content: validation.value })
      .then(async () => {
        setDraft('')
        await markConversationRead(conversationId, { mode: 'conversation' })
        const response = await getConversationHistory(conversationId)
        setHistory(response)
        setErrorMessage('')
        setSendErrorMessage('')
        void refreshInbox()
      })
      .catch((error) => {
        if (error instanceof HttpClientError && error.code === 'api.error.message.blocked_by_recipient') {
          setSendErrorMessage(error.message)
          return
        }
        setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.sendFailed'))
      })
      .finally(() => setIsSending(false))
  }

  const handleDraftKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' && !isSending) {
      event.preventDefault()
      submitDraft()
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {conversation ? t('messages.conversationHeading', { displayName: conversation.otherUser.displayName }) : t('messages.conversationFallbackHeading')}
            </h1>
            <p className="text-sm text-slate-600">{t('messages.conversationDescription')}</p>
          </div>
          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
                      <Link className="transition hover:text-cyan-800 hover:underline" to={`/user/${usernameValue(conversation.otherUser.username)}`}>
                        {conversation.otherUser.displayName}
                      </Link>
                    ) : (
                      t('messages.conversationTitle')
                    )}
                  </CardTitle>
                  <CardDescription>
                    {conversation ? `@${usernameValue(conversation.otherUser.username)}` : t('messages.conversationTitleDescription')}
                  </CardDescription>
                </div>
              </div>
              {conversation ? (
                <Button
                  type="button"
                  variant="outline"
                  disabled={!hasUnreadMessages || isMarkingConversationRead || isSending}
                  className="rounded-2xl border-slate-300 bg-white"
                  onClick={() => {
                    setIsMarkingConversationRead(true)
                    void markConversationRead(conversationId, { mode: 'conversation' })
                      .then(async () => {
                        const response = await getConversationHistory(conversationId)
                        setHistory(response)
                        setErrorMessage('')
                        setSendErrorMessage('')
                        void refreshInbox()
                      })
                      .catch((error) => {
                        setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
                      })
                      .finally(() => setIsMarkingConversationRead(false))
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

            <div className="space-y-3 rounded-3xl bg-slate-50 p-4">
              {isLoading && !history ? <p className="text-sm text-slate-500">{t('common.loading')}</p> : null}
              {!isLoading && history?.messages.length === 0 ? <p className="text-sm text-slate-500">{t('messages.noMessagesYet')}</p> : null}
              {history?.messages.map((message) => {
                const isOwn = message.sender.username === session.username
                const isUnreadIncoming = !isOwn && message.readAt === null
                const isPendingRead = pendingReadMessageId === messageIdValue(message.id)
                return (
                  <div key={message.id} className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[80%] rounded-3xl px-4 py-3 ${
                        isOwn ? 'bg-cyan-300 text-cyan-950' : 'bg-white text-slate-900 shadow-sm'
                      }`}
                    >
                      <p className="whitespace-pre-wrap text-sm leading-6">{message.content}</p>
                      <div className={`mt-2 flex items-center gap-2 text-xs ${isOwn ? 'text-cyan-900' : 'text-slate-500'}`}>
                        <span>{new Date(message.createdAt).toLocaleString()}</span>
                        {isOwn ? <span>{message.readAt ? t('messages.readStatus.read') : t('messages.readStatus.unread')}</span> : null}
                        {isUnreadIncoming ? (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            disabled={isPendingRead || isMarkingConversationRead || isSending}
                            className="h-auto px-2 py-1 text-xs text-sky-700 hover:text-sky-900"
                            onClick={() => {
                              setPendingReadMessageId(messageIdValue(message.id))
                              void markConversationRead(conversationId, { mode: 'message', messageId: message.id })
                                .then(async () => {
                                  const response = await getConversationHistory(conversationId)
                                  setHistory(response)
                                  setErrorMessage('')
                                  setSendErrorMessage('')
                                  void refreshInbox()
                                })
                                .catch((error) => {
                                  setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
                                })
                                .finally(() => setPendingReadMessageId(null))
                            }}
                          >
                            {isPendingRead ? t('messages.markingRead') : t('messages.markRead')}
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>

            <div className="space-y-3">
              <Textarea
                className="min-h-32 rounded-3xl border-slate-300 bg-white"
                value={draft}
                onChange={(event) => {
                  setDraft(event.target.value)
                  if (sendErrorMessage) {
                    setSendErrorMessage('')
                  }
                }}
                onKeyDown={handleDraftKeyDown}
                placeholder={t('messages.composePlaceholder')}
              />
              {sendErrorMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{sendErrorMessage}</AlertDescription>
                </Alert>
              ) : null}
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-slate-500">{t('messages.composeHelp')}</p>
                <div className="flex gap-2">
                  {conversation && showManageBlocksShortcut ? (
                    <Button asChild type="button" variant="outline" className="rounded-2xl border-rose-300 bg-white text-rose-950">
                      <Link to={`/user/${usernameValue(session.username)}/settings#message-blocks`}>
                        <ShieldBan className="size-4" />
                        {t('messages.manageBlocks')}
                      </Link>
                    </Button>
                  ) : null}
                  <Button
                    type="button"
                    disabled={isSending}
                    className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400"
                    onClick={submitDraft}
                  >
                    <SendHorizontal className="size-4" />
                    {isSending ? t('messages.sending') : t('messages.send')}
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
