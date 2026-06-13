import type { ConversationMessageFacts } from '@/objects/message/response/ConversationMessageFacts'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'

/** 私信历史响应；包含会话摘要、消息页、是否还有更多和会话事实。 */
export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}
