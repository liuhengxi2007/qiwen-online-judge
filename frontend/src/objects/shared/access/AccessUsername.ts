/** 访问策略中的用户名；与用户领域用户名保持同格式但使用独立品牌隔离授权边界。 */
export type AccessUsername = string & { readonly __brand: 'AccessUsername' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const usernamePattern = /^[a-z0-9_-]+$/

/** 将访问策略用户名还原为 API 字符串；无副作用。 */
export function accessUsernameValue(username: AccessUsername): string {
  return username
}

/** 解析访问策略用户名并规范化为小写；失败时返回可展示错误。 */
export function parseAccessUsername(rawUsername: string): ParseResult<AccessUsername> {
  const normalized = rawUsername.trim().toLowerCase()

  if (normalized.length < 3 || normalized.length > 32) {
    return { ok: false, error: 'Username must be between 3 and 32 characters.' }
  }

  if (!usernamePattern.test(normalized)) {
    return { ok: false, error: 'Username may contain only lowercase letters, numbers, underscores, and hyphens.' }
  }

  /** 注意：这里的 as 只在完成用户名格式校验后施加品牌类型，不改变运行时值。 */
  return { ok: true, value: normalized as AccessUsername }
}
