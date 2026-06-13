/** 用户显示名品牌类型；用于公开展示，必须非空且长度受限。 */
export type DisplayName = string & { readonly __brand: 'DisplayName' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建显示名品牌值；调用前必须完成非空和长度校验。 */
function createDisplayName(value: string): DisplayName {
  /** 注意：这里的 as 只在 parseDisplayName 校验通过后施加品牌类型。 */
  return value as DisplayName
}

/** 将显示名品牌值还原为普通字符串；无副作用。 */
export function displayNameValue(displayName: DisplayName): string {
  return displayName
}

/** 解析显示名输入；会去除首尾空白并返回结构化错误。 */
export function parseDisplayName(rawDisplayName: string): ParseResult<DisplayName> {
  const normalized = rawDisplayName.trim()

  if (!normalized) {
    return { ok: false, error: 'Display name is required.' }
  }

  if (normalized.length > 120) {
    return { ok: false, error: 'Display name must be at most 120 characters.' }
  }

  return { ok: true, value: createDisplayName(normalized) }
}
