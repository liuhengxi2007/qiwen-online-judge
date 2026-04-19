import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { UserAcceptedProblem } from '@/features/auth/model/UserAcceptedProblem'
import type { UserContribution } from '@/features/auth/model/UserContribution'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}
