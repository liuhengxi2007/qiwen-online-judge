import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageInboxResponse } from '@/objects/message/response/MessageInboxResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 查询私信收件箱；可选分页参数，输出会话摘要列表和总未读数。 */
export class ListInbox implements APIWithSessionMessage<MessageInboxResponse> {
  declare readonly responseType?: MessageInboxResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `messages/inbox${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
