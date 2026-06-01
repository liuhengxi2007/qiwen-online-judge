export type ConversationMessageFacts = {
  viewerHasSentMessage: boolean
  otherParticipantMessageCount: number
}

export function fromConversationMessageFactsContract(
  value: unknown,
  label = 'conversation message facts',
): ConversationMessageFacts {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const facts = value as Record<string, unknown>
  if (typeof facts.viewerHasSentMessage !== 'boolean') {
    throw new Error(`Invalid ${label} viewer has sent message in contract payload: expected boolean.`)
  }
  if (typeof facts.otherParticipantMessageCount !== 'number' || !Number.isSafeInteger(facts.otherParticipantMessageCount)) {
    throw new Error(`Invalid ${label} other participant message count in contract payload: expected safe integer.`)
  }

  return {
    viewerHasSentMessage: facts.viewerHasSentMessage,
    otherParticipantMessageCount: facts.otherParticipantMessageCount,
  }
}
