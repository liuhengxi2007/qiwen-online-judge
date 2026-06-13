package domains.notification.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

/** 通知 id 领域值，封装 UUID。 */
final case class NotificationId(value: UUID)

/** 提供通知 id 的字符串 JSON codec 和路径解析。 */
object NotificationId:
  given Encoder[NotificationId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[NotificationId] = Decoder.decodeString.emap(parse)

  /** 将路径字符串解析为 UUID 通知 id。 */
  def parse(raw: String): Either[String, NotificationId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(NotificationId(_))
