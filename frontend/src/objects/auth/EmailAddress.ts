/** 账号邮箱地址品牌类型；只能通过解析 helper 从用户输入构造。 */
export type EmailAddress = string & { readonly __brand: 'EmailAddress' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** 创建邮箱地址品牌值；调用前必须完成格式和长度校验。 */
function createEmailAddress(value: string): EmailAddress {
  /** 注意：这里的 as 只在 parseEmailAddress 校验通过后施加品牌类型。 */
  return value as EmailAddress
}

/** 将邮箱品牌值还原为普通字符串；用于 API body 和展示。 */
export function emailAddressValue(emailAddress: EmailAddress): string {
  return emailAddress
}

/** 解析邮箱输入；会去除首尾空白并返回结构化错误，不抛异常。 */
export function parseEmailAddress(rawEmailAddress: string): ParseResult<EmailAddress> {
  const normalized = rawEmailAddress.trim()

  if (!normalized) {
    return { ok: false, error: 'Email is required.' }
  }

  if (normalized.length > 255) {
    return { ok: false, error: 'Email must be at most 255 characters.' }
  }

  if (!emailPattern.test(normalized)) {
    return { ok: false, error: 'Please enter a valid email address.' }
  }

  return { ok: true, value: createEmailAddress(normalized) }
}
