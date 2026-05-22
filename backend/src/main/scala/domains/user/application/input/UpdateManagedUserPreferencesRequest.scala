package domains.user.application.input

import domains.user.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)

object UpdateManagedUserPreferencesRequest:
  given Encoder[UpdateManagedUserPreferencesRequest] = deriveEncoder[UpdateManagedUserPreferencesRequest]
  given Decoder[UpdateManagedUserPreferencesRequest] = deriveDecoder[UpdateManagedUserPreferencesRequest]
