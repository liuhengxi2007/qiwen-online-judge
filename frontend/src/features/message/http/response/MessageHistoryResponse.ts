import type { ConversationMessageFacts } from '@/features/message/http/response/ConversationMessageFacts'
import type { DirectMessage } from '@/features/message/http/response/DirectMessage'
import type { MessageConversationSummary } from '@/features/message/http/response/MessageConversationSummary'

export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}
