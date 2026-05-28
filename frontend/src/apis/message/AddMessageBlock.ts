import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class AddMessageBlock implements APIWithSessionMessage<MessageBlockEntry> {
  declare readonly responseType?: MessageBlockEntry
  readonly method = 'POST'
  readonly apiPath: string

  constructor(targetUsername: Username) {
    this.apiPath = `messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`
  }

  body(): undefined {
    return undefined
  }
}
