package domains.user.objects.response

import domains.user.objects.*

final case class UserAcceptedRanklistItem(
  user: UserIdentity,
  acceptedCount: Int
)
