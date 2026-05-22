import type { MessageBlockEntry } from '@/features/message/domain/message'
import { fromMessageBlockEntry } from '@/features/message/http/codec'
import { requestJson } from '@/shared/api/http-client'

export function listMessageBlocks(): Promise<MessageBlockEntry[]> {
  return requestJson('/api/messages/blocks', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid message blocks payload.')
    }

    return value.map(fromMessageBlockEntry)
  })
}
