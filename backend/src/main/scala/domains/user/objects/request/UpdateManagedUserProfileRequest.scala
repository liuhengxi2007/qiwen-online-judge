package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 管理端更新用户资料请求，目前仅允许修改展示名。 */
final case class UpdateManagedUserProfileRequest(
  displayName: DisplayName
)

/** 提供管理端资料更新请求 JSON 编解码。 */
object UpdateManagedUserProfileRequest:
  given Encoder[UpdateManagedUserProfileRequest] = deriveEncoder[UpdateManagedUserProfileRequest]
  given Decoder[UpdateManagedUserProfileRequest] = deriveDecoder[UpdateManagedUserProfileRequest]
