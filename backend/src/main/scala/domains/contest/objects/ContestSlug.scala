package domains.contest.objects

import io.circe.{Decoder, Encoder}

final case class ContestSlug(value: String)

object ContestSlug:
  given Encoder[ContestSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestSlug] = Decoder.decodeString.emap(parse)

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, ContestSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Contest slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Contest slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Contest slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ContestSlug(normalized))
