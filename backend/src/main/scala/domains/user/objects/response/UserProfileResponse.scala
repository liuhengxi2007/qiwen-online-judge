package domains.user.objects.response

import domains.user.objects.*

import domains.user.objects.{DisplayName, Username}

final case class UserProfileResponse(
  username: Username,
  displayName: DisplayName,
  contribution: UserContribution,
  acceptedProblems: List[UserAcceptedProblem]
)
