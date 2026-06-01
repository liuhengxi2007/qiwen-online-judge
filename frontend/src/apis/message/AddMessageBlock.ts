import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromMessageBlockEntryContract } from '@/objects/message/response/MessageBlockEntry'

export class AddMessageBlock implements APIWithSessionMessage<MessageBlockEntry> {
  declare readonly responseType?: MessageBlockEntry
  readonly method = 'POST'
  readonly decode = (value: unknown) => fromMessageBlockEntryContract(value, 'message block entry')
  readonly apiPath: string

  constructor(targetUsername: Username) {
    this.apiPath = `messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`
  }

  body(): undefined {
    return undefined
  }
}
