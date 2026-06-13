import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateConversationRequest } from '@/objects/message/request/CreateConversationRequest'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'

/** 创建或获取私信会话；输入目标用户，输出会话摘要。 */
export class CreateConversation implements APIWithSessionMessage<MessageConversationSummary> {
  declare readonly responseType?: MessageConversationSummary
  readonly method = 'POST'
  readonly apiPath = 'messages/conversations'
  private readonly request: CreateConversationRequest

  constructor(request: CreateConversationRequest) {
    this.request = request
  }

  body(): CreateConversationRequest {
    return this.request
  }
}
