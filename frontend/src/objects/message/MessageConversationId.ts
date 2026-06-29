/** 私信会话 UUID 品牌类型；用于读取历史、发送消息和标记已读。 */
export type MessageConversationId = string & { readonly __brand: 'MessageConversationId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** 创建会话 ID 品牌值；调用前必须完成 UUID 校验。 */
function createMessageConversationId(value: string): MessageConversationId {
  /** 注意：这里的 as 只在 parseMessageConversationId 校验通过后施加品牌类型。 */
  return value as MessageConversationId
}

/** 解析会话 ID；接受 UUID 字符串并返回结构化错误。 */
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

/** 将会话 ID 品牌值还原为字符串；用于 API path。 */
export function messageConversationIdValue(conversationId: MessageConversationId): string {
  return conversationId
}
