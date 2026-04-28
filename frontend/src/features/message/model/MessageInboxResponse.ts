import type { MessageConversationSummary } from '@/features/message/model/MessageConversationSummary'

export type MessageInboxResponse = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
}
