package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestDescription(value: String)

object ContestDescription:
  given Encoder[ContestDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestDescription] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, ContestDescription] =
    val normalized = raw.trim
    if normalized.length > 4000 then Left("Contest description must be at most 4000 characters.")
    else Right(ContestDescription(normalized))
