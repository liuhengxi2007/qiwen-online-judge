import type { MessageConversationSummary } from '@/features/message/model/MessageConversationSummary'

export type MessageInboxResponse = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
}
