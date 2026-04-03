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
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, UserGroupSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("User group slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("User group slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(UserGroupSlug(normalized))

  def unsafe(raw: String): UserGroupSlug =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid user group slug: $message"), identity)

  given Encoder[UserGroupSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupSlug] = Decoder.decodeString.emap(parse)

final case class UserGroupName(value: String)

object UserGroupName:
  def parse(raw: String): Either[String, UserGroupName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group name is required.")
    else if normalized.length > 120 then Left("User group name must be at most 120 characters.")
    else Right(UserGroupName(normalized))

  def unsafe(raw: String): UserGroupName =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid user group name: $message"), identity)

  given Encoder[UserGroupName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupName] = Decoder.decodeString.emap(parse)

final case class UserGroupDescription(value: String)

object UserGroupDescription:
  def parse(raw: String): Either[String, UserGroupDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("User group description must be at most 2000 characters.")
    else Right(UserGroupDescription(normalized))

  def unsafe(raw: String): UserGroupDescription =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid user group description: $message"), identity)

  given Encoder[UserGroupDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupDescription] = Decoder.decodeString.emap(parse)

final case class UserGroupMemberRecord(
  username: Username,
  displayName: DisplayName,
  role: UserGroupRole,
  joinedAt: Instant
)

final case class UserGroupSummaryView(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  createdAt: Instant,
  updatedAt: Instant
)

final case class UserGroup(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  members: List[UserGroupMemberRecord],
  createdAt: Instant,
  updatedAt: Instant
)

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

enum AddUserGroupMemberRole:
  case Manager
  case Member

object AddUserGroupMemberRole:
  def fromDatabase(value: String): Option[AddUserGroupMemberRole] =
    value match
      case "manager" => Some(AddUserGroupMemberRole.Manager)
      case "member" => Some(AddUserGroupMemberRole.Member)
      case _ => None

  def toDatabase(value: AddUserGroupMemberRole): String =
    value match
      case AddUserGroupMemberRole.Manager => "manager"
      case AddUserGroupMemberRole.Member => "member"

  given Encoder[AddUserGroupMemberRole] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[AddUserGroupMemberRole] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown add-user-group-member role: $value")
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
  role: AddUserGroupMemberRole
)

object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)

object UpdateUserGroupMemberRoleRequest:
  given Encoder[UpdateUserGroupMemberRoleRequest] = deriveEncoder[UpdateUserGroupMemberRoleRequest]
  given Decoder[UpdateUserGroupMemberRoleRequest] = deriveDecoder[UpdateUserGroupMemberRoleRequest]

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

final case class ManagedUserGroup(value: UserGroup)
final case class OwnedUserGroup(value: UserGroup)

type UserGroupListResponse = PageResponse[UserGroupSummary]
