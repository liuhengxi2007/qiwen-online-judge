package domains.submission.application.input



import io.circe.{Decoder, Encoder}

enum SubmissionSortDirection:
  case Asc
  case Desc

object SubmissionSortDirection:
  def parse(value: String): Either[String, SubmissionSortDirection] =
    value.trim match
      case "asc" => Right(SubmissionSortDirection.Asc)
      case "desc" => Right(SubmissionSortDirection.Desc)
      case _ => Left("Submission sort direction must be one of: asc, desc.")

  def toDatabase(value: SubmissionSortDirection): String =
    value match
      case SubmissionSortDirection.Asc => "asc"
      case SubmissionSortDirection.Desc => "desc"

  given Encoder[SubmissionSortDirection] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionSortDirection] = Decoder.decodeString.emap(parse)
