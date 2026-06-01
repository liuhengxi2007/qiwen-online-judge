import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import type { SendDirectMessageRequest } from '@/objects/message/request/SendDirectMessageRequest'
import { fromDirectMessageContract } from '@/objects/message/response/DirectMessage'

export class SendDirectMessage implements APIWithSessionMessage<DirectMessage> {
  declare readonly responseType?: DirectMessage
  readonly method = 'POST'
  readonly decode = (value: unknown) => fromDirectMessageContract(value, 'direct message')
  readonly apiPath: string
  private readonly request: SendDirectMessageRequest

  constructor(conversationId: MessageConversationId, request: SendDirectMessageRequest) {
    this.apiPath = `messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages`
    this.request = request
  }

  body(): SendDirectMessageRequest {
    return this.request
  }
}
