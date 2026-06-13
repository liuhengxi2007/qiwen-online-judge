import { useEffect } from 'react'

import { SubscribeMessageEvents } from '@/apis/message/SubscribeMessageEvents'
import { parseDisplayName } from '@/objects/user/DisplayName'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageContent } from '@/objects/message/MessageContent'
import type { MessageId } from '@/objects/message/MessageId'
import { parseMessageId } from '@/objects/message/MessageId'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import { useMessageInboxRefresh } from '@/pages/hooks/useMessageInboxRefresh'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { parseUsername } from '@/objects/user/Username'

/**
 * 浏览器全局消息流事件名，用于会话页等组件监听 SSE 解码后的领域事件。
 */
export const messageStreamEventName = 'qiwen:message-stream-event'

/**
 * 消息 SSE 解码后的事件详情，覆盖新消息、会话已读和收件箱变更。
 */
export type MessageStreamEventDetail =
  | { type: 'message_received'; payload: DirectMessage }
  | { type: 'conversation_read'; payload: ConversationReadStreamPayload }
  | { type: 'inbox_changed'; payload: Record<string, never> }

/**
 * 会话已读事件载荷，描述读者和已读到的消息边界。
 */
type ConversationReadStreamPayload = {
  conversationId: MessageConversationId
  readUpToMessageId: MessageId
  readerUsername: Username
}

// 注意：消息 SSE 使用模块级单例和订阅计数，避免多个页面/组件同时建立重复 EventSource。
let eventSource: EventSource | null = null
let subscriberCount = 0

/**
 * 将解码后的消息流事件分发到 window，供不直接持有 EventSource 的组件订阅。
 */
function dispatchMessageStreamEvent(detail: MessageStreamEventDetail) {
  window.dispatchEvent(new CustomEvent<MessageStreamEventDetail>(messageStreamEventName, { detail }))
}

/**
 * 将未知 SSE 载荷收窄为对象；字段合法性由后续读取函数负责。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

/**
 * 从未知载荷读取字符串字段；字段缺失或类型错误时抛出可定位的解码错误。
 */
function readString(value: unknown, field: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

/**
 * 将领域 parse 结果转为值或抛错，统一 SSE 解码失败路径。
 */
function requireParsed<T>(result: { ok: true; value: T } | { ok: false; error: string }, field: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${field}: ${result.error}`)
  }

  return result.value
}

/**
 * 从 SSE 载荷读取用户身份，并校验用户名和显示名的领域格式。
 */
function readUserIdentity(value: unknown, field: string): UserIdentity {
  if (!isRecord(value)) {
    throw new Error(`Invalid ${field}.`)
  }

  return {
    username: requireParsed(parseUsername(readString(value.username, `${field} username`)), `${field} username`),
    displayName: requireParsed(parseDisplayName(readString(value.displayName, `${field} display name`)), `${field} display name`),
  }
}

/**
 * 解码新私信事件载荷；输入来自 SSE JSON，字段不合法时抛出解码错误。
 */
function decodeDirectMessage(value: unknown): DirectMessage {
  if (!isRecord(value)) {
    throw new Error('Invalid direct message payload.')
  }

  return {
    id: requireParsed(parseMessageId(readString(value.id, 'message id')), 'message id'),
    conversationId: requireParsed(parseMessageConversationId(readString(value.conversationId, 'conversation id')), 'conversation id'),
    sender: readUserIdentity(value.sender, 'message sender'),
    recipientUsername: requireParsed(parseUsername(readString(value.recipientUsername, 'recipient username')), 'recipient username'),
    content: requireParsed(parseMessageContent(readString(value.content, 'message content')), 'message content'),
    createdAt: readString(value.createdAt, 'message created at'),
    readAt: value.readAt === null ? null : readString(value.readAt, 'message read at'),
  }
}

/**
 * 解码会话已读事件载荷，校验会话 ID、消息 ID 和读者用户名。
 */
function decodeConversationReadStreamPayload(value: unknown): ConversationReadStreamPayload {
  if (!isRecord(value)) {
    throw new Error('Invalid conversation read event payload.')
  }

  return {
    conversationId: requireParsed(
      parseMessageConversationId(readString(value.conversationId, 'conversation read conversation id')),
      'conversation read conversation id',
    ),
    readUpToMessageId: requireParsed(
      parseMessageId(readString(value.readUpToMessageId, 'conversation read message id')),
      'conversation read message id',
    ),
    readerUsername: requireParsed(parseUsername(readString(value.readerUsername, 'conversation read reader username')), 'conversation read reader username'),
  }
}

/**
 * 解码收件箱变更事件；当前只校验对象形态，事件本身不携带字段。
 */
function decodeInboxChangedStreamPayload(value: unknown): Record<string, never> {
  if (!isRecord(value)) {
    throw new Error('Invalid inbox changed event payload.')
  }

  return {}
}

/**
 * 根据 SSE 事件类型解码原始 JSON 字符串；解码失败时记录错误并返回 null。
 */
function decodeMessageStreamEvent(type: 'message_received' | 'conversation_read' | 'inbox_changed', rawData: string): MessageStreamEventDetail | null {
  try {
    const parsed = JSON.parse(rawData) as unknown
    switch (type) {
      case 'message_received':
        return { type, payload: decodeDirectMessage(parsed) }
      case 'conversation_read':
        return { type, payload: decodeConversationReadStreamPayload(parsed) }
      case 'inbox_changed':
        return { type, payload: decodeInboxChangedStreamPayload(parsed) }
    }
  } catch (error) {
    console.error(`Failed to decode ${type} event.`, error)
    return null
  }
}

/**
 * 处理单条 SSE 事件，成功解码后刷新收件箱并分发浏览器自定义事件。
 */
function handleIncomingEvent(type: 'message_received' | 'conversation_read' | 'inbox_changed', event: Event, refreshInbox: () => Promise<void>) {
  const decoded = decodeMessageStreamEvent(type, (event as MessageEvent).data)
  if (!decoded) {
    return
  }

  void refreshInbox()
  dispatchMessageStreamEvent(decoded)
}

/**
 * 确保消息 SSE 连接已建立，并为三类消息事件注册处理器。
 */
function ensureEventSource(refreshInbox: () => Promise<void>) {
  if (eventSource) {
    return
  }

  eventSource = new EventSource(new SubscribeMessageEvents().eventUrl(), { withCredentials: true })

  eventSource.addEventListener('message_received', (event) => {
    handleIncomingEvent('message_received', event, refreshInbox)
  })

  eventSource.addEventListener('conversation_read', (event) => {
    handleIncomingEvent('conversation_read', event, refreshInbox)
  })

  eventSource.addEventListener('inbox_changed', (event) => {
    handleIncomingEvent('inbox_changed', event, refreshInbox)
  })
}

/**
 * 在订阅者归零后关闭消息 SSE 连接，避免多页面实例重复连接。
 */
function releaseEventSource() {
  if (subscriberCount <= 0 && eventSource) {
    eventSource.close()
    eventSource = null
  }
}

/**
 * 管理消息实时连接生命周期；登录时订阅，退出时清空收件箱并关闭全局 EventSource。
 */
export function useMessageRealtimeConnection() {
  const session = useAuthStore((state) => state.session)
  const refreshInbox = useMessageInboxRefresh()
  const hasLoadedInbox = useMessageStore((state) => state.hasLoadedInbox)
  const clear = useMessageStore((state) => state.clear)

  useEffect(() => {
    if (!session) {
      clear()
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      subscriberCount = 0
      return
    }

    const isFirstSubscriber = subscriberCount === 0
    subscriberCount += 1
    ensureEventSource(refreshInbox)
    if (isFirstSubscriber && !hasLoadedInbox) {
      void refreshInbox()
    }

    return () => {
      subscriberCount -= 1
      releaseEventSource()
    }
  }, [clear, hasLoadedInbox, refreshInbox, session])
}
