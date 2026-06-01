import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateConversationRequest } from '@/objects/message/request/CreateConversationRequest'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { fromMessageConversationSummaryContract } from '@/objects/message/response/MessageConversationSummary'

export class CreateConversation implements APIWithSessionMessage<MessageConversationSummary> {
  declare readonly responseType?: MessageConversationSummary
  readonly method = 'POST'
  readonly decode = (value: unknown) => fromMessageConversationSummaryContract(value, 'message conversation')
  readonly apiPath = 'messages/conversations'
  private readonly request: CreateConversationRequest

  constructor(request: CreateConversationRequest) {
    this.request = request
  }

  body(): CreateConversationRequest {
    return this.request
  }
}
