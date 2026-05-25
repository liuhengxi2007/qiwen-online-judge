package domains.user.model.response

import domains.user.model.*

final case class UserRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)
