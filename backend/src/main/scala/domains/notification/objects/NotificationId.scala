package domains.notification.objects



import java.util.UUID
import scala.util.Try

final case class NotificationId(value: UUID)

object NotificationId:

  def parse(raw: String): Either[String, NotificationId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(NotificationId(_))
