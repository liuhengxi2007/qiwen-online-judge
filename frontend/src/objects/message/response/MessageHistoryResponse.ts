import type { ConversationMessageFacts } from '@/objects/message/response/ConversationMessageFacts'
import { fromConversationMessageFactsContract } from '@/objects/message/response/ConversationMessageFacts'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import { fromDirectMessageContract } from '@/objects/message/response/DirectMessage'
import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { fromMessageConversationSummaryContract } from '@/objects/message/response/MessageConversationSummary'
import { readArray, readBoolean, readRecord } from '@/objects/shared/PageResponse'

export type MessageHistoryResponse = {
  conversation: MessageConversationSummary
  messages: DirectMessage[]
  hasMore: boolean
  facts: ConversationMessageFacts
}

export function fromMessageHistoryResponseContract(value: unknown, label = 'message history response'): MessageHistoryResponse {
  const response = readRecord(value, label)
  return {
    conversation: fromMessageConversationSummaryContract(response.conversation, `${label} conversation`),
    messages: readArray(response.messages, `${label} messages`, fromDirectMessageContract),
    hasMore: readBoolean(response.hasMore, `${label} has more`),
    facts: fromConversationMessageFactsContract(response.facts, `${label} facts`),
  }
}
