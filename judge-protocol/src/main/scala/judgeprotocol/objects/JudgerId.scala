package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class JudgerId(value: String)

object JudgerId:
  def parse(raw: String): Either[String, JudgerId] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Judger id is required.")
    else if normalized.length > 120 then Left("Judger id must be at most 120 characters.")
    else Right(JudgerId(normalized))

  given Encoder[JudgerId] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgerId] = Decoder.decodeString.emap(parse)
