import { describe, expect, it } from 'vitest'

import { parseUsername } from '@/objects/user/Username'
import { messageConversationPath } from './message-paths'

function requireParsed<T>(result: { ok: true; value: T } | { ok: false; error: string }): T {
  if (!result.ok) {
    throw new Error(result.error)
  }
  return result.value
}

describe('message paths', () => {
  it('builds username-based conversation paths', () => {
    expect(messageConversationPath(requireParsed(parseUsername('alice')))).toBe('/messages/with/alice')
    expect(messageConversationPath(requireParsed(parseUsername('alice-bob')))).toBe('/messages/with/alice-bob')
  })
})
