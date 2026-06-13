/** 访问策略中的用户组 slug；独立于用户组领域类型，避免跨边界误用。 */
export type AccessUserGroupSlug = string & { readonly __brand: 'AccessUserGroupSlug' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const userGroupSlugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

/** 将访问策略用户组 slug 还原为 URL/API 可传输字符串；无副作用。 */
export function accessUserGroupSlugValue(slug: AccessUserGroupSlug): string {
  return slug
}

/** 解析并校验访问策略用户组 slug；返回结构化失败原因，不抛异常。 */
export function parseAccessUserGroupSlug(rawSlug: string): ParseResult<AccessUserGroupSlug> {
  const normalized = rawSlug.trim()

  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }

  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }

  if (!userGroupSlugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  /** 注意：这里的 as 只在完成 slug 格式校验后施加品牌类型，不改变运行时值。 */
  return { ok: true, value: normalized as AccessUserGroupSlug }
}
