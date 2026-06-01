import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import type { UserContribution } from '@/objects/user/UserContribution'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}