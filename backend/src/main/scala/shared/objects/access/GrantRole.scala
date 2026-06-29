package shared.objects.access

import io.circe.{Decoder, Encoder}

/** 资源授权角色，表示访问控制列表中被授予的查看或管理权限。 */
enum GrantRole:
  case Viewer
  case Manager

/** 负责在授权角色与 API/数据库使用的字符串值之间转换。 */
object GrantRole:
  given Encoder[GrantRole] = Encoder.encodeString.contramap(encode)
  given Decoder[GrantRole] = Decoder.decodeString.emap(parse)

  /** 解析外部输入中的授权角色，返回校验错误或规范化后的角色值，无副作用。 */
  def parse(value: String): Either[String, GrantRole] =
    value.trim match
      case "viewer" => Right(GrantRole.Viewer)
      case "manager" => Right(GrantRole.Manager)
      case _ => Left("Grant role must be one of: viewer, manager.")

  private def encode(value: GrantRole): String =
    value match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"
