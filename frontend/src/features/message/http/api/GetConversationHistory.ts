import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageHistoryResponse } from '@/features/message/http/response/MessageHistoryResponse'
import type { MessageId } from '@/features/message/model/MessageId'
import { messageConversationIdValue, messageIdValue } from '@/features/message/lib/message-parsers'
import { fromMessageHistoryResponse } from '@/features/message/http/codec'
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
