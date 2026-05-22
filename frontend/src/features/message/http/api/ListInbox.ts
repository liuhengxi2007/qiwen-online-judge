import type { MessageInboxResponse } from '@/features/message/domain/message'
import { fromMessageInboxResponse } from '@/features/message/domain/message'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

export function listInbox(pageRequest?: PageRequest): Promise<MessageInboxResponse> {
  const url = new URL('/api/messages/inbox', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromMessageInboxResponse)
}
