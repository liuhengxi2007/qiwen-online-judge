import { displayNameValue } from '@/objects/user/DisplayName'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { usernameValue } from '@/objects/user/Username'

/**
 * 用户展示标签所需的最小身份信息，只依赖显示名和用户名。
 */
type UserDisplayIdentity = Pick<UserIdentity, 'displayName' | 'username'>

/**
 * 根据用户偏好生成用户显示标签，可展示用户名、显示名或显示名加用户名。
 */
export function formatUserDisplayLabel(user: UserDisplayIdentity, displayMode: UserDisplayMode): string {
  const displayName = displayNameValue(user.displayName)
  const username = usernameValue(user.username)

  switch (displayMode) {
    case 'username':
      return username
    case 'display_name_with_username':
      return `${displayName} (${username})`
    case 'display_name':
    default:
      return displayName
  }
}

/**
 * 根据贡献值正负返回文本颜色类名，供资料页和榜单保持一致的视觉语义。
 */
export function contributionTextClassName(value: number): string {
  if (value > 0) {
    return 'text-emerald-700'
  }

  if (value < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}
