package domains.usergroup.objects.response

import domains.usergroup.objects.*

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 用户组详情响应，包含基础资料、所有者、成员列表和审计时间。 */
final case class UserGroupDetail(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  members: List[UserGroupMember],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供用户组详情响应编解码和从内部 UserGroup 转换的入口。 */
object UserGroupDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupDetail] = deriveEncoder[UserGroupDetail]
  given Decoder[UserGroupDetail] = deriveDecoder[UserGroupDetail]

  /** 将内部用户组模型转换为 API 详情响应，不改变成员排序。 */
  def fromUserGroup(group: UserGroup): UserGroupDetail =
    UserGroupDetail(
      id = group.id,
      slug = group.slug,
      name = group.name,
      description = group.description,
      ownerUsername = group.ownerUsername,
      members = group.members,
      createdAt = group.createdAt,
      updatedAt = group.updatedAt
    )
