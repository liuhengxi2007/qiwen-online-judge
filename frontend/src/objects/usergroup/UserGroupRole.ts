/** 用户组成员角色；owner 为所有者，manager 可管理成员，member 为普通成员。 */
export type UserGroupRole = 'owner' | 'manager' | 'member'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 解析用户组角色；拒绝未知角色字符串。 */
export function parseUserGroupRole(rawRole: string): ParseResult<UserGroupRole> {
  if (rawRole === 'owner' || rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'Unknown user group role.' }
}
