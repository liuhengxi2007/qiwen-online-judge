export type MessageId = string & { readonly __brand: 'MessageId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createMessageId(value: string): MessageId {
  return value as MessageId
}

export function parseMessageId(rawId: string): ParseResult<MessageId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Message id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Message id must be a valid UUID.' }
  }

  return { ok: true, value: createMessageId(normalized) }
}

export function messageIdValue(messageId: MessageId): string {
  return messageId
}

export function fromMessageIdContract(value: string, label: string): MessageId {
  const result = parseMessageId(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toMessageIdContract(value: MessageId): string {
  return messageIdValue(value)
}
