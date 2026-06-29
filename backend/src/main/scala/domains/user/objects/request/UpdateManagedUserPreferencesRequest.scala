package domains.user.objects.request

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 管理端更新用户偏好请求，保留与前端管理表单的镜像形状。 */
final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)

/** 提供管理端偏好更新请求 JSON 编解码。 */
object UpdateManagedUserPreferencesRequest:
  given Encoder[UpdateManagedUserPreferencesRequest] = deriveEncoder[UpdateManagedUserPreferencesRequest]
  given Decoder[UpdateManagedUserPreferencesRequest] = deriveDecoder[UpdateManagedUserPreferencesRequest]
