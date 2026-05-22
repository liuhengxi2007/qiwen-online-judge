import type {
  DirectMessage,
  MessageConversationId,
  SendDirectMessageRequest,
} from '@/features/message/domain/message'
import {
  fromDirectMessage,
  messageConversationIdValue,
  toSendDirectMessageRequest,
} from '@/features/message/domain/message'
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
