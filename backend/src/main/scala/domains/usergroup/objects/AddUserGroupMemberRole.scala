package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


enum AddUserGroupMemberRole:
  case Manager
  case Member

object AddUserGroupMemberRole:
  given Encoder[AddUserGroupMemberRole] = Encoder.encodeString.contramap(encode)
  given Decoder[AddUserGroupMemberRole] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, AddUserGroupMemberRole] =
    value.trim match
      case "manager" => Right(AddUserGroupMemberRole.Manager)
      case "member" => Right(AddUserGroupMemberRole.Member)
      case _ => Left("Add-user-group-member role must be one of: manager, member.")

  private def encode(value: AddUserGroupMemberRole): String =
    value match
      case AddUserGroupMemberRole.Manager => "manager"
      case AddUserGroupMemberRole.Member => "member"
