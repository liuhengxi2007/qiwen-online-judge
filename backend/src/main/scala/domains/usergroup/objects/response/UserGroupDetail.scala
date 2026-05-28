package domains.usergroup.objects.response

import domains.usergroup.objects.*

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

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

object UserGroupDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupDetail] = deriveEncoder[UserGroupDetail]
  given Decoder[UserGroupDetail] = deriveDecoder[UserGroupDetail]

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
