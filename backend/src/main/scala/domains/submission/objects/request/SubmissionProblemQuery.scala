package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


final case class SubmissionProblemQuery(value: String)

object SubmissionProblemQuery:
  given Encoder[SubmissionProblemQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionProblemQuery] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, SubmissionProblemQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission problem query is required.")
    else Right(SubmissionProblemQuery(normalized))
