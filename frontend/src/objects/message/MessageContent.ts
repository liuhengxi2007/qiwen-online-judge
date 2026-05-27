export type MessageContent = string & { readonly __brand: 'MessageContent' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const maxMessageLength = 5000

function createMessageContent(value: string): MessageContent {
  return value as MessageContent
}

export function parseMessageContent(rawContent: string): ParseResult<MessageContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Message content is required.' }
  }
  if (normalized.length > maxMessageLength) {
    return { ok: false, error: `Message content must be at most ${maxMessageLength} characters.` }
  }

  return { ok: true, value: createMessageContent(normalized) }
}

export function messageContentValue(content: MessageContent): string {
  return content
}

export function fromMessageContentContract(value: string, label: string): MessageContent {
  const result = parseMessageContent(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toMessageContentContract(value: MessageContent): string {
  return messageContentValue(value)
}
