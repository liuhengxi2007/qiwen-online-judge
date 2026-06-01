import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageInboxResponse } from '@/objects/message/response/MessageInboxResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { fromMessageInboxResponseContract } from '@/objects/message/response/MessageInboxResponse'

export class ListInbox implements APIWithSessionMessage<MessageInboxResponse> {
  declare readonly responseType?: MessageInboxResponse
  readonly method = 'GET'
  readonly decode = fromMessageInboxResponseContract
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
