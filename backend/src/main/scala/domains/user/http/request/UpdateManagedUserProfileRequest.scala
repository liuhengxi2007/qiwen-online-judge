package domains.user.http.request

import domains.user.model.*

import domains.auth.model.DisplayName
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserProfileRequest(
  displayName: DisplayName
)

object UpdateManagedUserProfileRequest:
  given Encoder[UpdateManagedUserProfileRequest] = deriveEncoder[UpdateManagedUserProfileRequest]
  given Decoder[UpdateManagedUserProfileRequest] = deriveDecoder[UpdateManagedUserProfileRequest]
