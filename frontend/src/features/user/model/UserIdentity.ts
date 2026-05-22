import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'

export type UserIdentity = {
  username: Username
  displayName: DisplayName
}
