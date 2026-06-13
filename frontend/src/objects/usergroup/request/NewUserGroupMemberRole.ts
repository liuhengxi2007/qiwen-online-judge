/** 新增用户组成员时允许设置的角色；不能直接创建 owner。 */
export type NewUserGroupMemberRole = 'manager' | 'member'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 解析新增成员角色；拒绝 owner 和未知角色。 */
export function parseNewUserGroupMemberRole(rawRole: string): ParseResult<NewUserGroupMemberRole> {
  if (rawRole === 'manager' || rawRole === 'member') {
    return { ok: true, value: rawRole }
  }

  return { ok: false, error: 'New members may only be added as member or manager.' }
}
