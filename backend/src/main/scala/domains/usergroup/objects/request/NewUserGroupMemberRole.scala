package domains.usergroup.objects.request

import io.circe.{Decoder, Encoder}

enum NewUserGroupMemberRole:
  case Manager
  case Member

object NewUserGroupMemberRole:
  given Encoder[NewUserGroupMemberRole] = Encoder.encodeString.contramap(encode)
  given Decoder[NewUserGroupMemberRole] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, NewUserGroupMemberRole] =
    value.trim match
      case "manager" => Right(NewUserGroupMemberRole.Manager)
      case "member" => Right(NewUserGroupMemberRole.Member)
      case _ => Left("New user-group member role must be one of: manager, member.")

  private def encode(value: NewUserGroupMemberRole): String =
    value match
      case NewUserGroupMemberRole.Manager => "manager"
      case NewUserGroupMemberRole.Member => "member"
