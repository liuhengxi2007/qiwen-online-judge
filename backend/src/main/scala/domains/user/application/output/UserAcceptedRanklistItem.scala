package domains.user.application.output

import domains.user.model.*

final case class UserAcceptedRanklistItem(
  user: UserIdentity,
  acceptedCount: Int
)
