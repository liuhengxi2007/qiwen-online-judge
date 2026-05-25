import type { MessageContent } from '@/features/message/model/MessageContent'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageId } from '@/features/message/model/MessageId'
import {
  messageContentValue,
  messageIdValue,
  parseMessageContent,
  parseMessageConversationId,
  parseMessageId,
} from '@/features/message/lib/message-parsers'
import { requireParsed } from '@/features/user/lib/user-parsers'

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
