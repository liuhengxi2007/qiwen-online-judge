import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'

export type MessageInboxResponse = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
}
