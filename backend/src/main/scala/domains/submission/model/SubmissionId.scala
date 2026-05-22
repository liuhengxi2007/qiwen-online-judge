package domains.submission.model



import scala.util.Try

final case class SubmissionId(value: Long)

object SubmissionId:
  def parse(raw: String): Either[String, SubmissionId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Submission id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Submission id is invalid.")
        else Right(SubmissionId(value))
      }
