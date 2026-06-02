package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestTitle(value: String)

object ContestTitle:
  given Encoder[ContestTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestTitle] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, ContestTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Contest title is required.")
    else if normalized.length > 120 then Left("Contest title must be at most 120 characters.")
    else Right(ContestTitle(normalized))
