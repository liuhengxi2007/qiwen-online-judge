import type { MessageInboxResponse } from '@/objects/message/response/MessageInboxResponse'
import { fromMessageInboxResponse } from '@/apis/message/codecs/MessageHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

export function listInbox(pageRequest?: PageRequest): Promise<MessageInboxResponse> {
  const url = new URL('/api/messages/inbox', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromMessageInboxResponse)
}
