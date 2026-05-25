package domains.user.model.response

import domains.user.model.*

import domains.user.model.{DisplayName, Username}

final case class UserProfileResponse(
  username: Username,
  displayName: DisplayName,
  contribution: UserContribution,
  acceptedProblems: List[UserAcceptedProblem]
)
