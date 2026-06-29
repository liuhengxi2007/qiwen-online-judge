import type { DisplayName } from '@/objects/user/DisplayName'
import type { RatingValue } from '@/objects/rating/RatingValue'
import type { Username } from '@/objects/user/Username'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'
import type { UserContribution } from '@/objects/user/UserContribution'

/** 用户公开资料响应；包含头像、贡献、rating 和已通过题目数量。 */
export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  avatarUrl: UserAvatarUrl | null
  contribution: UserContribution
  rating: RatingValue
  acceptedProblemCount: number
}
