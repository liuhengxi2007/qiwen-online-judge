import type { DisplayName } from '@/objects/user/DisplayName'
import type { RatingValue } from '@/objects/rating/RatingValue'
import type { Username } from '@/objects/user/Username'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'
import type { UserContribution } from '@/objects/user/UserContribution'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  avatarUrl: UserAvatarUrl | null
  contribution: UserContribution
  rating: RatingValue
  acceptedProblems: UserAcceptedProblem[]
}
