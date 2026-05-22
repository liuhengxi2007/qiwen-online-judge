package domains.user.application.output

import domains.user.model.*

final case class UserRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)
