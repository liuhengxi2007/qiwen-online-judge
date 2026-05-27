import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import type { MessageId } from '@/objects/message/MessageId'
import { messageIdValue } from '@/objects/message/MessageId'
import { fromMessageHistoryResponse } from '@/apis/message/codecs/MessageHttpCodecs'
import { requestJson } from '@/system/api/http-client'

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
