package shared.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


import java.time.Instant
import scala.util.Try

/** 通用审计时间字段，用于 API 输出资源创建和最后更新时间。 */
final case class AuditFields(createdAt: Instant, updatedAt: Instant)

/** 提供审计字段与 ISO-8601 字符串时间之间的 JSON 编解码。 */
object AuditFields:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[AuditFields] = deriveEncoder[AuditFields]
  given Decoder[AuditFields] = deriveDecoder[AuditFields]
