import { describe, expect, it } from 'vitest'

import {
  fromDirectMessage,
  fromMessageConversationSummary,
  fromMessageHistoryResponse,
  fromMessageInboxResponse,
  toMarkConversationReadRequest,
} from '@/features/message/http/codec'
import {
  messageContentValue,
  messageConversationIdValue,
  messageIdValue,
} from '@/features/message/lib/message-parsers'
import type { MessageId as MessageIdValue } from '@/features/message/model/MessageId'

const conversationId = '11111111-1111-4111-8111-111111111111'
const messageId = '22222222-2222-4222-8222-222222222222'

describe('message codec', () => {
  it('maps a conversation summary contract payload', () => {
    const summary = fromMessageConversationSummary({
      id: conversationId,
      otherUser: {
        username: 'alice',
        displayName: 'Alice',
      },
      lastMessagePreview: 'Last message',
      lastMessageSenderUsername: 'alice',
      lastMessageAt: '2026-04-29T12:00:00Z',
      unreadCount: 3,
    })

    expect(messageConversationIdValue(summary.id)).toBe(conversationId)
    expect(summary.otherUser.username).toBe('alice')
    expect(summary.otherUser.displayName).toBe('Alice')
    expect(summary.lastMessagePreview).toBe('Last message')
    expect(summary.lastMessageSenderUsername).toBe('alice')
    expect(summary.lastMessageAt).toBe('2026-04-29T12:00:00Z')
    expect(summary.unreadCount).toBe(3)
  })

  it('throws for invalid conversation summary payloads', () => {
    expect(() => fromMessageConversationSummary(null)).toThrow('Invalid message conversation payload.')
    expect(() =>
      fromMessageConversationSummary({
        id: conversationId,
        otherUser: {
          username: 'alice',
          displayName: 'Alice',
        },
        lastMessagePreview: null,
        lastMessageSenderUsername: null,
        lastMessageAt: '2026-04-29T12:00:00Z',
        unreadCount: '3',
      }),
    ).toThrow('Invalid unread count.')
  })

  it('maps a direct message contract payload', () => {
    const message = fromDirectMessage({
      id: messageId,
      conversationId,
      sender: {
        username: 'alice',
        displayName: 'Alice',
      },
      recipientUsername: 'bob',
      content: '  hi there  ',
      createdAt: '2026-04-29T12:00:00Z',
      readAt: null,
    })

    expect(messageIdValue(message.id)).toBe(messageId)
    expect(messageConversationIdValue(message.conversationId)).toBe(conversationId)
    expect(message.sender.username).toBe('alice')
    expect(message.recipientUsername).toBe('bob')
    expect(messageContentValue(message.content)).toBe('hi there')
    expect(message.createdAt).toBe('2026-04-29T12:00:00Z')
    expect(message.readAt).toBeNull()
  })

  it('throws for invalid direct message payloads', () => {
    expect(() =>
      fromDirectMessage({
        id: messageId,
        conversationId,
        sender: {
          username: 'alice',
          displayName: 'Alice',
        },
        recipientUsername: 'bob',
        content: '   ',
        createdAt: '2026-04-29T12:00:00Z',
        readAt: null,
      }),
    ).toThrow('Invalid message content in contract payload: Message content is required.')
  })

  it('maps a message inbox contract payload', () => {
    const inbox = fromMessageInboxResponse({
      conversations: [
        {
          id: conversationId,
          otherUser: {
            username: 'alice',
            displayName: 'Alice',
          },
          lastMessagePreview: null,
          lastMessageSenderUsername: null,
          lastMessageAt: '2026-04-29T12:00:00Z',
          unreadCount: 1,
        },
      ],
      totalUnreadCount: 1,
      page: 2,
      pageSize: 10,
      totalItems: 11,
    })

    expect(inbox.conversations).toHaveLength(1)
    expect(messageConversationIdValue(inbox.conversations[0].id)).toBe(conversationId)
    expect(inbox.page).toBe(2)
    expect(inbox.pageSize).toBe(10)
    expect(inbox.totalItems).toBe(11)
    expect(inbox.totalUnreadCount).toBe(1)
  })

  it('throws for invalid message inbox payloads', () => {
    expect(() => fromMessageInboxResponse({ conversations: 'not-an-array', totalUnreadCount: 1 })).toThrow(
      'Invalid message inbox payload.',
    )
  })

  it('maps a message history payload with conversation facts', () => {
    const history = fromMessageHistoryResponse({
      conversation: {
        id: conversationId,
        otherUser: {
          username: 'alice',
          displayName: 'Alice',
        },
        lastMessagePreview: null,
        lastMessageSenderUsername: null,
        lastMessageAt: '2026-04-29T12:00:00Z',
        unreadCount: 0,
      },
      messages: [
        {
          id: messageId,
          conversationId,
          sender: {
            username: 'alice',
            displayName: 'Alice',
          },
          recipientUsername: 'bob',
          content: 'hello',
          createdAt: '2026-04-29T12:00:00Z',
          readAt: null,
        },
      ],
      hasMore: false,
      facts: {
        viewerHasSentMessage: false,
        otherParticipantMessageCount: 6,
      },
    })

    expect(history.facts.viewerHasSentMessage).toBe(false)
    expect(history.facts.otherParticipantMessageCount).toBe(6)
  })

  it('maps mark conversation read requests for both supported modes', () => {
    expect(toMarkConversationReadRequest({ mode: 'conversation' })).toEqual({
      mode: 'conversation',
    })
    expect(toMarkConversationReadRequest({ mode: 'message', messageId: messageId as MessageIdValue })).toEqual({
      mode: 'message',
      messageId,
    })
  })
})
