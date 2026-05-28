package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserProfileRequest(
  displayName: DisplayName
)

object UpdateManagedUserProfileRequest:
  given Encoder[UpdateManagedUserProfileRequest] = deriveEncoder[UpdateManagedUserProfileRequest]
  given Decoder[UpdateManagedUserProfileRequest] = deriveDecoder[UpdateManagedUserProfileRequest]
