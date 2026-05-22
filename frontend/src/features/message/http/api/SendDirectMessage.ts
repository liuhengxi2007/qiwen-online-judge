import type {
  DirectMessage,
  MessageConversationId,
  SendDirectMessageRequest,
} from '@/features/message/domain/message'
import { messageConversationIdValue } from '@/features/message/domain/message'
import {
  fromDirectMessage,
  toSendDirectMessageRequest,
} from '@/features/message/http/codec'
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
