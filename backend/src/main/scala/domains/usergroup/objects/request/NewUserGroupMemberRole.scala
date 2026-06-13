package domains.usergroup.objects.request

import io.circe.{Decoder, Encoder}

/** 新增用户组成员时允许指定的角色，不允许直接添加 owner。 */
enum NewUserGroupMemberRole:
  case Manager
  case Member

/** 提供新增成员角色的 JSON 编解码和输入解析。 */
object NewUserGroupMemberRole:
  given Encoder[NewUserGroupMemberRole] = Encoder.encodeString.contramap(encode)
  given Decoder[NewUserGroupMemberRole] = Decoder.decodeString.emap(parse)

  /** 解析新增成员角色字符串，非法值返回业务校验错误。 */
  def parse(value: String): Either[String, NewUserGroupMemberRole] =
    value.trim match
      case "manager" => Right(NewUserGroupMemberRole.Manager)
      case "member" => Right(NewUserGroupMemberRole.Member)
      case _ => Left("New user-group member role must be one of: manager, member.")

  private def encode(value: NewUserGroupMemberRole): String =
    value match
      case NewUserGroupMemberRole.Manager => "manager"
      case NewUserGroupMemberRole.Member => "member"
