import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'

export class ListMessageBlocks implements APIWithSessionMessage<MessageBlockEntry[]> {
  declare readonly responseType?: MessageBlockEntry[]
  readonly method = 'GET'
  readonly apiPath = 'messages/blocks'

  body(): undefined {
    return undefined
  }
}
