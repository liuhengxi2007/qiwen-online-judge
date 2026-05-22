package domains.user.application.input

import domains.user.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)

object UpdateOwnPreferencesRequest:
  given Encoder[UpdateOwnPreferencesRequest] = deriveEncoder[UpdateOwnPreferencesRequest]
  given Decoder[UpdateOwnPreferencesRequest] = deriveDecoder[UpdateOwnPreferencesRequest]
