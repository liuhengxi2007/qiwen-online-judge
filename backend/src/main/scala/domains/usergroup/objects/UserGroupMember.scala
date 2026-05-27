package domains.usergroup.objects



import domains.user.objects.{DisplayName, Username}

import java.time.Instant

final case class UserGroupMember(
  username: Username,
  displayName: DisplayName,
  role: UserGroupRole,
  joinedAt: Instant
)
