package domains.message.objects.response

import domains.message.objects.*

import domains.user.objects.Username
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 私信消息响应，包含发送者身份、收件人、内容、创建时间和已读时间。 */
final case class DirectMessage(
  id: MessageId,
  conversationId: MessageConversationId,
  sender: UserIdentity,
  recipientUsername: Username,
  content: MessageContent,
  createdAt: Instant,
  readAt: Option[Instant]
)

/** 提供私信消息 JSON codec，并显式处理 Instant 字符串格式。 */
object DirectMessage:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[DirectMessage] = deriveEncoder[DirectMessage]
  given Decoder[DirectMessage] = deriveDecoder[DirectMessage]
