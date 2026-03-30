package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)

object SessionResponse:
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]
