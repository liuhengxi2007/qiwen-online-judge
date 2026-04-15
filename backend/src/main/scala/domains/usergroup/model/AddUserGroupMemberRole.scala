package domains.usergroup.model

import io.circe.{Decoder, Encoder}

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
