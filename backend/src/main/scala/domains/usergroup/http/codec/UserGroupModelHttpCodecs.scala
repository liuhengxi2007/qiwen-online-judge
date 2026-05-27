package domains.usergroup.http.codec

import domains.user.http.codec.UserModelHttpCodecs.given
import domains.usergroup.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import java.util.UUID
import scala.util.Try

object UserGroupModelHttpCodecs:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[UserGroupId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(UserGroupId(_))
  }

  given Encoder[UserGroupSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupSlug] = Decoder.decodeString.emap(UserGroupSlug.parse)

  given Encoder[UserGroupName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupName] = Decoder.decodeString.emap(UserGroupName.parse)

  given Encoder[UserGroupDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupDescription] = Decoder.decodeString.emap(UserGroupDescription.parse)

  given Encoder[UserGroupRole] = Encoder.encodeString.contramap(encodeUserGroupRole)
  given Decoder[UserGroupRole] = Decoder.decodeString.emap(UserGroupRole.parse)

  given Encoder[AddUserGroupMemberRole] = Encoder.encodeString.contramap(encodeAddUserGroupMemberRole)
  given Decoder[AddUserGroupMemberRole] = Decoder.decodeString.emap(AddUserGroupMemberRole.parse)

  given Encoder[UserGroupMember] = deriveEncoder[UserGroupMember]
  given Decoder[UserGroupMember] = deriveDecoder[UserGroupMember]

  private def encodeUserGroupRole(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"

  private def encodeAddUserGroupMemberRole(value: AddUserGroupMemberRole): String =
    value match
      case AddUserGroupMemberRole.Manager => "manager"
      case AddUserGroupMemberRole.Member => "member"
