package domains.usergroup.model

import domains.auth.model.{DisplayName, Username}
import domains.shared.model.PageResponse
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import scala.util.Try

final case class UserGroupId(value: UUID)

object UserGroupId:
  def random(): UserGroupId = UserGroupId(UUID.randomUUID())

  given Encoder[UserGroupId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[UserGroupId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(UserGroupId(_))
  }

final case class UserGroupSlug(value: String)

object UserGroupSlug:
  given Encoder[UserGroupSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupSlug] = Decoder.decodeString.map(UserGroupSlug(_))

final case class UserGroupName(value: String)

object UserGroupName:
  given Encoder[UserGroupName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupName] = Decoder.decodeString.map(UserGroupName(_))

final case class UserGroupDescription(value: String)

object UserGroupDescription:
  given Encoder[UserGroupDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupDescription] = Decoder.decodeString.map(UserGroupDescription(_))

enum UserGroupRole:
  case Owner
  case Manager
  case Member

object UserGroupRole:
  def fromDatabase(value: String): Option[UserGroupRole] =
    value match
      case "owner" => Some(UserGroupRole.Owner)
      case "manager" => Some(UserGroupRole.Manager)
      case "member" => Some(UserGroupRole.Member)
      case _ => None

  def fromDatabaseUnsafe(value: String): UserGroupRole =
    fromDatabase(value).getOrElse(throw IllegalArgumentException(s"Unknown user group role: $value"))

  def toDatabase(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"

  given Encoder[UserGroupRole] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[UserGroupRole] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown user group role: $value")
  }

final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)

object CreateUserGroupRequest:
  given Encoder[CreateUserGroupRequest] = deriveEncoder[CreateUserGroupRequest]
  given Decoder[CreateUserGroupRequest] = deriveDecoder[CreateUserGroupRequest]

final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)

object UpdateUserGroupRequest:
  given Encoder[UpdateUserGroupRequest] = deriveEncoder[UpdateUserGroupRequest]
  given Decoder[UpdateUserGroupRequest] = deriveDecoder[UpdateUserGroupRequest]

final case class AddUserGroupMemberRequest(
  username: Username,
  role: UserGroupRole
)

object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]

final case class UserGroupMember(
  username: Username,
  displayName: DisplayName,
  role: UserGroupRole,
  joinedAt: Instant
)

object UserGroupMember:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupMember] = deriveEncoder[UserGroupMember]
  given Decoder[UserGroupMember] = deriveDecoder[UserGroupMember]

final case class UserGroupSummary(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  createdAt: Instant,
  updatedAt: Instant
)

object UserGroupSummary:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupSummary] = deriveEncoder[UserGroupSummary]
  given Decoder[UserGroupSummary] = deriveDecoder[UserGroupSummary]

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
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupDetail] = deriveEncoder[UserGroupDetail]
  given Decoder[UserGroupDetail] = deriveDecoder[UserGroupDetail]

final case class ManagedUserGroup(value: UserGroupDetail)

type UserGroupListResponse = PageResponse[UserGroupSummary]
