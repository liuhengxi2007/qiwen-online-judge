package domains.message.application.output

import domains.message.model.*

import domains.user.model.UserIdentity

import java.time.Instant

final case class MessageBlockEntry(
  user: UserIdentity,
  createdAt: Instant
)
