import type { ConversationMessageFacts } from '@/features/message/model/response/ConversationMessageFacts'
import type { DirectMessage } from '@/features/message/model/response/DirectMessage'
import type { MessageConversationSummary } from '@/features/message/model/response/MessageConversationSummary'

export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}
