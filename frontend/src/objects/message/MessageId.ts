/** 私信消息 UUID 品牌类型；用于游标分页和按消息标记已读。 */
export type MessageId = string & { readonly __brand: 'MessageId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** 创建消息 ID 品牌值；调用前必须完成 UUID 校验。 */
function createMessageId(value: string): MessageId {
  /** 注意：这里的 as 只在 parseMessageId 校验通过后施加品牌类型。 */
  return value as MessageId
}

/** 解析消息 ID；接受 UUID 字符串并返回结构化错误。 */
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

/** 将消息 ID 品牌值还原为字符串；用于查询参数或 API body。 */
export function messageIdValue(messageId: MessageId): string {
  return messageId
}
