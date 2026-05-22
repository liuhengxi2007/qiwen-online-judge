package domains.message.model



import java.util.UUID
import scala.util.Try

final case class MessageId(value: UUID)

object MessageId:

  def parse(raw: String): Either[String, MessageId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(MessageId(_))
