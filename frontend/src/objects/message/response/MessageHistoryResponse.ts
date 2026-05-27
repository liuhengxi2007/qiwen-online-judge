import type { ConversationMessageFacts } from '@/objects/message/response/ConversationMessageFacts'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'

export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}
