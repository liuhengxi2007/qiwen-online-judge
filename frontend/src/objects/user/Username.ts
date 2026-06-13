/** 用户名品牌类型；作为账号、作者、消息对象的稳定标识。 */
export type Username = string & { readonly __brand: 'Username' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const usernamePattern = /^[a-z0-9_-]+$/

/** 创建用户名品牌值；调用前必须完成小写化、长度和字符校验。 */
function createUsername(value: string): Username {
  /** 注意：这里的 as 只在 parseUsername 校验通过后施加品牌类型。 */
  return value as Username
}

/** 将用户名品牌值还原为普通字符串；用于 URL path 和 API body。 */
export function usernameValue(username: Username): string {
  return username
}

/** 解析用户名输入；会去除空白、转小写并返回结构化校验结果。 */
export function parseUsername(rawUsername: string): ParseResult<Username> {
  const normalized = rawUsername.trim().toLowerCase()

  if (!normalized) {
    return { ok: false, error: 'Username is required.' }
  }

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  return { ok: true, value: createUsername(normalized) }
}
