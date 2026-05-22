package domains.user.application.output

import domains.user.model.*

import domains.user.model.{DisplayName, Username}

final case class UserProfileResponse(
  username: Username,
  displayName: DisplayName,
  contribution: UserContribution,
  acceptedProblems: List[UserAcceptedProblem]
)
