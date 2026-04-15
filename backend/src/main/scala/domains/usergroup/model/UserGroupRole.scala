package domains.usergroup.model

import io.circe.{Decoder, Encoder}

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

  def toDatabase(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"

  given Encoder[UserGroupRole] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[UserGroupRole] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown user group role: $value")
  }
