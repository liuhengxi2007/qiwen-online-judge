package domains.message.model.response


import domains.user.model.UserIdentity

import java.time.Instant

final case class MessageBlockEntry(
  user: UserIdentity,
  createdAt: Instant
)
