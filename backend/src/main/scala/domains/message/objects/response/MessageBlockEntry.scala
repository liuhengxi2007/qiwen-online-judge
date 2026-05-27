package domains.message.objects.response


import domains.user.objects.UserIdentity

import java.time.Instant

final case class MessageBlockEntry(
  user: UserIdentity,
  createdAt: Instant
)
