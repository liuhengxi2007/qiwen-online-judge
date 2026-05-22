package domains.usergroup.model



import domains.user.model.{DisplayName, Username}

import java.time.Instant

final case class UserGroupMember(
  username: Username,
  displayName: DisplayName,
  role: UserGroupRole,
  joinedAt: Instant
)
