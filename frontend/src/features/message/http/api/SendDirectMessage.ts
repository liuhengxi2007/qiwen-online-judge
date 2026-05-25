import type { DirectMessage } from '@/features/message/model/response/DirectMessage'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { SendDirectMessageRequest } from '@/features/message/model/request/SendDirectMessageRequest'
import { messageConversationIdValue } from '@/features/message/lib/message-parsers'
import {
  fromDirectMessage,
  toSendDirectMessageRequest,
} from '@/features/message/http/codec/MessageHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function sendDirectMessage(
  conversationId: MessageConversationId,
  request: SendDirectMessageRequest,
): Promise<DirectMessage> {
  return postJson(
    `/api/messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages`,
    fromDirectMessage,
    toSendDirectMessageRequest(request),
  )
}
