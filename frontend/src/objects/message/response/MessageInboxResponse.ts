import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'

/** 私信收件箱响应；包含会话分页和总未读数。 */
export type MessageInboxResponse = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
}
