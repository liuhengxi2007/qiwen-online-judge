package domains.user.objects.response

import domains.user.objects.*

final case class UserRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)
