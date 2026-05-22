package domains.message.application.output

import domains.message.model.*

import domains.auth.model.Username
import domains.user.model.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class DirectMessage(
  id: MessageId,
  conversationId: MessageConversationId,
  sender: UserIdentity,
  recipientUsername: Username,
  content: MessageContent,
  createdAt: Instant,
  readAt: Option[Instant]
)

object DirectMessage:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[DirectMessage] = deriveEncoder[DirectMessage]
  given Decoder[DirectMessage] = deriveDecoder[DirectMessage]
