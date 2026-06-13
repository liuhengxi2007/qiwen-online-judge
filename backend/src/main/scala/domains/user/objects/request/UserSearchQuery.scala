package domains.user.objects.request

import io.circe.{Decoder, Encoder}


/** 用户搜索词值对象，用于管理列表和用户建议接口。 */
final case class UserSearchQuery(value: String)

/** 提供用户搜索词编解码和非空校验。 */
object UserSearchQuery:
  given Encoder[UserSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserSearchQuery] = Decoder.decodeString.emap(parse)

  /** 解析搜索词并去除首尾空白，空搜索词返回业务错误。 */
  def parse(raw: String): Either[String, UserSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User search query is required.")
    else Right(UserSearchQuery(normalized))
