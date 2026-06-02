package domains.user.objects

import io.circe.{Decoder, Encoder}

final case class UserAvatarUrl(value: String)

object UserAvatarUrl:
  given Encoder[UserAvatarUrl] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserAvatarUrl] = Decoder.decodeString.map(UserAvatarUrl(_))
