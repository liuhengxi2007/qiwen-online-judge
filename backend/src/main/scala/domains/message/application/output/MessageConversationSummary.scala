package domains.message.application.output

import domains.message.model.*

import domains.auth.model.Username
import domains.user.model.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class MessageConversationSummary(
  id: MessageConversationId,
  otherUser: UserIdentity,
  lastMessagePreview: Option[String],
  lastMessageSenderUsername: Option[Username],
  lastMessageAt: Instant,
  unreadCount: Int
)

object MessageConversationSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[MessageConversationSummary] = deriveEncoder[MessageConversationSummary]
  given Decoder[MessageConversationSummary] = deriveDecoder[MessageConversationSummary]
