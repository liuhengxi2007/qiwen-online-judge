import { describe, expect, it } from 'vitest'

import { messageContentValue, parseMessageContent } from '@/objects/message/MessageContent'
import { messageConversationIdValue, parseMessageConversationId } from '@/objects/message/MessageConversationId'
import { messageIdValue, parseMessageId } from '@/objects/message/MessageId'

const conversationId = '11111111-1111-4111-8111-111111111111'
const messageId = '22222222-2222-4222-8222-222222222222'

describe('message-parsers', () => {
  it('parses message conversation ids', () => {
    const parsed = parseMessageConversationId(` ${conversationId} `)
    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(messageConversationIdValue(parsed.value)).toBe(conversationId)
    }
  })

  it('rejects invalid message conversation ids', () => {
    expect(parseMessageConversationId('   ')).toEqual({
      ok: false,
      error: 'Conversation id is required.',
    })
    expect(parseMessageConversationId('not-a-uuid')).toEqual({
      ok: false,
      error: 'Conversation id must be a valid UUID.',
    })
  })

  it('parses message ids', () => {
    const parsed = parseMessageId(` ${messageId} `)
    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(messageIdValue(parsed.value)).toBe(messageId)
    }
  })

  it('rejects invalid message ids', () => {
    expect(parseMessageId('')).toEqual({
      ok: false,
      error: 'Message id is required.',
    })
    expect(parseMessageId('bad-id')).toEqual({
      ok: false,
      error: 'Message id must be a valid UUID.',
    })
  })

  it('parses message content and trims it', () => {
    const parsed = parseMessageContent('  hello world  ')
    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(messageContentValue(parsed.value)).toBe('hello world')
    }
  })

  it('rejects invalid message content', () => {
    expect(parseMessageContent('   ')).toEqual({
      ok: false,
      error: 'Message content is required.',
    })
    expect(parseMessageContent('x'.repeat(5001))).toEqual({
      ok: false,
      error: 'Message content must be at most 5000 characters.',
    })
  })
})
