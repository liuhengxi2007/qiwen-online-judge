import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { SendDirectMessageRequest } from '@/objects/message/request/SendDirectMessageRequest'
import { messageConversationIdValue } from '@/objects/message/message-parsers'
import {
  fromDirectMessage,
  toSendDirectMessageRequest,
} from '@/apis/message/codecs/MessageHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
