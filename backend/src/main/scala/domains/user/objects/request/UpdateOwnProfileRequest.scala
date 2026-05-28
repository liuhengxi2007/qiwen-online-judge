package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateOwnProfileRequest(
  displayName: DisplayName
)

object UpdateOwnProfileRequest:
  given Encoder[UpdateOwnProfileRequest] = deriveEncoder[UpdateOwnProfileRequest]
  given Decoder[UpdateOwnProfileRequest] = deriveDecoder[UpdateOwnProfileRequest]
