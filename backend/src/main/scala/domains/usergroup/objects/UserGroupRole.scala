package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


/** 用户组成员角色，owner 代表所有者，manager 代表管理成员，member 为普通成员。 */
enum UserGroupRole:
  case Owner
  case Manager
  case Member

/** 提供用户组成员角色的 JSON 编解码和输入解析。 */
object UserGroupRole:
  given Encoder[UserGroupRole] = Encoder.encodeString.contramap(encode)
  given Decoder[UserGroupRole] = Decoder.decodeString.emap(parse)

  /** 解析组内角色字符串，非法值返回业务校验错误。 */
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
