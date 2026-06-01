import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import { fromUserAcceptedProblemContract } from '@/objects/user/UserAcceptedProblem'
import type { UserContribution } from '@/objects/user/UserContribution'
import { fromUserContributionContract } from '@/objects/user/UserContribution'
import { readArray, readNumber, readRecord, readString } from '@/objects/shared/PageResponse'

export type UserProfileResponse = {
  username: Username
  displayName: DisplayName
  contribution: UserContribution
  acceptedProblems: UserAcceptedProblem[]
}

export function fromUserProfileResponseContract(value: unknown, label = 'user profile response'): UserProfileResponse {
  const response = readRecord(value, label)
  return {
    username: fromUsernameContract(readString(response.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(response.displayName, `${label} display name`), `${label} display name`),
    contribution: fromUserContributionContract(readNumber(response.contribution, `${label} contribution`), `${label} contribution`),
    acceptedProblems: readArray(response.acceptedProblems, `${label} accepted problems`, fromUserAcceptedProblemContract),
  }
}
