import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import type { SendDirectMessageRequest } from '@/objects/message/request/SendDirectMessageRequest'

/** 发送私信；输入会话 ID 和消息内容，输出创建后的消息。 */
export class SendDirectMessage implements APIWithSessionMessage<DirectMessage> {
  declare readonly responseType?: DirectMessage
  readonly method = 'POST'
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
