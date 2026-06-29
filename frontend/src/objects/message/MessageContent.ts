/** 私信正文品牌类型；用于发送消息请求和消息展示。 */
export type MessageContent = string & { readonly __brand: 'MessageContent' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const maxMessageLength = 5000

/** 创建私信正文品牌值；调用前必须完成非空和长度校验。 */
function createMessageContent(value: string): MessageContent {
  /** 注意：这里的 as 只在 parseMessageContent 校验通过后施加品牌类型。 */
  return value as MessageContent
}

/** 解析私信正文；去除首尾空白并限制最大长度。 */
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

/** 将私信正文品牌值还原为字符串；用于 API body 和展示。 */
export function messageContentValue(content: MessageContent): string {
  return content
}
