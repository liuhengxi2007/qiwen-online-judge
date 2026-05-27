import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import type { Username } from '@/objects/user/Username'
import { fromMessageBlockEntry } from '@/apis/message/codecs/MessageHttpCodecs'
import { usernameValue } from '@/objects/user/Username'
import { postJson } from '@/system/api/http-client'

export function addMessageBlock(targetUsername: Username): Promise<MessageBlockEntry> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`,
    fromMessageBlockEntry,
    {},
  )
}
