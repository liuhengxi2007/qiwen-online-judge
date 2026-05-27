import type { MessageContent } from '@/objects/message/MessageContent'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { MessageId } from '@/objects/message/MessageId'
import {
  messageContentValue,
  messageIdValue,
  parseMessageContent,
  parseMessageConversationId,
  parseMessageId,
} from '@/objects/message/message-parsers'
import { requireParsed } from '@/objects/user/user-parsers'

export type MessageIdContract = string
export type MessageConversationIdContract = string
export type MessageContentContract = string

export function fromMessageIdContract(value: MessageIdContract, label: string): MessageId {
  return requireParsed(parseMessageId(value), label)
}

export function toMessageIdContract(value: MessageId): MessageIdContract {
  return messageIdValue(value)
}

export function fromMessageConversationIdContract(
  value: MessageConversationIdContract,
  label: string,
): MessageConversationId {
  return requireParsed(parseMessageConversationId(value), label)
}

export function fromMessageContentContract(value: MessageContentContract, label: string): MessageContent {
  return requireParsed(parseMessageContent(value), label)
}

export function toMessageContentContract(value: MessageContent): MessageContentContract {
  return messageContentValue(value)
}
