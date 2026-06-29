/** 用户组公开 slug 品牌类型；用于 URL、访问策略和 API 路径。 */
export type UserGroupSlug = string & { readonly __brand: 'UserGroupSlug' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

/** 创建用户组 slug 品牌值；调用前必须完成格式和长度校验。 */
function createUserGroupSlug(value: string): UserGroupSlug {
  /** 注意：这里的 as 只在 parseUserGroupSlug 校验通过后施加品牌类型。 */
  return value as UserGroupSlug
}

/** 将用户组 slug 还原为字符串；slug 语法已限制为 URL 安全字符，调用方可按路径或 query 场景继续编码。 */
export function userGroupSlugValue(slug: UserGroupSlug): string {
  return slug
}

/** 解析用户组 slug；拒绝空值、过短/过长值和非法字符。 */
export function parseUserGroupSlug(rawSlug: string): ParseResult<UserGroupSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'User group slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'User group slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'User group slug may contain only lowercase letters, numbers, and hyphens.' }
  }

  return { ok: true, value: createUserGroupSlug(normalized) }
}
