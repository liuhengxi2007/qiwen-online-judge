package domains.auth.objects.request

import domains.auth.objects.{EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateOwnAccountRequest(
  email: EmailAddress,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)

object UpdateOwnAccountRequest:
  given Encoder[UpdateOwnAccountRequest] = deriveEncoder[UpdateOwnAccountRequest]
  given Decoder[UpdateOwnAccountRequest] = deriveDecoder[UpdateOwnAccountRequest]
