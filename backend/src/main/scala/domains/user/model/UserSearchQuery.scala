package domains.user.model



import io.circe.{Decoder, Encoder}

final case class UserSearchQuery(value: String)

object UserSearchQuery:
  def parse(raw: String): Either[String, UserSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User search query is required.")
    else Right(UserSearchQuery(normalized))

  given Encoder[UserSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserSearchQuery] = Decoder.decodeString.emap(parse)
