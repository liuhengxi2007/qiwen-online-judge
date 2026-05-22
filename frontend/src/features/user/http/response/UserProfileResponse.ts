import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { UserAcceptedProblem } from '@/features/user/model/UserAcceptedProblem'
import type { UserContribution } from '@/features/user/model/UserContribution'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}
