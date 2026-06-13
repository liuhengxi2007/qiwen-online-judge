package domains.user.objects.request

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户自助更新偏好请求。 */
final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)

/** 提供用户自助偏好更新请求 JSON 编解码。 */
object UpdateOwnPreferencesRequest:
  given Encoder[UpdateOwnPreferencesRequest] = deriveEncoder[UpdateOwnPreferencesRequest]
  given Decoder[UpdateOwnPreferencesRequest] = deriveDecoder[UpdateOwnPreferencesRequest]
