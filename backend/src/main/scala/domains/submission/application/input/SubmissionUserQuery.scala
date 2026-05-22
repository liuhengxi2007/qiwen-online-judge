package domains.submission.application.input



import io.circe.{Decoder, Encoder}

final case class SubmissionUserQuery(value: String)

object SubmissionUserQuery:
  def parse(raw: String): Either[String, SubmissionUserQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission username query is required.")
    else Right(SubmissionUserQuery(normalized))

  given Encoder[SubmissionUserQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionUserQuery] = Decoder.decodeString.emap(parse)
