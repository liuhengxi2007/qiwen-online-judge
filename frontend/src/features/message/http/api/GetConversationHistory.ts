import type {
  MessageConversationId,
  MessageHistoryResponse,
  MessageId,
} from '@/features/message/domain/message'
import {
  fromMessageHistoryResponse,
  messageConversationIdValue,
  messageIdValue,
} from '@/features/message/domain/message'
import { requestJson } from '@/shared/api/http-client'

export function getConversationHistory(
  conversationId: MessageConversationId,
  options: { beforeMessageId?: MessageId | null; limit?: number } = {},
): Promise<MessageHistoryResponse> {
  const url = new URL(`/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages`, window.location.origin)
  if (options.beforeMessageId) {
    url.searchParams.set('before', messageIdValue(options.beforeMessageId))
  }
  if (options.limit) {
    url.searchParams.set('limit', String(options.limit))
  }

  return requestJson(url.pathname + url.search, fromMessageHistoryResponse)
}
