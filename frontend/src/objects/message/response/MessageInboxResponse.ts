import type { MessageConversationSummary } from '@/objects/message/response/MessageConversationSummary'
import { fromMessageConversationSummaryContract } from '@/objects/message/response/MessageConversationSummary'
import { readArray, readNonNegativeSafeInteger, readPositiveSafeInteger, readRecord } from '@/objects/shared/PageResponse'

export type MessageInboxResponse = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
}

export function fromMessageInboxResponseContract(value: unknown, label = 'message inbox response'): MessageInboxResponse {
  const response = readRecord(value, label)
  return {
    conversations: readArray(response.conversations, `${label} conversations`, fromMessageConversationSummaryContract),
    totalUnreadCount: readNonNegativeSafeInteger(response.totalUnreadCount, `${label} total unread count`),
    page: readPositiveSafeInteger(response.page, `${label} page`),
    pageSize: readPositiveSafeInteger(response.pageSize, `${label} page size`),
    totalItems: readNonNegativeSafeInteger(response.totalItems, `${label} total items`),
  }
}
