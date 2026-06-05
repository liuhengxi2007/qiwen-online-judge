package domains.contest.objects.response

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ContestRegistrant(
  user: UserIdentity
)

object ContestRegistrant:
  given Encoder[ContestRegistrant] = deriveEncoder[ContestRegistrant]
  given Decoder[ContestRegistrant] = deriveDecoder[ContestRegistrant]
