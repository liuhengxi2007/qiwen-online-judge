package domains.user.model.response

import domains.user.model.*

final case class UserAcceptedRanklistItem(
  user: UserIdentity,
  acceptedCount: Int
)
