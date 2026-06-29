import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ConversationMessageFacts } from '@/objects/message/response/ConversationMessageFacts'
import type { MarkConversationReadRequest } from '@/objects/message/request/MarkConversationReadRequest'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'

/** 标记私信会话已读；输入会话 ID 和模式化请求，输出更新后的会话事实。 */
export class MarkConversationRead implements APIWithSessionMessage<ConversationMessageFacts> {
  declare readonly responseType?: ConversationMessageFacts
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: MarkConversationReadRequest

  constructor(conversationId: MessageConversationId, request: MarkConversationReadRequest) {
    this.apiPath = `messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/mark-read`
    this.request = request
  }

  body(): MarkConversationReadRequest {
    return this.request
  }
}
