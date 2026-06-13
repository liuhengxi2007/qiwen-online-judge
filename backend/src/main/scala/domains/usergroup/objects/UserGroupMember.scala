package domains.usergroup.objects



import domains.user.objects.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 用户组成员模型，包含成员身份、组内角色和加入时间。 */
final case class UserGroupMember(
  username: Username,
  displayName: DisplayName,
  role: UserGroupRole,
  joinedAt: Instant
)

/** 提供用户组成员 JSON 编解码，并以 ISO 字符串传输时间。 */
object UserGroupMember:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupMember] = deriveEncoder[UserGroupMember]
  given Decoder[UserGroupMember] = deriveDecoder[UserGroupMember]
