import type {
  MarkConversationReadRequest,
  MessageConversationId,
  MessageConversationSummary,
} from '@/features/message/domain/message'
import {
  fromMessageConversationSummary,
  messageConversationIdValue,
  toMarkConversationReadRequest,
} from '@/features/message/domain/message'
import { postJson } from '@/shared/api/http-client'

export function markConversationRead(
  conversationId: MessageConversationId,
  request: MarkConversationReadRequest,
): Promise<MessageConversationSummary> {
  return postJson(
    `/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/read`,
    fromMessageConversationSummary,
    toMarkConversationReadRequest(request),
  )
}
