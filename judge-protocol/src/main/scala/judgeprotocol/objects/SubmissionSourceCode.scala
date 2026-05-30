package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class SubmissionSourceCode(value: String)

object SubmissionSourceCode:
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Submission source code is required.")
    else if raw.length > 200000 then Left("Submission source code must be at most 200000 characters.")
    else Right(SubmissionSourceCode(raw))

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)
