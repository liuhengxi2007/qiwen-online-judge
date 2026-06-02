import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { Username } from '@/objects/user/Username'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'

export type UserProfileSettings = {
  username: Username
  displayName: DisplayName
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
  avatarUrl: UserAvatarUrl | null
}
