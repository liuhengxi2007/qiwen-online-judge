package domains.user.model



import domains.auth.model.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserIdentity(
  username: Username,
  displayName: DisplayName
)

object UserIdentity:
  given Encoder[UserIdentity] = deriveEncoder[UserIdentity]
  given Decoder[UserIdentity] = deriveDecoder[UserIdentity]
