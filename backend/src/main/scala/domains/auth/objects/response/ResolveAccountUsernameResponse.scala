package domains.auth.objects.response

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ResolveAccountUsernameResponse(
  username: Option[Username]
)

object ResolveAccountUsernameResponse:
  given Encoder[ResolveAccountUsernameResponse] = deriveEncoder[ResolveAccountUsernameResponse]
  given Decoder[ResolveAccountUsernameResponse] = deriveDecoder[ResolveAccountUsernameResponse]
