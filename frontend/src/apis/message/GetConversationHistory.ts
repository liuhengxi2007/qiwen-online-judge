import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { messageConversationIdValue } from '@/objects/message/MessageConversationId'
import type { MessageHistoryResponse } from '@/objects/message/response/MessageHistoryResponse'
import type { MessageId } from '@/objects/message/MessageId'
import { messageIdValue } from '@/objects/message/MessageId'

export class GetConversationHistory implements APIWithSessionMessage<MessageHistoryResponse> {
  declare readonly responseType?: MessageHistoryResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(conversationId: MessageConversationId, options: { beforeMessageId?: MessageId | null; limit?: number } = {}) {
    const params = new URLSearchParams()
    if (options.beforeMessageId) {
      params.set('before', messageIdValue(options.beforeMessageId))
    }
    if (options.limit !== undefined) {
      params.set('limit', String(options.limit))
    }
    this.apiPath = `messages/conversations/${encodeURIComponent(messageConversationIdValue(conversationId))}/messages${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
