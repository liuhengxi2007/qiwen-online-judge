package domains.usergroup.objects.response

import domains.usergroup.objects.*

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 用户组列表摘要响应，包含基础资料、所有者和审计时间，不包含成员列表。 */
final case class UserGroupSummary(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供用户组摘要响应 JSON 编解码，并以 ISO 字符串传输时间。 */
object UserGroupSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupSummary] = deriveEncoder[UserGroupSummary]
  given Decoder[UserGroupSummary] = deriveDecoder[UserGroupSummary]
