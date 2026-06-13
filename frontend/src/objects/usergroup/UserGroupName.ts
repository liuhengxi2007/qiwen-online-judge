/** 用户组名称品牌类型；用于列表、详情和编辑表单展示。 */
export type UserGroupName = string & { readonly __brand: 'UserGroupName' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建用户组名称品牌值；调用前必须完成非空和长度校验。 */
function createUserGroupName(value: string): UserGroupName {
  /** 注意：这里的 as 只在 parseUserGroupName 校验通过后施加品牌类型。 */
  return value as UserGroupName
}

/** 将用户组名称品牌值还原为字符串；无副作用。 */
export function userGroupNameValue(name: UserGroupName): string {
  return name
}

/** 解析用户组名称；去除首尾空白并返回结构化校验结果。 */
export function parseUserGroupName(rawName: string): ParseResult<UserGroupName> {
  const normalized = rawName.trim()
  if (!normalized) {
    return { ok: false, error: 'User group name is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'User group name must be at most 120 characters.' }
  }

  return { ok: true, value: createUserGroupName(normalized) }
}
