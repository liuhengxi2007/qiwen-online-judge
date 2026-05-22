import type {
  MessageBlockEntry,
  Username,
} from '@/features/message/domain/message'
import { fromMessageBlockEntry } from '@/features/message/domain/message'
import { usernameValue } from '@/features/auth/domain/auth'
import { postJson } from '@/shared/api/http-client'

export function addMessageBlock(targetUsername: Username): Promise<MessageBlockEntry> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}`,
    fromMessageBlockEntry,
    {},
  )
}
