package domains.user.application.input

import domains.user.model.*

import domains.auth.model.{EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserAccountRequest(
  email: EmailAddress,
  newPassword: Option[PlaintextPassword]
)

object UpdateManagedUserAccountRequest:
  given Encoder[UpdateManagedUserAccountRequest] = deriveEncoder[UpdateManagedUserAccountRequest]
  given Decoder[UpdateManagedUserAccountRequest] = deriveDecoder[UpdateManagedUserAccountRequest]
