package domains.user.model

import domains.auth.model.{DisplayName, EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateOwnSettingsRequest(
  displayName: DisplayName,
  email: EmailAddress,
  preferences: UserPreferences,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)

object UpdateOwnSettingsRequest:
  given Encoder[UpdateOwnSettingsRequest] = deriveEncoder[UpdateOwnSettingsRequest]
  given Decoder[UpdateOwnSettingsRequest] = deriveDecoder[UpdateOwnSettingsRequest]
