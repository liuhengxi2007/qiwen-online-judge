package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


/** 用户组名称值对象，要求非空并限制长度。 */
final case class UserGroupName(value: String)

/** 提供用户组名称 JSON 编解码和业务校验。 */
object UserGroupName:
  given Encoder[UserGroupName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupName] = Decoder.decodeString.emap(parse)

  /** 解析并修剪用户组名称，空值或超长时返回业务校验错误。 */
  def parse(raw: String): Either[String, UserGroupName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group name is required.")
    else if normalized.length > 120 then Left("User group name must be at most 120 characters.")
    else Right(UserGroupName(normalized))
