package domains.auth.objects.request

import domains.auth.objects.{EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 站点管理员更新他人账号请求，允许修改邮箱并可选重置密码。 */
final case class UpdateManagedUserAccountRequest(
  email: EmailAddress,
  newPassword: Option[PlaintextPassword]
)

/** 提供管理员账号更新请求 JSON 编解码。 */
object UpdateManagedUserAccountRequest:
  given Encoder[UpdateManagedUserAccountRequest] = deriveEncoder[UpdateManagedUserAccountRequest]
  given Decoder[UpdateManagedUserAccountRequest] = deriveDecoder[UpdateManagedUserAccountRequest]
