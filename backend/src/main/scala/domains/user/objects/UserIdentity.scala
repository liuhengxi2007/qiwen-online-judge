package domains.user.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


/** 用户身份摘要，包含规范用户名和展示名，供跨领域列表/建议项复用。 */
final case class UserIdentity(
  username: Username,
  displayName: DisplayName
)

/** 提供用户身份摘要 JSON 编解码。 */
object UserIdentity:
  given Encoder[UserIdentity] = deriveEncoder[UserIdentity]
  given Decoder[UserIdentity] = deriveDecoder[UserIdentity]
