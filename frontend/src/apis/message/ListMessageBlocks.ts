import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import { fromMessageBlockEntry } from '@/apis/message/codecs/MessageHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export function listMessageBlocks(): Promise<MessageBlockEntry[]> {
  return requestJson('/api/messages/blocks', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid message blocks payload.')
    }

    return value.map(fromMessageBlockEntry)
  })
}
