package domains.contest.objects.response

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class ContestRegistrant(
  user: UserIdentity,
  registeredAt: Instant
)

object ContestRegistrant:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ContestRegistrant] = deriveEncoder[ContestRegistrant]
  given Decoder[ContestRegistrant] = deriveDecoder[ContestRegistrant]
