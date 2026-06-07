package domains.submission.objects

import io.circe.{Decoder, Encoder}


final case class SubmissionSourceCode(value: String)

object SubmissionSourceCode:
  val MaxChars: Int = 200000

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Source code is required.")
    else if raw.length > MaxChars then Left(s"Source code must be at most $MaxChars characters.")
    else Right(SubmissionSourceCode(raw))
