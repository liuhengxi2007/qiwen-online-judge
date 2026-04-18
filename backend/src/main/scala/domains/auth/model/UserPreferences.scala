package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserPreferences(
  displayMode: UserDisplayMode
)

object UserPreferences:
  given Encoder[UserPreferences] = deriveEncoder[UserPreferences]
  given Decoder[UserPreferences] = deriveDecoder[UserPreferences]
