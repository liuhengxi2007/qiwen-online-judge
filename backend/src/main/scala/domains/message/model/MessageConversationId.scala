package domains.message.model



import java.util.UUID
import scala.util.Try

final case class MessageConversationId(value: UUID)

object MessageConversationId:

  def parse(raw: String): Either[String, MessageConversationId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(MessageConversationId(_))
