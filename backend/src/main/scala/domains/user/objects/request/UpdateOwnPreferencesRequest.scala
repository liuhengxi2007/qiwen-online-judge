package domains.user.objects.request

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)

object UpdateOwnPreferencesRequest:
  given Encoder[UpdateOwnPreferencesRequest] = deriveEncoder[UpdateOwnPreferencesRequest]
  given Decoder[UpdateOwnPreferencesRequest] = deriveDecoder[UpdateOwnPreferencesRequest]
