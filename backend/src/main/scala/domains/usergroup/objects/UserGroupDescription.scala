package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


final case class UserGroupDescription(value: String)

object UserGroupDescription:
  given Encoder[UserGroupDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupDescription] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, UserGroupDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("User group description must be at most 2000 characters.")
    else Right(UserGroupDescription(normalized))
