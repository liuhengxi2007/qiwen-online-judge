import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'

/** 用户公开身份；只包含展示所需的用户名和显示名，不包含邮箱或权限。 */
export type UserIdentity = {
  username: Username
  displayName: DisplayName
}
