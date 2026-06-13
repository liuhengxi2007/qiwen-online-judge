import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'

/** 管理员可见的用户列表项；包含邮箱和权限位，不能用于公开资料页。 */
export type ManagedUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}
