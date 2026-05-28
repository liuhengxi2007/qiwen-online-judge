package domains.submission.objects

import io.circe.{Decoder, Encoder}


import scala.util.Try

final case class SubmissionId(value: Long)

object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

  def parse(raw: String): Either[String, SubmissionId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Submission id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Submission id is invalid.")
        else Right(SubmissionId(value))
      }
