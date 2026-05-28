package domains.auth.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)

object UpdateUserPermissionsRequest:
  given Encoder[UpdateUserPermissionsRequest] = deriveEncoder[UpdateUserPermissionsRequest]
  given Decoder[UpdateUserPermissionsRequest] = deriveDecoder[UpdateUserPermissionsRequest]
