import type { MessageContent } from '@/objects/message/MessageContent'

/** 发送私信请求体；目标会话由 API path 指定。 */
export type SendDirectMessageRequest = {
  content: MessageContent
}
