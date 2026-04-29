import type { ConversationMessageFacts } from '@/features/message/model/ConversationMessageFacts'
import type { DirectMessage } from '@/features/message/model/DirectMessage'
import type { MessageConversationSummary } from '@/features/message/model/MessageConversationSummary'

export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}
