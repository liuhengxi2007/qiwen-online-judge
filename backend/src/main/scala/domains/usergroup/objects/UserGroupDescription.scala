package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


/** 用户组描述值对象，允许为空但限制最大长度。 */
final case class UserGroupDescription(value: String)

/** 提供用户组描述 JSON 编解码和长度校验。 */
object UserGroupDescription:
  given Encoder[UserGroupDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupDescription] = Decoder.decodeString.emap(parse)

  /** 解析并修剪用户组描述，超长时返回业务校验错误。 */
  def parse(raw: String): Either[String, UserGroupDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("User group description must be at most 2000 characters.")
    else Right(UserGroupDescription(normalized))
