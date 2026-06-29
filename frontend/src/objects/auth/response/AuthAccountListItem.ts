import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'

/** 账号管理列表项；暴露邮箱和权限位，仅应在受管账号页面使用。 */
export type AuthAccountListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}
