package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户自助更新资料请求，目前仅允许修改展示名。 */
final case class UpdateOwnProfileRequest(
  displayName: DisplayName
)

/** 提供用户自助资料更新请求 JSON 编解码。 */
object UpdateOwnProfileRequest:
  given Encoder[UpdateOwnProfileRequest] = deriveEncoder[UpdateOwnProfileRequest]
  given Decoder[UpdateOwnProfileRequest] = deriveDecoder[UpdateOwnProfileRequest]
