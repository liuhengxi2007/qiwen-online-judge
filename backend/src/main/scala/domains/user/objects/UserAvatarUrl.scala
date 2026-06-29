package domains.user.objects

import io.circe.{Decoder, Encoder}

/** 用户头像访问 URL，通常带更新时间查询参数用于缓存失效。 */
final case class UserAvatarUrl(value: String)

/** 提供头像 URL 的 JSON 编解码。 */
object UserAvatarUrl:
  given Encoder[UserAvatarUrl] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserAvatarUrl] = Decoder.decodeString.map(UserAvatarUrl(_))
