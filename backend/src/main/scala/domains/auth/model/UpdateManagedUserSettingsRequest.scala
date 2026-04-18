package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateManagedUserSettingsRequest(
  displayName: DisplayName,
  email: EmailAddress,
  preferences: UserPreferences,
  newPassword: Option[PlaintextPassword]
)

object UpdateManagedUserSettingsRequest:
  given Encoder[UpdateManagedUserSettingsRequest] = deriveEncoder[UpdateManagedUserSettingsRequest]
  given Decoder[UpdateManagedUserSettingsRequest] = deriveDecoder[UpdateManagedUserSettingsRequest]
