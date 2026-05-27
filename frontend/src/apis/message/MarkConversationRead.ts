import type { MarkConversationReadRequest } from '@/objects/message/request/MarkConversationReadRequest'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { messageConversationIdValue } from '@/objects/message/message-parsers'
import {
  fromMessageConversationSummary,
  toMarkConversationReadRequest,
} from '@/apis/message/codecs/MessageHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
