import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { UserAcceptedProblem } from '@/features/auth/model/UserAcceptedProblem'
import type { UserContribution } from '@/features/auth/model/UserContribution'
import type { UserPreferences } from '@/features/auth/model/UserPreferences'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  preferences: UserPreferences
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}
