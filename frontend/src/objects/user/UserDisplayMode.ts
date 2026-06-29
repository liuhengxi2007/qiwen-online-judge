/** 用户身份显示模式；控制界面优先显示昵称、用户名或组合形式。 */
export type UserDisplayMode = 'display_name' | 'username' | 'display_name_with_username'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 返回显示模式原始枚举值；用于持久化偏好。 */
export function userDisplayModeValue(displayMode: UserDisplayMode): UserDisplayMode {
  return displayMode
}

/** 解析显示模式字符串；拒绝未知枚举值并返回可展示错误。 */
export function parseUserDisplayMode(rawDisplayMode: string): ParseResult<UserDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'display_name':
    case 'username':
    case 'display_name_with_username':
      return { ok: true, value: normalized }
    default:
      return {
        ok: false,
        error: 'Display mode must be one of: display_name, username, display_name_with_username.',
      }
  }
}
