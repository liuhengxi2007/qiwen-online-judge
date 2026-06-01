export type MessageConversationId = string & { readonly __brand: 'MessageConversationId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createMessageConversationId(value: string): MessageConversationId {
  return value as MessageConversationId
}

export function parseMessageConversationId(rawId: string): ParseResult<MessageConversationId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Conversation id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Conversation id must be a valid UUID.' }
  }

  return { ok: true, value: createMessageConversationId(normalized) }
}

export function messageConversationIdValue(conversationId: MessageConversationId): string {
  return conversationId
}