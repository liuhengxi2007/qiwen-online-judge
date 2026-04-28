import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { MessageCircle, SendHorizontal, ShieldBan } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { usernameValue } from '@/features/auth/domain/auth'
import { getConversationHistory, markConversationRead, sendDirectMessage } from '@/features/message/api/message-client'
import type { MessageHistoryResponse } from '@/features/message/domain/message'
import { messageConversationIdValue, parseMessageContent, parseMessageConversationId } from '@/features/message/domain/message'
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

export function MessageConversationPage() {
  const { t } = useI18n()
  usePageTitle(t('messages.conversationPageTitle'))
  const navigate = useNavigate()
  const { conversationId: routeConversationId } = useParams<{ conversationId: string }>()
  const { session, navigationIntent } = useSessionGuard()
  const refreshInbox = useMessageStore((state) => state.refreshInbox)
  const [history, setHistory] = useState<MessageHistoryResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [draft, setDraft] = useState('')
  const [isSending, setIsSending] = useState(false)

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
        await markConversationRead(activeConversationId)
        void refreshInbox()
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
        .then(async (response) => {
          setHistory(response)
          if (detail.type === 'message_received') {
            await markConversationRead(activeConversationId)
          }
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

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
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
                    {conversation?.otherUser.displayName ?? t('messages.conversationTitle')}
                  </CardTitle>
                  <CardDescription>
                    {conversation ? `@${usernameValue(conversation.otherUser.username)}` : t('messages.conversationTitleDescription')}
                  </CardDescription>
                </div>
              </div>
              {conversation ? (
                <div className="flex gap-2">
                  <Button asChild variant="outline" className="rounded-2xl border-cyan-300 bg-white text-cyan-950">
                    <Link to={`/user/${usernameValue(conversation.otherUser.username)}`}>{t('messages.openProfile')}</Link>
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    onClick={() => navigate('/messages')}
                  >
                    {t('messages.backToInbox')}
                  </Button>
                </div>
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
                        {isOwn ? <span>{message.readAt ? t('messages.readStatus.read') : t('messages.readStatus.sent')}</span> : null}
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
                onChange={(event) => setDraft(event.target.value)}
                placeholder={t('messages.composePlaceholder')}
              />
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-slate-500">{t('messages.composeHelp')}</p>
                <div className="flex gap-2">
                  {conversation ? (
                    <Button
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-rose-300 bg-white text-rose-950"
                      onClick={() => navigate(`/user/${usernameValue(session.username)}/settings`)}
                    >
                      <ShieldBan className="size-4" />
                      {t('messages.manageBlocks')}
                    </Button>
                  ) : null}
                  <Button
                    type="button"
                    disabled={isSending}
                    className="rounded-2xl bg-cyan-300 text-cyan-950 hover:bg-cyan-400"
                    onClick={() => {
                      const validation = parseMessageContent(draft)
                      if (!validation.ok) {
                        setErrorMessage(validation.error)
                        return
                      }

                      setIsSending(true)
                      void sendDirectMessage(conversationId, { content: validation.value })
                        .then(async () => {
                          setDraft('')
                          const response = await getConversationHistory(conversationId)
                          setHistory(response)
                          setErrorMessage('')
                          await markConversationRead(conversationId)
                          void refreshInbox()
                        })
                        .catch((error) => {
                          setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.sendFailed'))
                        })
                        .finally(() => setIsSending(false))
                    }}
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
