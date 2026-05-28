import { useCallback, useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'

import { CreateConversation } from '@/apis/message/CreateConversation'
import { GetConversationHistory } from '@/apis/message/GetConversationHistory'
import { MarkConversationRead } from '@/apis/message/MarkConversationRead'
import { SendDirectMessage } from '@/apis/message/SendDirectMessage'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import type { MessageId } from '@/objects/message/MessageId'
import { parseMessageContent } from '@/objects/message/MessageContent'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import { messageIdValue } from '@/objects/message/MessageId'
import {
  messageStreamEventName,
  type MessageStreamEventDetail,
} from '@/pages/hooks/use-message-realtime-connection'
import { useMessageInboxRefresh } from '@/pages/hooks/use-message-inbox-refresh'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

const minimumIncomingMessagesBeforeBlockShortcut = 5

type UseMessageConversationArgs = {
  session: SessionResponse | null
  targetUsername: Username | null
}

export function useMessageConversation({ session, targetUsername }: UseMessageConversationArgs) {
  const { t } = useI18n()
  const refreshInbox = useMessageInboxRefresh()
  const [history, setHistory] = useState<MessageHistoryResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [sendErrorMessage, setSendErrorMessage] = useState('')
  const [draft, setDraft] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [isLoadingOlderMessages, setIsLoadingOlderMessages] = useState(false)
  const [olderMessagesError, setOlderMessagesError] = useState('')
  const [isMarkingConversationRead, setIsMarkingConversationRead] = useState(false)
  const [pendingReadMessageId, setPendingReadMessageId] = useState<string | null>(null)
  const [conversationId, setConversationId] = useState<MessageConversationId | null>(null)
  const isAutoMarkingConversationReadRef = useRef(false)
  const autoMarkMessageRead = session?.preferences.autoMarkMessageRead ?? false

  const syncConversationReadState = useCallback(
    async (
      activeConversationId: MessageConversationId,
      response: MessageHistoryResponse,
      options: { autoMarkRead?: boolean } = {},
    ): Promise<MessageHistoryResponse> => {
      const shouldAutoMarkRead = options.autoMarkRead ?? true
      if (
        !shouldAutoMarkRead ||
        !autoMarkMessageRead ||
        response.conversation.unreadCount <= 0 ||
        isAutoMarkingConversationReadRef.current
      ) {
        return response
      }

      isAutoMarkingConversationReadRef.current = true
      setIsMarkingConversationRead(true)
      try {
        await sendAPI(new MarkConversationRead(activeConversationId, { mode: 'conversation' }))
        void refreshInbox()
        return await sendAPI(new GetConversationHistory(activeConversationId))
      } finally {
        isAutoMarkingConversationReadRef.current = false
        setIsMarkingConversationRead(false)
      }
    },
    [autoMarkMessageRead, refreshInbox],
  )

  const refreshConversationState = useCallback(
    async (
      activeConversationId: MessageConversationId,
      options: { autoMarkRead?: boolean } = {},
    ): Promise<void> => {
      const response = await sendAPI(new GetConversationHistory(activeConversationId))
      const syncedResponse = await syncConversationReadState(activeConversationId, response, options)
      setHistory(syncedResponse)
      setErrorMessage('')
      setSendErrorMessage('')
      void refreshInbox()
    },
    [refreshInbox, syncConversationReadState],
  )

  useEffect(() => {
    if (!session || !targetUsername) {
      return
    }
    const activeTargetUsername = targetUsername
    let cancelled = false

    async function loadConversation() {
      setIsLoading(true)
      setHistory(null)
      setErrorMessage('')
      setSendErrorMessage('')
      try {
        const conversation = await sendAPI(new CreateConversation({ targetUsername: activeTargetUsername }))
        if (cancelled) {
          return
        }
        const activeConversationId = conversation.id
        setConversationId(activeConversationId)
        await refreshConversationState(activeConversationId)
      } catch (error) {
        if (!cancelled) {
          setConversationId(null)
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
  }, [refreshConversationState, session, t, targetUsername])

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

      void refreshConversationState(activeConversationId).catch((error) => {
        setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
      })
    }

    window.addEventListener(messageStreamEventName, handleRealtimeEvent as EventListener)
    return () => window.removeEventListener(messageStreamEventName, handleRealtimeEvent as EventListener)
  }, [conversationId, refreshConversationState, t])

  const markWholeConversationRead = useCallback(async () => {
    if (!conversationId) {
      return
    }
    setIsMarkingConversationRead(true)
    try {
      await sendAPI(new MarkConversationRead(conversationId, { mode: 'conversation' }))
      const response = await sendAPI(new GetConversationHistory(conversationId))
      setHistory(response)
      setErrorMessage('')
      setSendErrorMessage('')
      void refreshInbox()
    } catch (error) {
      setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
    } finally {
      setIsMarkingConversationRead(false)
    }
  }, [conversationId, refreshInbox, t])

  const markSingleMessageRead = useCallback(
    async (messageId: MessageId) => {
      if (!conversationId) {
        return
      }
      setPendingReadMessageId(messageIdValue(messageId))
      try {
        await sendAPI(new MarkConversationRead(conversationId, { mode: 'message', messageId }))
        const response = await sendAPI(new GetConversationHistory(conversationId))
        setHistory(response)
        setErrorMessage('')
        setSendErrorMessage('')
        void refreshInbox()
      } catch (error) {
        setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.loadFailed'))
      } finally {
        setPendingReadMessageId(null)
      }
    },
    [conversationId, refreshInbox, t],
  )

  const submitDraft = useCallback(() => {
    if (!conversationId) {
      return
    }
    const validation = parseMessageContent(draft)
    if (!validation.ok) {
      setSendErrorMessage(validation.error)
      return
    }

    setIsSending(true)
    void sendAPI(new SendDirectMessage(conversationId, { content: validation.value }))
      .then(async () => {
        setDraft('')
        await sendAPI(new MarkConversationRead(conversationId, { mode: 'conversation' }))
        await refreshConversationState(conversationId, { autoMarkRead: false })
      })
      .catch((error) => {
        if (error instanceof HttpClientError && error.code === 'api.error.message.blocked_by_recipient') {
          setSendErrorMessage(error.message)
          return
        }
        setErrorMessage(error instanceof HttpClientError ? error.message : t('messages.sendFailed'))
      })
      .finally(() => setIsSending(false))
  }, [conversationId, draft, refreshConversationState, t])

  const loadOlderMessages = useCallback(() => {
    if (!conversationId || !history || history.messages.length === 0 || !history.hasMore) {
      return
    }
    setIsLoadingOlderMessages(true)
    setOlderMessagesError('')
    const earliestMessage = history.messages[0]
    void sendAPI(new GetConversationHistory(conversationId, { beforeMessageId: earliestMessage.id }))
      .then((response) => {
        setHistory((current) => {
          if (!current) {
            return response
          }
          return {
            ...response,
            messages: [...response.messages, ...current.messages],
          }
        })
      })
      .catch((error) => {
        setOlderMessagesError(error instanceof HttpClientError ? error.message : t('messages.loadOlderFailed'))
      })
      .finally(() => setIsLoadingOlderMessages(false))
  }, [conversationId, history, t])

  const updateDraft = useCallback(
    (value: string) => {
      setDraft(value)
      if (sendErrorMessage) {
        setSendErrorMessage('')
      }
    },
    [sendErrorMessage],
  )

  const handleDraftKeyDown = useCallback(
    (event: KeyboardEvent<HTMLTextAreaElement>) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' && !isSending) {
        event.preventDefault()
        submitDraft()
      }
    },
    [isSending, submitDraft],
  )

  return {
    autoMarkMessageRead,
    conversation: history?.conversation ?? null,
    draft,
    errorMessage,
    handleDraftKeyDown,
    hasUnreadMessages: (history?.conversation.unreadCount ?? 0) > 0,
    history,
    isLoading,
    isLoadingOlderMessages,
    isMarkingConversationRead,
    isSending,
    loadOlderMessages,
    markSingleMessageRead,
    markWholeConversationRead,
    olderMessagesError,
    pendingReadMessageId,
    sendErrorMessage,
    setDraft: updateDraft,
    showManageBlocksShortcut:
      !history?.facts.viewerHasSentMessage &&
      (history?.facts.otherParticipantMessageCount ?? 0) >= minimumIncomingMessagesBeforeBlockShortcut,
    submitDraft,
  }
}
