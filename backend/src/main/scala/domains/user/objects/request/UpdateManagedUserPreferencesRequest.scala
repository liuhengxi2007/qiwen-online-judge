package domains.user.objects.request

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)

object UpdateManagedUserPreferencesRequest:
  given Encoder[UpdateManagedUserPreferencesRequest] = deriveEncoder[UpdateManagedUserPreferencesRequest]
  given Decoder[UpdateManagedUserPreferencesRequest] = deriveDecoder[UpdateManagedUserPreferencesRequest]
