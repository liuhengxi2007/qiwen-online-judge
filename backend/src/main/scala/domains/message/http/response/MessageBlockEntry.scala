package domains.message.http.response

import domains.message.model.*

import domains.user.model.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class MessageBlockEntry(
  user: UserIdentity,
  createdAt: Instant
)

object MessageBlockEntry:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[MessageBlockEntry] = deriveEncoder[MessageBlockEntry]
  given Decoder[MessageBlockEntry] = deriveDecoder[MessageBlockEntry]
