package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


enum UserGroupRole:
  case Owner
  case Manager
  case Member

object UserGroupRole:
  given Encoder[UserGroupRole] = Encoder.encodeString.contramap(encode)
  given Decoder[UserGroupRole] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, UserGroupRole] =
    value.trim match
      case "owner" => Right(UserGroupRole.Owner)
      case "manager" => Right(UserGroupRole.Manager)
      case "member" => Right(UserGroupRole.Member)
      case _ => Left("User group role must be one of: owner, manager, member.")

  private def encode(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"
