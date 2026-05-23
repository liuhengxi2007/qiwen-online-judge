import type { MessageBlockEntry } from '@/features/message/http/response/MessageBlockEntry'
import type { Username } from '@/features/user/model/Username'
import { fromMessageBlockEntry } from '@/features/message/http/codec'
import { usernameValue } from '@/features/user/lib/user-parsers'
import { postJson } from '@/shared/api/http-client'

export function addMessageBlock(targetUsername: Username): Promise<MessageBlockEntry> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`,
    fromMessageBlockEntry,
    {},
  )
}
