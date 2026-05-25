import type { MarkConversationReadRequest } from '@/features/message/http/request/MarkConversationReadRequest'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageConversationSummary } from '@/features/message/http/response/MessageConversationSummary'
import { messageConversationIdValue } from '@/features/message/lib/message-parsers'
import {
  fromMessageConversationSummary,
  toMarkConversationReadRequest,
} from '@/features/message/http/codec/MessageHttpCodecs'
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
