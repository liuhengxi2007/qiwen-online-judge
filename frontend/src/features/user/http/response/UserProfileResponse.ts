import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'
import type { UserAcceptedProblem } from '@/features/user/model/UserAcceptedProblem'
import type { UserContribution } from '@/features/user/model/UserContribution'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}
