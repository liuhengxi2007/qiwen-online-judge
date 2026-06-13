package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

/** 用户组主键 UUID 值对象。 */
final case class UserGroupId(value: UUID)

/** 提供用户组 ID 的 JSON 字符串编解码。 */
object UserGroupId:
  given Encoder[UserGroupId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[UserGroupId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(UserGroupId(_))
  }
